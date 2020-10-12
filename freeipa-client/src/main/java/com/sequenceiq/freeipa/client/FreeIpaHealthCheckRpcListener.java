package com.sequenceiq.freeipa.client;

import javax.ws.rs.core.Response;

public interface FreeIpaHealthCheckRpcListener {
    void onBeforeResponseProcessed(Response response) throws Exception;
}
