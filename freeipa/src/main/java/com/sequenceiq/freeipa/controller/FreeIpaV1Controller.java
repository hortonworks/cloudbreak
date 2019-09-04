package com.sequenceiq.freeipa.controller;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentCrn;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.EnvironmentCrn;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.ResourceType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.service.freeipa.CleanupService;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDescribeService;
import com.sequenceiq.freeipa.service.stack.FreeIpaListService;
import com.sequenceiq.freeipa.service.stack.FreeIpaRootCertificateService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@AuthorizationResource(type = ResourceType.ENVIRONMENT)
public class FreeIpaV1Controller implements FreeIpaV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaV1Controller.class);

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
    private CleanupService cleanupService;

    @Inject
    private CrnService crnService;

    @Inject
    private Validator<CreateFreeIpaRequest> createFreeIpaRequestValidator;

    @Override
    @CheckPermissionByEnvironmentCrn(action = ResourceAction.WRITE)
    public DescribeFreeIpaResponse create(@EnvironmentCrn @Valid CreateFreeIpaRequest request) {
        ValidationResult validationResult = createFreeIpaRequestValidator.validate(request);
        if (validationResult.getState() == State.ERROR) {
            LOGGER.debug("FreeIPA request has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        String accountId = crnService.getCurrentAccountId();
        return freeIpaCreationService.launchFreeIpa(request, accountId);
    }

    @Override
    @DisableCheckPermissions
    public DescribeFreeIpaResponse describe(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaDescribeService.describe(environmentCrn, accountId);
    }

    @Override
    @DisableCheckPermissions
    public List<ListFreeIpaResponse> list() {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaListService.list(accountId);
    }

    @Override
    @DisableCheckPermissions
    public String getRootCertificate(String environmentCrn) {
        try {
            String accountId = crnService.getCurrentAccountId();
            return freeIpaRootCertificateService.getRootCertificate(environmentCrn, accountId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @CheckPermissionByEnvironmentCrn(action = ResourceAction.WRITE)
    public void delete(@EnvironmentCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaDeletionService.delete(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByEnvironmentCrn(action = ResourceAction.WRITE)
    public CleanupResponse cleanup(@EnvironmentCrn @Valid CleanupRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return cleanupService.cleanup(accountId, request);
    }
}
