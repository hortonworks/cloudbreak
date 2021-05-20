package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.REPAIR_FREEIPA;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import java.util.List;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.cloudbreak.validation.ValidationResult;
import com.sequenceiq.cloudbreak.validation.ValidationResult.State;
import com.sequenceiq.freeipa.api.v1.freeipa.cleanup.CleanupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.FreeIpaV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.binduser.BindUserCreateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.detachchildenv.DetachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.health.HealthDetailsFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.reboot.RebootInstancesRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.repair.RepairInstancesRequest;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationStatus;
import com.sequenceiq.freeipa.authorization.FreeIpaFiltering;
import com.sequenceiq.freeipa.client.FreeIpaClientException;
import com.sequenceiq.freeipa.client.FreeIpaClientExceptionWrapper;
import com.sequenceiq.freeipa.controller.validation.AttachChildEnvironmentRequestValidator;
import com.sequenceiq.freeipa.controller.validation.CreateFreeIpaRequestValidator;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.binduser.BindUserCreateService;
import com.sequenceiq.freeipa.service.freeipa.cert.root.FreeIpaRootCertificateService;
import com.sequenceiq.freeipa.service.freeipa.cleanup.CleanupService;
import com.sequenceiq.freeipa.service.stack.ChildEnvironmentService;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.FreeIpaCreationService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDeletionService;
import com.sequenceiq.freeipa.service.stack.FreeIpaDescribeService;
import com.sequenceiq.freeipa.service.stack.FreeIpaListService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStackHealthDetailsService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStartService;
import com.sequenceiq.freeipa.service.stack.FreeIpaStopService;
import com.sequenceiq.freeipa.service.stack.RepairInstancesService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@AccountEntityType(Stack.class)
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
    private FreeIpaStackHealthDetailsService freeIpaStackHealthDetailsService;

    @Inject
    private FreeIpaRootCertificateService freeIpaRootCertificateService;

    @Inject
    private CleanupService cleanupService;

    @Inject
    private RepairInstancesService repairInstancesService;

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

    @Inject
    private FreeIpaFiltering freeIpaFiltering;

    @Inject
    private BindUserCreateService bindUserCreateService;

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public DescribeFreeIpaResponse create(@RequestObject @Valid CreateFreeIpaRequest request) {
        ValidationResult validationResult = createFreeIpaRequestValidator.validate(request);
        if (validationResult.getState() == State.ERROR) {
            LOGGER.debug("FreeIPA request has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        String accountId = crnService.getCurrentAccountId();
        return freeIpaCreationService.launchFreeIpa(request, accountId);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "parentEnvironmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    @CheckPermissionByRequestProperty(path = "childEnvironmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public void attachChildEnvironment(@RequestObject @Valid AttachChildEnvironmentRequest request) {
        ValidationResult validationResult = attachChildEnvironmentRequestValidator.validate(request);
        if (validationResult.hasError()) {
            LOGGER.debug("AttachChildEnvironmentRequest has validation error(s): {}.", validationResult.getFormattedErrors());
            throw new BadRequestException(validationResult.getFormattedErrors());
        }
        String accountId = crnService.getCurrentAccountId();
        childEnvironmentService.attachChildEnvironment(request, accountId);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "parentEnvironmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    @CheckPermissionByRequestProperty(path = "childEnvironmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public void detachChildEnvironment(@RequestObject @Valid DetachChildEnvironmentRequest request) {
        String accountId = crnService.getCurrentAccountId();
        childEnvironmentService.detachChildEnvironment(request, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public DescribeFreeIpaResponse describe(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaDescribeService.describe(environmentCrn, accountId);
    }

    @Override
    @InternalOnly
    public DescribeFreeIpaResponse describeInternal(@ResourceCrn String environmentCrn, @AccountId String accountId) {
        return freeIpaDescribeService.describe(environmentCrn, accountId);
    }

    @Override
    @FilterListBasedOnPermissions(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT, filter = FreeIpaFiltering.class)
    public List<ListFreeIpaResponse> list() {
        return freeIpaFiltering.filterFreeIpas(AuthorizationResourceAction.DESCRIBE_ENVIRONMENT);
    }

    @Override
    @InternalOnly
    public List<ListFreeIpaResponse> listInternal(@AccountId String accountId) {
        return freeIpaListService.list(accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public HealthDetailsFreeIpaResponse healthDetails(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return freeIpaStackHealthDetailsService.getHealthDetails(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_ENVIRONMENT)
    public String getRootCertificate(@ResourceCrn @TenantAwareParam String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        try {
            return freeIpaRootCertificateService.getRootCertificate(environmentCrn, accountId);
        } catch (FreeIpaClientException e) {
            throw new FreeIpaClientExceptionWrapper(e);
        }
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_ENVIRONMENT)
    public void delete(@ResourceCrn String environmentCrn, boolean forced) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaDeletionService.delete(environmentCrn, accountId, forced);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public OperationStatus cleanup(@RequestObject @Valid CleanupRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return internalCleanup(request, accountId);
    }

    @Override
    @InternalOnly
    public OperationStatus internalCleanup(@Valid CleanupRequest request, @AccountId String accountId) {
        return cleanupService.cleanup(accountId, request);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = REPAIR_FREEIPA)
    public OperationStatus rebootInstances(@RequestObject @Valid RebootInstancesRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return repairInstancesService.rebootInstances(accountId, request);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = REPAIR_FREEIPA)
    public OperationStatus repairInstances(@RequestObject @Valid RepairInstancesRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return repairInstancesService.repairInstances(accountId, request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_ENVIRONMENT)
    public void start(@ResourceCrn @NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaStartService.start(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.STOP_ENVIRONMENT)
    public void stop(@ResourceCrn @NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        freeIpaStopService.stop(environmentCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public String registerWithClusterProxy(@ResourceCrn @NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return clusterProxyService.registerFreeIpa(accountId, environmentCrn).toString();
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public void deregisterWithClusterProxy(@ResourceCrn @NotEmpty String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        clusterProxyService.deregisterFreeIpa(accountId, environmentCrn);
    }

    @Override
    @InternalOnly
    public OperationStatus createBindUser(@Valid @NotNull BindUserCreateRequest request, @InitiatorUserCrn @NotEmpty String initiatorUserCrn) {
        String accountId = crnService.getCurrentAccountId();
        return bindUserCreateService.createBindUser(accountId, request);
    }
}
