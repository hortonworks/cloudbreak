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
        //TODO: Parse Account Id from Header (crn)
        String accountId = "test_account";
        freeIpaCreationService.launchFreeIpa(request, environmentName, accountId);
        return new CreateFreeIpaResponse();
    }

    @Override
    public void delete(String environmentName, String name) {
        //TODO: Parse Account Id from Header (crn)
        String accountId = "test_account";
        freeIpaDeletionService.delete(accountId, environmentName, name);
    }
}
