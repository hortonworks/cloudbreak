package com.sequenceiq.freeipa.controller;

import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@Controller
public class FreeIpaV1Controller implements FreeIpaV1Endpoint {
    @Override
    public DescribeFreeIpaResponse create(@Valid CreateFreeIpaRequest request) {
        return null;
    }

    @Override
    public DescribeFreeIpaResponse describe(String environmentId) {
        return null;
    }

    @Override
    public void delete(String environmentId) {

    }
}
