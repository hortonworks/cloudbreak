package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.model.endpoint.FreeIpaEndpoint;
import com.sequenceiq.freeipa.api.model.freeipa.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.model.freeipa.CreateFreeIpaResponse;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;

@Controller
public class FreeIpaController implements FreeIpaEndpoint {

    @Inject
    private FreeIpaCreationService freeIpaCreationService;

    @Inject
    private FreeIpaDeletionService freeIpaDeletionService;

    @Override
    public CreateFreeIpaResponse create(String environmentName, CreateFreeIpaRequest request) {
        // TODO: Get customer id (tenant from header crn)
        String tenant = "test_tenant";
        freeIpaCreationService.launchFreeIpa(request, environmentName, tenant);
        return new CreateFreeIpaResponse();
    }

    @Override
    public void delete(String environmentName, String name) {
        freeIpaDeletionService.delete(environmentName, name);
    }
}
