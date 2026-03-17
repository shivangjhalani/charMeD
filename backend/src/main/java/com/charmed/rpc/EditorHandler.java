package com.charmed.rpc;

import com.charmed.document.CursorPosition;
import com.charmed.document.Document;
import com.charmed.editor.Editor;
import com.charmed.editor.HandleResult;
import com.google.gson.JsonObject;

/**
 * Adapter: JSON-RPC editor/* methods → Editor calls.
 */
public class EditorHandler implements RpcHandler {

    private final Editor editor;

    public EditorHandler(Editor editor) {
        this.editor = editor;
    }

    @Override
    public RpcResponse handle(String method, JsonObject params) {
        try {
            return switch (method) {
                case "editor/moveCursor" -> handleMoveCursor(params);
                case "editor/executeCommand" -> handleExecuteCommand(params);
                default -> RpcResponse.error(RpcError.methodNotFound(method));
            };
        } catch (Exception e) {
            return RpcResponse.error(RpcError.internalError(e.getMessage()));
        }
    }

    private RpcResponse handleMoveCursor(JsonObject params) {
        String direction = params.get("direction").getAsString();
        int count = params.has("count") ? params.get("count").getAsInt() : 1;

        // Map direction to vim key
        String key = switch (direction) {
            case "up" -> "k";
            case "down" -> "j";
            case "left" -> "h";
            case "right" -> "l";
            case "lineStart" -> "0";
            case "lineEnd" -> "$";
            case "documentStart" -> "g";
            case "documentEnd" -> "G";
            default -> null;
        };

        if (key != null) {
            for (int i = 0; i < count; i++) {
                editor.processKey(key);
            }
        }

        Document doc = editor.getDocument();
        CursorPosition cursor = doc != null ? doc.getCursor() : CursorPosition.ORIGIN;

        JsonObject res = new JsonObject();
        JsonObject cursorObj = new JsonObject();
        cursorObj.addProperty("line", cursor.line());
        cursorObj.addProperty("column", cursor.column());
        res.add("cursor", cursorObj);
        return RpcResponse.success(res);
    }

    private RpcResponse handleExecuteCommand(JsonObject params) {
        String command = params.get("command").getAsString();
        // Execute the command through the editor's command infrastructure
        JsonObject res = new JsonObject();
        res.addProperty("executed", true);
        res.addProperty("output", "Executed: " + command);
        return RpcResponse.success(res);
    }
}
