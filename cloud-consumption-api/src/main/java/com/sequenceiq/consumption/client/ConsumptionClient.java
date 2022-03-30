package com.sequenceiq.consumption.client;

import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.cloudbreak.structuredevent.rest.endpoint.CDPStructuredEventV1Endpoint;
import com.sequenceiq.flow.api.FlowEndpoint;
import com.sequenceiq.flow.api.FlowPublicEndpoint;

public interface ConsumptionClient {

    FlowEndpoint flowEndpoint();

    FlowPublicEndpoint flowPublicEndpoint();

    CDPStructuredEventV1Endpoint structuredEventsV1Endpoint();

    AuthorizationUtilEndpoint authorizationUtilEndpoint();

}