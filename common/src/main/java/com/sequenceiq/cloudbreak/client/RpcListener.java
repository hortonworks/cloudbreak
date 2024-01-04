package com.sequenceiq.cloudbreak.client;

import jakarta.ws.rs.core.Response;

public interface RpcListener {
    void onBeforeResponseProcessed(Response response) throws Exception;
}
