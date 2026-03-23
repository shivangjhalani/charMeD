package com.charmed.rpc;

import com.charmed.document.Document;
import com.charmed.document.DocumentManager;
import com.charmed.editor.Editor;
import com.charmed.exception.CharMedException;
import com.charmed.renderer.HtmlRenderer;
import com.charmed.renderer.PlainTextRenderer;
import com.charmed.renderer.Renderer;
import com.charmed.visitor.SearchVisitor;
import com.charmed.visitor.WordCountVisitor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.file.Path;
import java.util.List;

/**
 * Adapter: JSON-RPC document/* methods → DocumentManager/Editor calls.
 * Adapter pattern — converts RPC protocol to internal Java API.
 */
public class DocumentHandler implements RpcHandler {

    private final DocumentManager documentManager;
    private final Editor editor;

    public DocumentHandler(DocumentManager documentManager, Editor editor) {
        this.documentManager = documentManager;
        this.editor = editor;
    }

    @Override
    public RpcResponse handle(String method, JsonObject params) {
        try {
            return switch (method) {
                case "document/open"       -> handleOpen(params);
                case "document/new"        -> handleNew(params);
                case "document/save"       -> handleSave(params);
                case "document/edit"       -> handleEdit(params);
                case "document/search"     -> handleSearch(params);
                case "document/getContent" -> handleGetContent(params);
                case "document/export"     -> handleExport(params);
                default -> RpcResponse.error(RpcError.methodNotFound(method));
            };
        } catch (CharMedException e) {
            return RpcResponse.error(RpcError.fromCode(e.errorCode(), e.getMessage()));
        } catch (Exception e) {
            return RpcResponse.error(RpcError.internalError(e.getMessage()));
        }
    }

    private RpcResponse handleOpen(JsonObject params) {
        String path = params.get("path").getAsString();
        Document doc = documentManager.open(Path.of(path));
        WordCountVisitor wc = new WordCountVisitor();
        doc.getAst().accept(wc);

        JsonObject result = new JsonObject();
        result.addProperty("content", doc.getContent());
        result.addProperty("lineCount", doc.lineCount());
        result.addProperty("wordCount", wc.getCount());
        result.addProperty("filePath", path);
        return RpcResponse.success(result);
    }

    private RpcResponse handleNew(JsonObject params) {
        Document doc = documentManager.newDocument();
        JsonObject result = new JsonObject();
        result.addProperty("content", "");
        result.addProperty("lineCount", 1);
        result.addProperty("wordCount", 0);
        result.add("filePath", null);
        return RpcResponse.success(result);
    }

    private RpcResponse handleSave(JsonObject params) {
        Path path = params.has("path") && !params.get("path").isJsonNull()
                ? Path.of(params.get("path").getAsString()) : null;
        long bytes = documentManager.save(path);
        Document doc = documentManager.getActiveDocument();

        JsonObject result = new JsonObject();
        result.addProperty("saved", true);
        result.addProperty("path", doc.getFilePath() != null ? doc.getFilePath().toString() : null);
        result.addProperty("bytesWritten", bytes);
        return RpcResponse.success(result);
    }

