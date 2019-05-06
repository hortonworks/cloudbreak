package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.model.create.CreateEndpoint;
import com.sequenceiq.freeipa.api.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.model.create.CreateFreeIpaResponse;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;

@Controller
public class CreateController implements CreateEndpoint {

    @Inject
    private FreeIpaCreationService freeIpaCreationService;

    @Override
    public CreateFreeIpaResponse create(CreateFreeIpaRequest request) {
        freeIpaCreationService.launchFreeIpa(request);
        return new CreateFreeIpaResponse();
    }
}
