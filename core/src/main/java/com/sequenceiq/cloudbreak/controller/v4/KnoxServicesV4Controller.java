package com.sequenceiq.cloudbreak.controller.v4;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v4.knoxservices.KnoxServicesV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.knoxservices.responses.ExposedServiceV4Responses;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;

public class KnoxServicesV4Controller implements KnoxServicesV4Endpoint {

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Override
    public ExposedServiceV4Responses listByWorkspaceAndBlueprint(Long workspaceId, String blueprintName) {
        return ExposedServiceV4Responses.exposedServiceV4Responses(
                serviceEndpointCollector.getKnoxServices(workspaceId, blueprintName));
    }
}
