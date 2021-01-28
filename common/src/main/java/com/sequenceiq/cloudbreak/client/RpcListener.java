package com.sequenceiq.cloudbreak.client;

import javax.ws.rs.core.Response;

public interface RpcListener {
    void onBeforeResponseProcessed(Response response) throws Exception;
}
