package com.charmed.rpc;

import com.google.gson.JsonObject;

/** Response envelope for JSON-RPC results. Record for immutable data. */
public record RpcResponse(Object result, RpcError error) {

    public static RpcResponse success(Object result) {
        return new RpcResponse(result, null);
    }

    public static RpcResponse error(RpcError error) {
        return new RpcResponse(null, error);
    }

    public boolean isError() { return error != null; }
}
