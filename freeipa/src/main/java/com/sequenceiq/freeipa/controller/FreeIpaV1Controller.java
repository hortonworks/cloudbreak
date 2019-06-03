package com.sequenceiq.freeipa.controller;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDescribeService;
import com.sequenceiq.freeipa.service.stack.FreeIpaListService;
import com.sequenceiq.freeipa.service.stack.FreeIpaRootCertificateService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
public class FreeIpaV1Controller implements FreeIpaV1Endpoint {

    @Inject
    private FreeIpaCreationService freeIpaCreationService;

    @Inject
    private FreeIpaDeletionService freeIpaDeletionService;

    @Inject
    private FreeIpaDescribeService freeIpaDescribeService;

    @Inject
    private FreeIpaListService freeIpaListService;

    @Inject
    private FreeIpaRootCertificateService freeIpaRootCertificateService;

    @Inject
    private CrnService crnService;

    @Override
    public DescribeFreeIpaResponse create(@Valid CreateFreeIpaRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaCreationService.launchFreeIpa(request, accountId);
    }

    @Override
    public DescribeFreeIpaResponse describe(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaDescribeService.describe(environmentCrn, accountId);
    }

    @Override
    public List<ListFreeIpaResponse> list() {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaListService.list(accountId);
    }

    @Override
    public String getRootCertificate(String environmentCrn) {
        try {
            String accountId = crnService.getCurrentAccountId();
            return freeIpaRootCertificateService.getRootCertificate(environmentCrn, accountId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaDeletionService.delete(environmentCrn, accountId);
    }
}
