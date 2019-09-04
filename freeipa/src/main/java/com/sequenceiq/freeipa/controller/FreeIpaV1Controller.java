package com.sequenceiq.freeipa.controller;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.cloudbreak.validation.Validator;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDescribeService;
import com.sequenceiq.freeipa.service.stack.FreeIpaListService;
import com.sequenceiq.freeipa.service.stack.FreeIpaRootCertificateService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStartService;
import com.sequenceiq.freeipa.service.stack.FreeIpaHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStopService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@AuthorizationResource(type = AuthorizationResourceType.ENVIRONMENT)
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
    private FreeIpaHealthDetailsService freeIpaHealthDetailsService;

    @Inject
    private FreeIpaRootCertificateService freeIpaRootCertificateService;

    @Inject
    private CleanupService cleanupService;

    @Inject
    private CrnService crnService;

    @Inject
    private Validator<CreateFreeIpaRequest> createFreeIpaRequestValidator;

    @Inject
    private FreeIpaStartService freeIpaStartService;

    @Inject
    private FreeIpaStopService freeIpaStopService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    @CheckPermissionByAccount
    public DescribeFreeIpaResponse create(@Valid CreateFreeIpaRequest request) {
        ValidationResult validationResult = createFreeIpaRequestValidator.validate(request);
        if (validationResult.getState() == State.ERROR) {
            LOGGER.debug("FreeIPA request has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        String accountId = crnService.getCurrentAccountId();
        return freeIpaCreationService.launchFreeIpa(request, accountId);
    }

    @Override
    @CheckPermissionByAccount
    public DescribeFreeIpaResponse describe(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaDescribeService.describe(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount
    public List<ListFreeIpaResponse> list() {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaListService.list(accountId);
    }

    @Override
    @CheckPermissionByAccount
    public HealthDetailsFreeIpaResponse healthDetails(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaHealthDetailsService.getHealthDetails(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount
    public String getRootCertificate(String environmentCrn) {
        try {
            String accountId = crnService.getCurrentAccountId();
            return freeIpaRootCertificateService.getRootCertificate(environmentCrn, accountId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @CheckPermissionByAccount
    public void delete(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaDeletionService.delete(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount
    public OperationStatus cleanup(@Valid CleanupRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return cleanupService.cleanup(accountId, request);
    }

    @Override
    @CheckPermissionByAccount
    public void start(@NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaStartService.start(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount
    public void stop(@NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaStopService.stop(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount
    public String registerWithClusterProxy(@NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return clusterProxyService.registerFreeIpa(accountId, environmentCrn).toString();
    }

    @Override
    @CheckPermissionByAccount
    public void deregisterWithClusterProxy(@NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        clusterProxyService.deregisterFreeIpa(accountId, environmentCrn);
    }
}