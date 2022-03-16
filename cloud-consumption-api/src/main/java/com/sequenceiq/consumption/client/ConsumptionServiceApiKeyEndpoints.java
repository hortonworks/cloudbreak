package com.sequenceiq.consumption.client;

import javax.ws.rs.client.WebTarget;

import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.client.AbstractKeyBasedServiceEndpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public class ConsumptionServiceApiKeyEndpoints extends AbstractKeyBasedServiceEndpoint implements ConsumptionClient {

    protected ConsumptionServiceApiKeyEndpoints(WebTarget webTarget, String accessKey, String secretKey) {
        super(webTarget, accessKey, secretKey);
    }

    @Override
    public FlowEndpoint flowEndpoint() {
        return getEndpoint(FlowEndpoint.class);
    }

    @Override
    public FlowPublicEndpoint flowPublicEndpoint() {
        return getEndpoint(FlowPublicEndpoint.class);
    }

    @Override
    public CDPStructuredEventV1Endpoint structuredEventsV1Endpoint() {
        return getEndpoint(CDPStructuredEventV1Endpoint.class);
    }

    @Override
    public AuthorizationUtilEndpoint authorizationUtilEndpoint() {
        return getEndpoint(AuthorizationUtilEndpoint.class);
    }

}
