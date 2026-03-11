package com.charmed.rpc;

import com.google.gson.JsonObject;

/** Handler interface for RPC method dispatch. */
public interface RpcHandler {
    RpcResponse handle(String method, JsonObject params);
}
