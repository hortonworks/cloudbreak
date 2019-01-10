package com.sequenceiq.cloudbreak.controller;

import java.util.Collection;

import javax.inject.Inject;

import com.sequenceiq.cloudbreak.api.endpoint.v3.KnoxServicesV3Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.ExposedServiceV4Response;
import com.sequenceiq.cloudbreak.service.ServiceEndpointCollector;

public class KnoxServicesV3Controller implements KnoxServicesV3Endpoint {

    @Inject
    private ServiceEndpointCollector serviceEndpointCollector;

    @Override
    public Collection<ExposedServiceV4Response> listByWorkspaceAndBlueprint(Long workspaceId, String blueprintName) {
        return serviceEndpointCollector.getKnoxServices(workspaceId, blueprintName);
    }
}
