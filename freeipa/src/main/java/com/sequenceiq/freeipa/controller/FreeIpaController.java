package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.freeipa.api.model.endpoint.FreeIpaEndpoint;
import com.sequenceiq.freeipa.api.model.freeipa.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.model.freeipa.CreateFreeIpaResponse;
import com.sequenceiq.freeipa.api.model.freeipa.FreeIpaDetailsResponse;
import com.sequenceiq.freeipa.service.stack.FreeIPADetails;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDetailsService;

@Controller
public class FreeIpaController implements FreeIpaEndpoint {

    @Inject
    private FreeIpaCreationService freeIpaCreationService;

    @Inject
    private FreeIpaDeletionService freeIpaDeletionService;

    @Inject
    private FreeIpaDetailsService freeIpaDetailsService;

    @Override
    public CreateFreeIpaResponse create(String environmentName, CreateFreeIpaRequest request) {
        //TODO: Parse Account Id from Header (crn)
        String accountId = "test_account";
        freeIpaCreationService.launchFreeIpa(request, environmentName, accountId);

        CreateFreeIpaResponse res = new CreateFreeIpaResponse();
        res.setStatus(Status.REQUESTED);
        res.setStatusReason("Started launching FreeIPA Server");
        return res;
    }

    @Override
    public void delete(String environmentName, String name) {
        //TODO: Parse Account Id from Header (crn)
        String accountId = "test_account";
        freeIpaDeletionService.delete(accountId, environmentName, name);
    }

    @Override
    public FreeIpaDetailsResponse get(String environmentName, String name) {
        //TODO: Parse Account Id from Header (crn)
        String accountId = "test_account";

        FreeIPADetails details = freeIpaDetailsService.details(accountId, environmentName, name);
        return convertAndGetResponse(details);
    }

    private FreeIpaDetailsResponse convertAndGetResponse(FreeIPADetails details) {
        // TODO: populate fields
        return new FreeIpaDetailsResponse();
    }
}
