package com.charmed.rpc;

/** JSON-RPC error with code, message, and optional data. */
public class RpcError {

    private final int code;
    private final String message;
    private final Object data;

    public RpcError(int code, String message, Object data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public int code() { return code; }
    public String message() { return message; }
    public Object data() { return data; }

    // Standard JSON-RPC errors
    public static RpcError parseError(String detail) {
        return new RpcError(-32700, "Parse error: " + detail, null);
    }

    public static RpcError invalidRequest(String detail) {
        return new RpcError(-32600, "Invalid request: " + detail, null);
    }

    public static RpcError methodNotFound(String method) {
        return new RpcError(-32601, "Method not found: " + method, null);
    }

    public static RpcError invalidParams(String detail) {
        return new RpcError(-32602, "Invalid params: " + detail, null);
    }

    public static RpcError internalError(String detail) {
        return new RpcError(-32603, "Internal error: " + detail, null);
    }

    // Application-specific errors
    public static RpcError fromCode(String errorCode, String message) {
        return new RpcError(Integer.parseInt(errorCode), message, null);
    }
}
