package com.sequenceiq.cloudbreak.controller;

import java.util.Set;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v3.ManagementPackV3Endpoint;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackRequest;
import com.sequenceiq.cloudbreak.api.model.mpack.ManagementPackResponse;

@Controller
public class ManagementPackV3Controller extends NotificationController implements ManagementPackV3Endpoint {

    @Override
    public Set<ManagementPackResponse> listByOrganization(Long organizationId) {
        return null;
    }

    @Override
    public ManagementPackResponse getByNameInOrganization(Long organizationId, String name) {
        return null;
    }

    @Override
    public ManagementPackResponse createInOrganization(Long organizationId, @Valid ManagementPackRequest request) {
        return null;
    }

    @Override
    public ManagementPackResponse deleteInOrganization(Long organizationId, String name) {
        return null;
    }
}
