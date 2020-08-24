package com.sequenceiq.sdx.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxUpgradeEndpoint;

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
    public SdxUpgradeEndpoint sdxUpgradeEndpoint() {
        return getEndpoint(SdxUpgradeEndpoint.class);
    }

    @Override
    public FlowEndpoint flowEndpoint() {
        return getEndpoint(FlowEndpoint.class);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        return getEndpoint(FlowPublicEndpoint.class);
    }
}
