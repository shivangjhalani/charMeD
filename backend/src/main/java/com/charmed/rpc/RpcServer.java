package com.charmed.rpc;

import com.charmed.document.Document;
import com.charmed.document.DocumentManager;
import com.charmed.event.DocumentChangedEvent;
import com.charmed.event.EventBus;
import com.charmed.visitor.WordCountVisitor;
import com.google.gson.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON-RPC 2.0 server over stdin/stdout with Content-Length framing.
 * Uses virtual threads for async notification handling.
 */
public class RpcServer {

    private final BufferedInputStream in;
    private final OutputStream out;
    private final Map<String, RpcHandler> handlers = new HashMap<>();
    private final Gson gson = new GsonBuilder().serializeNulls().create();
    private final DocumentManager documentManager;
    private final EventBus eventBus;
    private volatile boolean running = true;

    public RpcServer(InputStream in, OutputStream out,
                     DocumentManager documentManager, EventBus eventBus) {
        this.in = new BufferedInputStream(in);
        this.out = out;
        this.documentManager = documentManager;
        this.eventBus = eventBus;

        // Subscribe to events → send notifications
        eventBus.subscribe(DocumentChangedEvent.class, this::onDocumentChanged);
    }

    public void registerHandler(String prefix, RpcHandler handler) {
        handlers.put(prefix, handler);
    }

    /** Main message loop — blocks until EOF or shutdown. */
    public void run() {
        while (running) {
            try {
                String message = readMessage();
                if (message == null) break; // EOF

                JsonObject json = JsonParser.parseString(message).getAsJsonObject();

                if (!json.has("id")) {
                    // Notification from client (no response needed)
                    handleNotification(json);
                    continue;
                }

                // Request — needs a response
                long id = json.get("id").getAsLong();
                String method = json.get("method").getAsString();
                JsonObject params = json.has("params")
                        ? json.getAsJsonObject("params") : new JsonObject();

                RpcResponse response = dispatch(method, params);
                sendResponse(id, response);

            } catch (IOException e) {
                if (running) {
                    System.err.println("RPC read error: " + e.getMessage());
                }
                break;
            } catch (Exception e) {
                System.err.println("RPC processing error: " + e.getMessage());
                e.printStackTrace(System.err);
            }
        }
    }

    private RpcResponse dispatch(String method, JsonObject params) {
        // Special lifecycle methods
        if ("initialize".equals(method)) {
            return handleInitialize(params);
        }
        if ("shutdown".equals(method)) {
            running = false;
            return RpcResponse.success(null);
        }

        // Route to handler by method prefix
        for (var entry : handlers.entrySet()) {
            if (method.startsWith(entry.getKey())) {
                return entry.getValue().handle(method, params);
            }
        }
        return RpcResponse.error(RpcError.methodNotFound(method));
    }

    private RpcResponse handleInitialize(JsonObject params) {
        JsonObject result = new JsonObject();
        result.addProperty("serverVersion", "0.1.0");
        JsonObject capabilities = new JsonObject();
        capabilities.addProperty("undo", true);
        capabilities.addProperty("redo", true);
        capabilities.addProperty("search", true);
        JsonArray exportFormats = new JsonArray();
        exportFormats.add("markdown");
        exportFormats.add("html");
        exportFormats.add("plaintext");
        capabilities.add("export", exportFormats);
        result.add("capabilities", capabilities);
        return RpcResponse.success(result);
    }

    private void handleNotification(JsonObject json) {
        // Client notifications (if any) handled here
        String method = json.has("method") ? json.get("method").getAsString() : "";
        System.err.println("Received notification: " + method);
    }

    // --- Event handlers → send notifications to Go ---

    private void onDocumentChanged(DocumentChangedEvent event) {
        Thread.startVirtualThread(() -> {
            Document doc = documentManager.getActiveDocument();
            if (doc == null) return;

            WordCountVisitor wc = new WordCountVisitor();
            doc.getAst().accept(wc);

            JsonObject params = new JsonObject();
            params.addProperty("content", doc.getContent());
            JsonArray lines = new JsonArray();
            for (String line : doc.getLines()) lines.add(line);
            params.add("lines", lines);
            JsonObject cursor = new JsonObject();
            cursor.addProperty("line", doc.getCursor().line());
            cursor.addProperty("column", doc.getCursor().column());
            params.add("cursor", cursor);
            params.addProperty("dirty", doc.isDirty());

            try { sendNotification("ui/refresh", params); }
            catch (IOException e) { System.err.println("Failed to send notification: " + e.getMessage()); }
        });
    }


    // --- Content-Length framed I/O ---

    private String readMessage() throws IOException {
        // Read Content-Length header
        String header = readLine();
        if (header == null) return null;

        while (!header.startsWith("Content-Length:")) {
            header = readLine();
            if (header == null) return null;
        }

        int length = Integer.parseInt(header.substring("Content-Length:".length()).trim());

        // Skip blank line
        readLine();

        // Read body
        byte[] body = new byte[length];
        int read = 0;
        while (read < length) {
            int n = in.read(body, read, length - read);
            if (n == -1) return null;
            read += n;
        }
        return new String(body, StandardCharsets.UTF_8);
    }

    private String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = in.read()) != -1) {
            if (c == '\r') {
                int next = in.read(); // consume \n
                if (next != '\n' && next != -1) {
                    sb.append((char) c);
                    sb.append((char) next);
                    continue;
                }
                break;
            }
            if (c == '\n') break;
            sb.append((char) c);
        }
        return c == -1 && sb.isEmpty() ? null : sb.toString();
    }

    private synchronized void sendResponse(long id, RpcResponse response) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("jsonrpc", "2.0");
        json.addProperty("id", id);

        if (response.isError()) {
            JsonObject err = new JsonObject();
            err.addProperty("code", response.error().code());
            err.addProperty("message", response.error().message());
            json.add("error", err);
        } else {
            json.add("result", gson.toJsonTree(response.result()));
        }
        writeMessage(gson.toJson(json));
    }

    private synchronized void sendNotification(String method, JsonObject params) throws IOException {
        JsonObject json = new JsonObject();
        json.addProperty("jsonrpc", "2.0");
        json.addProperty("method", method);
        json.add("params", params);
        writeMessage(gson.toJson(json));
    }

    private void writeMessage(String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        String header = "Content-Length: " + body.length + "\r\n\r\n";
        out.write(header.getBytes(StandardCharsets.UTF_8));
        out.write(body);
        out.flush();
    }
}
