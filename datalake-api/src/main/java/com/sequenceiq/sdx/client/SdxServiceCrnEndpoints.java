package com.sequenceiq.sdx.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.cloudbreak.client.AbstractUserCrnServiceEndpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxEndpoint;
import com.sequenceiq.sdx.api.endpoint.SdxInternalEndpoint;

public class SdxServiceCrnEndpoints extends AbstractUserCrnServiceEndpoint implements SdxClient {

    protected SdxServiceCrnEndpoints(WebTarget webTarget, String crn) {
        super(webTarget, crn);
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