    private RpcResponse handleEdit(JsonObject params) {
        String action = params.get("action").getAsString();
        Document doc = documentManager.getActiveDocument();
        if (doc == null) {
            return RpcResponse.error(RpcError.internalError("No active document"));
        }

        if ("insert".equals(action)) {
            String text = params.get("text").getAsString();
            JsonObject posObj = params.getAsJsonObject("position");
            var pos = new com.charmed.document.CursorPosition(
                    posObj.get("line").getAsInt(), posObj.get("column").getAsInt());
            editor.insertText(pos, text);

            String[] insertedLines = text.split("\n", -1);
            int newLine = pos.line() + insertedLines.length - 1;
            int newCol = insertedLines.length == 1
                    ? pos.column() + text.length()
                    : insertedLines[insertedLines.length - 1].length();

            JsonObject result = new JsonObject();
            result.addProperty("applied", true);
            JsonObject cursor = new JsonObject();
            cursor.addProperty("line", newLine);
            cursor.addProperty("column", newCol);
            result.add("newCursor", cursor);
            return RpcResponse.success(result);
        }

        if ("delete".equals(action)) {
            JsonObject rangeObj = params.getAsJsonObject("range");
            JsonObject startObj = rangeObj.getAsJsonObject("start");
            JsonObject endObj = rangeObj.getAsJsonObject("end");
            var start = new com.charmed.document.CursorPosition(
                    startObj.get("line").getAsInt(), startObj.get("column").getAsInt());
            var end = new com.charmed.document.CursorPosition(
                    endObj.get("line").getAsInt(), endObj.get("column").getAsInt());
            editor.deleteText(start, end);

            JsonObject result = new JsonObject();
            result.addProperty("applied", true);
            JsonObject cursor = new JsonObject();
            cursor.addProperty("line", start.line());
            cursor.addProperty("column", start.column());
            result.add("newCursor", cursor);
            return RpcResponse.success(result);
        }

        if ("newline".equals(action)) {
            JsonObject posObj = params.getAsJsonObject("position");
            var pos = new com.charmed.document.CursorPosition(
                    posObj.get("line").getAsInt(), posObj.get("column").getAsInt());
            editor.insertText(pos, "\n");

            JsonObject result = new JsonObject();
            result.addProperty("applied", true);
            JsonObject cursor = new JsonObject();
            cursor.addProperty("line", pos.line() + 1);
            cursor.addProperty("column", 0);
            result.add("newCursor", cursor);
            return RpcResponse.success(result);
        }

        return RpcResponse.error(RpcError.invalidParams("Unknown action: " + action));
    }

    private RpcResponse handleSearch(JsonObject params) {
        String query = params.get("query").getAsString();
        boolean caseSensitive = params.has("caseSensitive")
                && params.get("caseSensitive").getAsBoolean();
        Document doc = documentManager.getActiveDocument();
        if (doc == null) {
            return RpcResponse.error(RpcError.internalError("No active document"));
        }

        SearchVisitor sv = new SearchVisitor(query, caseSensitive);
        doc.getAst().accept(sv);
        List<SearchVisitor.SearchMatch> matches = sv.getMatches();

        JsonArray matchArr = new JsonArray();
        for (var m : matches) {
            JsonObject obj = new JsonObject();
            obj.addProperty("line", m.line());
            obj.addProperty("column", m.column());
            obj.addProperty("length", m.length());
            obj.addProperty("context", m.context());
            matchArr.add(obj);
        }

        JsonObject result = new JsonObject();
        result.add("matches", matchArr);
        result.addProperty("totalMatches", matches.size());
        return RpcResponse.success(result);
    }

    private RpcResponse handleGetContent(JsonObject params) {
        Document doc = documentManager.getActiveDocument();
        if (doc == null) {
            return RpcResponse.error(RpcError.internalError("No active document"));
        }

        String format = params.has("format") ? params.get("format").getAsString() : "raw";
        String content;

        if ("html".equals(format)) {
            Renderer r = new HtmlRenderer();
            content = r.render(doc.getAst());
        } else if ("plaintext".equals(format)) {
            Renderer r = new PlainTextRenderer();
            content = r.render(doc.getAst());
        } else {
            content = doc.getContent();
        }

        JsonObject result = new JsonObject();
        result.addProperty("content", content);
        result.addProperty("format", format);
        return RpcResponse.success(result);
    }

    private RpcResponse handleExport(JsonObject params) {
        Document doc = documentManager.getActiveDocument();
        if (doc == null) {
            return RpcResponse.error(RpcError.internalError("No active document"));
        }

        String format = params.get("format").getAsString();
        Renderer renderer = switch (format) {
            case "html" -> new HtmlRenderer();
            case "plaintext" -> new PlainTextRenderer();
            default -> null;
        };

        if (renderer == null) {
            return RpcResponse.error(RpcError.invalidParams("Unknown format: " + format));
        }

        String content = renderer.render(doc.getAst());

        if (params.has("outputPath") && !params.get("outputPath").isJsonNull()) {
            String outPath = params.get("outputPath").getAsString();
            try {
                java.nio.file.Files.writeString(Path.of(outPath), content);
                JsonObject result = new JsonObject();
                result.addProperty("outputPath", outPath);
                result.addProperty("format", format);
                result.addProperty("bytesWritten", content.getBytes().length);
                return RpcResponse.success(result);
            } catch (java.io.IOException e) {
                return RpcResponse.error(RpcError.fromCode("-32004", "Write error: " + e.getMessage()));
            }
        }

        JsonObject result = new JsonObject();
        result.addProperty("content", content);
        result.addProperty("format", format);
        return RpcResponse.success(result);
    }
}
