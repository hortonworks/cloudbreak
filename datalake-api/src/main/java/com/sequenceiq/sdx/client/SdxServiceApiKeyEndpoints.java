package com.sequenceiq.sdx.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;

public class SdxServiceApiKeyEndpoints extends AbstractKeyBasedServiceEndpoint implements SdxClient {

    protected SdxServiceApiKeyEndpoints(WebTarget webTarget, String accessKey, String secretKey) {
        super(webTarget, accessKey, secretKey);
    }

    @Override
    public SdxInternalEndpoint sdxInternalEndpoint() {
        return getEndpoint(SdxInternalEndpoint.class);
    }

    @Override
    public SdxEndpoint sdxEndpoint() {
        return getEndpoint(SdxEndpoint.class);
    }

    @Override
    public FlowEndpoint flowEndpoint() {
        return getEndpoint(FlowEndpoint.class);
    }
}
