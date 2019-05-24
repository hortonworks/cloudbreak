package com.sequenceiq.freeipa.controller;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDescribeService;
import com.sequenceiq.freeipa.service.stack.FreeIpaRootCertificateService;

@Controller
public class FreeIpaV1Controller implements FreeIpaV1Endpoint {

    @Inject
    private FreeIpaCreationService freeIpaCreationService;

    @Inject
    private FreeIpaDeletionService freeIpaDeletionService;

    @Inject
    private FreeIpaDescribeService freeIpaDescribeService;

    @Inject
    private FreeIpaRootCertificateService freeIpaRootCertificateService;

    @Override
    public DescribeFreeIpaResponse create(@Valid CreateFreeIpaRequest request) {
        // TODO parse account from header
        String accountId = "test_account";
        return freeIpaCreationService.launchFreeIpa(request, accountId);
    }

    @Override
    public DescribeFreeIpaResponse describe(String environmentCrn) {
        return freeIpaDescribeService.describe(environmentCrn);
    }

    @Override
    public String getRootCertificate(String environmentCrn) {
        try {
            return freeIpaRootCertificateService.getRootCertificate(environmentCrn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String environmentCrn) {
        freeIpaDeletionService.delete(environmentCrn);
    }
}
