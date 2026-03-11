package com.charmed;

import com.charmed.document.DocumentManager;
import com.charmed.editor.Editor;
import com.charmed.event.EventBus;
import com.charmed.rpc.DocumentHandler;
import com.charmed.rpc.EditorHandler;
import com.charmed.rpc.RpcServer;

/**
 * Entry point for the charMeD backend. Wires all components together
 * and starts the JSON-RPC message loop on stdin/stdout.
 *
 * <p>Manual wiring (no DI framework) — keeps OOP patterns explicit.</p>
 */
public final class App {

    public static void main(String[] args) {
        // 1. Create EventBus (Observer pattern)
        EventBus eventBus = new EventBus();

        // 2. Create DocumentManager
        DocumentManager documentManager = new DocumentManager(eventBus);

        // 3. Create Editor (State pattern)
        Editor editor = new Editor(documentManager, eventBus);

        // 4. Create RPC handlers (Adapter pattern)
        DocumentHandler documentHandler = new DocumentHandler(documentManager, editor);
        EditorHandler editorHandler = new EditorHandler(editor);

        // 5. Create and configure RPC server
        RpcServer server = new RpcServer(System.in, System.out, documentManager, eventBus);
        server.registerHandler("document/", documentHandler);
        server.registerHandler("editor/", editorHandler);

        // 6. Log startup to stderr (not stdout — stdout is protocol)
        System.err.println("charMeD backend v0.1.0 started");

        // 7. Run the message loop (blocks until shutdown or EOF)
        server.run();

        System.err.println("charMeD backend shutdown");
    }
}
