package com.sequenceiq.freeipa.controller;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.controller.exception.BadRequestException;
import com.sequenceiq.freeipa.controller.validation.AttachChildEnvironmentRequestValidator;
import com.sequenceiq.freeipa.controller.validation.CreateFreeIpaRequestValidator;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.stack.ChildEnvironmentService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDescribeService;
import com.sequenceiq.freeipa.service.stack.FreeIpaHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.FreeIpaListService;
import com.sequenceiq.freeipa.service.stack.FreeIpaRootCertificateService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStartService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStopService;
import com.sequenceiq.freeipa.service.stack.RebootInstancesService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@AuthorizationResource(type = AuthorizationResourceType.ENVIRONMENT)
public class FreeIpaV1Controller implements FreeIpaV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaV1Controller.class);

    @Inject
    private FreeIpaCreationService freeIpaCreationService;

    @Inject
    private ChildEnvironmentService childEnvironmentService;

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
    private RebootInstancesService rebootInstancesService;

    @Inject
    private CrnService crnService;

    @Inject
    private CreateFreeIpaRequestValidator createFreeIpaRequestValidator;

    @Inject
    private AttachChildEnvironmentRequestValidator attachChildEnvironmentRequestValidator;

    @Inject
    private FreeIpaStartService freeIpaStartService;

    @Inject
    private FreeIpaStopService freeIpaStopService;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
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
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void attachChildEnvironment(@Valid AttachChildEnvironmentRequest request) {
        ValidationResult validationResult = attachChildEnvironmentRequestValidator.validate(request);
        if (validationResult.hasError()) {
            LOGGER.debug("AttachChildEnvironmentRequest has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        String accountId = crnService.getCurrentAccountId();
        childEnvironmentService.attachChildEnvironment(request, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void detachChildEnvironment(@Valid DetachChildEnvironmentRequest request) {
        String accountId = crnService.getCurrentAccountId();
        childEnvironmentService.detachChildEnvironment(request, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public DescribeFreeIpaResponse describe(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaDescribeService.describe(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public List<ListFreeIpaResponse> list() {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaListService.list(accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public HealthDetailsFreeIpaResponse healthDetails(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaHealthDetailsService.getHealthDetails(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public String getRootCertificate(String environmentCrn) {
        try {
            String accountId = crnService.getCurrentAccountId();
            return freeIpaRootCertificateService.getRootCertificate(environmentCrn, accountId);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void delete(String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaDeletionService.delete(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public OperationStatus cleanup(@Valid CleanupRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        return cleanupService.cleanup(accountId, request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void rebootInstances(@Valid RebootInstancesRequest request) throws FreeIpaClientException {
        String accountId = crnService.getCurrentAccountId();
        rebootInstancesService.rebootInstances(accountId, request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void start(@NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaStartService.start(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void stop(@NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaStopService.stop(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public String registerWithClusterProxy(@NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return clusterProxyService.registerFreeIpa(accountId, environmentCrn).toString();
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deregisterWithClusterProxy(@NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        clusterProxyService.deregisterFreeIpa(accountId, environmentCrn);
    }
}
