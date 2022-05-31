package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DELETE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_RECIPE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ENVIRONMENT_CREATE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.RECOVER_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN_LIST;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME_LIST;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.FilterParam;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.DistroXSyncCmV1Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.cluster.ClusterDiagnosticsService;
import com.sequenceiq.cloudbreak.service.diagnostics.DiagnosticsService;
import com.sequenceiq.cloudbreak.service.operation.OperationService;
import com.sequenceiq.cloudbreak.service.progress.ProgressService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.converter.VmLogsToVmLogsResponseConverter;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXGenerateImageCatalogV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMaintenanceModeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.MultipleInstanceDeleteRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model.CmDiagnosticsCollectionV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model.DiagnosticsCollectionV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.authorization.DataHubFiltering;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXMaintenanceModeV1ToMainenanceModeV4Converter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXRepairV1RequestToClusterRepairV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXScaleV1RequestToStackScaleV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.service.DistroXService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.flow.api.model.RetryableFlowResponse.Builder;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.domain.RetryableFlow;

@Controller
public class DistroXV1Controller implements DistroXV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXV1Controller.class);

    @Lazy
    @Inject
    private StackOperations stackOperations;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private DistroXV1RequestToStackV4RequestConverter stackRequestConverter;

    @Inject
    private DistroXScaleV1RequestToStackScaleV4RequestConverter scaleRequestConverter;

    @Inject
    private DistroXRepairV1RequestToClusterRepairV4RequestConverter clusterRepairRequestConverter;

    @Inject
    private DistroXMaintenanceModeV1ToMainenanceModeV4Converter maintenanceModeConverter;

    @Inject
    private CloudbreakRestRequestThreadLocalService crnService;

    @Inject
    private DiagnosticsService diagnosticsService;

    @Inject
    private ProgressService progressService;

    @Inject
    private OperationService operationService;

    @Inject
    private VmLogsService vmLogsService;

    @Inject
    private VmLogsToVmLogsResponseConverter vmlogsConverter;

    @Inject
    private ClusterDiagnosticsService clusterDiagnosticsService;

    @Inject
    private StackOperationService stackOperationService;

    @Inject
    private DistroXService distroxService;

    @Inject
    private DataHubFiltering dataHubFiltering;

    @Override
    @FilterListBasedOnPermissions
    public StackViewV4Responses list(@FilterParam(DataHubFiltering.ENV_NAME) String environmentName,
            @FilterParam(DataHubFiltering.ENV_CRN) String environmentCrn) {
        return dataHubFiltering.filterDataHubs(DESCRIBE_DATAHUB, environmentName, environmentCrn);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentName", type = NAME, action = ENVIRONMENT_CREATE_DATAHUB)
    @CheckPermissionByRequestProperty(path = "image.catalog", type = NAME, action = DESCRIBE_IMAGE_CATALOG, skipOnNull = true)
    @CheckPermissionByRequestProperty(path = "cluster.blueprintName", type = NAME, action = DESCRIBE_CLUSTER_TEMPLATE)
    @CheckPermissionByRequestProperty(path = "allRecipes", type = NAME_LIST, action = DESCRIBE_RECIPE)
    public StackV4Response post(@Valid @RequestObject DistroXV1Request request) {
        return distroxService.post(request);
    }

    @Override
    @InternalOnly
    public StackV4Response postInternal(@InitiatorUserCrn @NotEmpty String initiatorUserCrn,
            String accountId, @Valid DistroXV1Request request) {
        return post(request);
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_DATAHUB)
    public StackV4Response getByName(@ResourceName String name, Set<String> entries) {
        return stackOperations.get(
                NameOrCrn.ofName(name),
                getWorkspaceIdForCurrentUser(),
                entries,
                StackType.WORKLOAD);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public StackV4Response getByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @ResourceCrn @TenantAwareParam String crn, Set<String> entries) {
        return stackOperations.get(
                NameOrCrn.ofCrn(crn),
                getWorkspaceIdForCurrentUser(),
                entries,
                StackType.WORKLOAD);
    }

    @Override
    @CheckPermissionByResourceName(action = DELETE_DATAHUB)
    public void deleteByName(@ResourceName String name, Boolean forced) {
        stackOperations.delete(NameOrCrn.ofName(name), getWorkspaceIdForCurrentUser(), forced);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DELETE_DATAHUB)
    public void deleteByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn, Boolean forced) {
        stackOperations.delete(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser(), forced);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "names", type = NAME_LIST, action = DELETE_DATAHUB)
    @CheckPermissionByRequestProperty(path = "crns", type = CRN_LIST, action = DELETE_DATAHUB)
    public void deleteMultiple(@RequestObject DistroXMultiDeleteV1Request multiDeleteRequest, Boolean forced) {
        validateMultidelete(multiDeleteRequest);
        if (CollectionUtils.isNotEmpty(multiDeleteRequest.getNames())) {
            multideleteByNames(multiDeleteRequest, forced);
        } else {
            multideleteByCrn(multiDeleteRequest, forced);
        }
    }

    private void validateMultidelete(DistroXMultiDeleteV1Request multiDeleteRequest) {
        if (CollectionUtils.isNotEmpty(multiDeleteRequest.getNames()) && CollectionUtils.isNotEmpty(multiDeleteRequest.getCrns())) {
            throw new BadRequestException("Both names and crns cannot be provided, only one of them.");
        }
        if (CollectionUtils.isEmpty(multiDeleteRequest.getNames()) && CollectionUtils.isEmpty(multiDeleteRequest.getCrns())) {
            throw new BadRequestException("No names or crns were provided. At least one name or crn should be provided.");
        }
    }

    private void multideleteByNames(DistroXMultiDeleteV1Request multiDeleteRequest, Boolean forced) {
        Set<NameOrCrn> nameOrCrns = multiDeleteRequest.getNames().stream()
                .map(NameOrCrn::ofName)
                .collect(Collectors.toSet());
        nameOrCrns.forEach(nameOrCrn -> stackOperations.delete(nameOrCrn, getWorkspaceIdForCurrentUser(), forced));
    }

    private void multideleteByCrn(DistroXMultiDeleteV1Request multiDeleteRequest, Boolean forced) {
        Set<NameOrCrn> nameOrCrns = multiDeleteRequest.getCrns().stream()
                .map(NameOrCrn::ofCrn)
                .collect(Collectors.toSet());
        nameOrCrns.forEach(accessDto -> stackOperations.delete(accessDto, getWorkspaceIdForCurrentUser(), forced));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SYNC_DATAHUB)
    public void syncByName(@ResourceName String name) {
        stackOperations.sync(NameOrCrn.ofName(name), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SYNC_DATAHUB)
    public void syncByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn) {
        stackOperations.sync(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RETRY_DATAHUB_OPERATION)
    public void retryByName(@ResourceName String name) {
        stackOperations.retry(
                NameOrCrn.ofName(name),
                getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_RETRYABLE_DATAHUB_OPERATION)
    public List<RetryableFlowResponse> listRetryableFlows(@ResourceName String name) {
        List<RetryableFlow> retryableFlows = stackOperations.getRetryableFlows(name, getWorkspaceIdForCurrentUser());
        return retryableFlows.stream()
                .map(retryable -> Builder.builder().setName(retryable.getName()).setFailDate(retryable.getFailDate()).build())
                .collect(Collectors.toList());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.RETRY_DATAHUB_OPERATION)
    public void retryByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn) {
        stackOperations.retry(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.STOP_DATAHUB)
    public FlowIdentifier putStopByName(@ResourceName String name) {
        return stackOperations.putStop(NameOrCrn.ofName(name), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.STOP_DATAHUB)
    public FlowIdentifier putStopByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn) {
        return stackOperations.putStop(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.STOP_DATAHUB)
    public void putStopByNames(@ResourceNameList List<String> names) {
        names.forEach(this::putStopByName);
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.STOP_DATAHUB)
    public void putStopByCrns(@ResourceCrnList List<String> crns) {
        crns.forEach(this::putStopByCrn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.START_DATAHUB)
    public FlowIdentifier putStartByName(@ResourceName String name) {
        return stackOperations.putStart(NameOrCrn.ofName(name), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_DATAHUB)
    public FlowIdentifier putStartByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn) {
        return stackOperations.putStart(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.START_DATAHUB)
    public void restartClusterServicesByCrns(@ResourceCrnList List<String> crns) {
        crns.forEach(this::restartClusterServicesByCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ROTATE_SALTUSER_PASSWORD_DATAHUB)
    public FlowIdentifier rotateSaltPasswordByCrn(@ResourceCrn String crn) {
        return stackOperations.rotateSaltPassword(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceNameList(action = AuthorizationResourceAction.START_DATAHUB)
    public void putStartByNames(@ResourceNameList List<String> names) {
        names.forEach(this::putStartByName);
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.START_DATAHUB)
    public void putStartByCrns(@ResourceCrnList List<String> crns) {
        crns.forEach(this::putStartByCrn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public FlowIdentifier putScalingByName(@ResourceName String name, @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperations.getStackByName(name).getId());
        return stackOperations.putScaling(NameOrCrn.ofName(name), getWorkspaceIdForCurrentUser(), stackScaleV4Request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public void putScalingByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn,
            @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperations.getStackByCrn(crn).getId());
        stackOperations.putScaling(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser(), stackScaleV4Request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public FlowIdentifier repairClusterByName(@ResourceName String name, @Valid DistroXRepairV1Request clusterRepairRequest) {
        return stackOperations.repairCluster(
                NameOrCrn.ofName(name),
                getWorkspaceIdForCurrentUser(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public FlowIdentifier repairClusterByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn,
            @Valid DistroXRepairV1Request clusterRepairRequest) {
        return stackOperations.repairCluster(
                NameOrCrn.ofCrn(crn),
                getWorkspaceIdForCurrentUser(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));
    }

    @Override
    @CheckPermissionByRequestProperty(type = NAME, path = "environmentName", action = DESCRIBE_ENVIRONMENT)
    public GeneratedBlueprintV4Response postStackForBlueprint(@RequestObject @Valid DistroXV1Request stackRequest) {
        return stackOperations.postStackForBlueprint(stackRequestConverter.convert(stackRequest));
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_DATAHUB)
    public Object getRequestfromName(@ResourceName String name) {
        throw new UnsupportedOperationException("not supported request");
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public Object getRequestfromCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn) {
        throw new UnsupportedOperationException("not supported request");
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_DATAHUB)
    public StackStatusV4Response getStatusByName(@ResourceName String name) {
        return stackOperations.getStatus(
                NameOrCrn.ofName(name),
                getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public StackStatusV4Response getStatusByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn) {
        return stackOperations.getStatusByCrn(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_DATAHUB_INSTANCE)
    public FlowIdentifier deleteInstanceByName(@ResourceName String name, Boolean forced, String instanceId) {
        return stackOperations.deleteInstance(
                NameOrCrn.ofName(name),
                getWorkspaceIdForCurrentUser(),
                forced,
                instanceId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATAHUB_INSTANCE)
    public FlowIdentifier deleteInstanceByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn, Boolean forced,
            String instanceId) {
        return stackOperations.deleteInstance(
                NameOrCrn.ofCrn(crn),
                getWorkspaceIdForCurrentUser(),
                forced,
                instanceId);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_DATAHUB_INSTANCE)
    public FlowIdentifier deleteInstancesByName(@ResourceName String name, List<String> instances,
            MultipleInstanceDeleteRequest request, boolean forced) {
        return stackOperations.deleteInstances(
                NameOrCrn.ofName(name),
                getWorkspaceIdForCurrentUser(),
                getInstances(instances, request),
                forced);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATAHUB_INSTANCE)
    public FlowIdentifier deleteInstancesByCrn(@TenantAwareParam @ResourceCrn String crn, List<String> instances,
            MultipleInstanceDeleteRequest request, boolean forced) {
        return stackOperations.deleteInstances(
                NameOrCrn.ofCrn(crn),
                getWorkspaceIdForCurrentUser(),
                getInstances(instances, request),
                forced);
    }

    private Set<String> getInstances(List<String> instances, MultipleInstanceDeleteRequest request) {
        Set<String> ids = new HashSet<>();
        if (instances != null) {
            ids.addAll(instances);
        }
        if (request != null && request.getInstances() != null) {
            ids.addAll(request.getInstances());
        }
        return ids;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SET_DATAHUB_MAINTENANCE_MODE)
    public void setClusterMaintenanceModeByName(@ResourceName String name, @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperations.setClusterMaintenanceMode(
                NameOrCrn.ofName(name),
                getWorkspaceIdForCurrentUser(),
                maintenanceModeConverter.convert(maintenanceMode));

    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SET_DATAHUB_MAINTENANCE_MODE)
    public void setClusterMaintenanceModeByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn,
            @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperations.setClusterMaintenanceMode(
                NameOrCrn.ofCrn(crn),
                getWorkspaceIdForCurrentUser(),
                maintenanceModeConverter.convert(maintenanceMode));
    }

    @Override
    @CheckPermissionByResourceName(action = DELETE_DATAHUB)
    public void deleteWithKerberosByName(@ResourceName String name, boolean forced) {
        stackOperations.delete(
                NameOrCrn.ofName(name),
                getWorkspaceIdForCurrentUser(),
                forced);

    }

    @Override
    @CheckPermissionByResourceCrn(action = DELETE_DATAHUB)
    public void deleteWithKerberosByCrn(@TenantAwareParam @ResourceCrn String crn, boolean forced) {
        stackOperations.delete(
                NameOrCrn.ofCrn(crn),
                getWorkspaceIdForCurrentUser(),
                forced);

    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DATAHUB_READ)
    public Object getCreateAwsClusterForCli(DistroXV1Request request) {
        throw new UnsupportedOperationException("not supported request");
    }

    private StackV4Request getStackV4Request(NameOrCrn nameOrCrn) {
        return stackOperations.getRequest(nameOrCrn, getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByRequestProperty(path = "stackCrn", type = CRN, action = DESCRIBE_DATAHUB)
    public FlowIdentifier collectDiagnostics(@RequestObject @Valid DiagnosticsCollectionV1Request request) {
        String userCrn = crnService.getCloudbreakUser().getUserCrn();
        LOGGER.debug("collectDiagnostics called with userCrn '{}' for stack '{}'", userCrn, request.getStackCrn());
        return diagnosticsService.startDiagnosticsCollection(request, request.getStackCrn(), userCrn);
    }

    @Override
    @DisableCheckPermissions
    public VmLogsResponse getVmLogs() {
        return vmlogsConverter.convert(vmLogsService.getVmLogs());
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public ListDiagnosticsCollectionResponse listCollections(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn) {
        return diagnosticsService.getDiagnosticsCollections(crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public void cancelCollections(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn) {
        diagnosticsService.cancelDiagnosticsCollections(crn);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "stackCrn", type = CRN, action = DESCRIBE_DATAHUB)
    public FlowIdentifier collectCmDiagnostics(@RequestObject @Valid CmDiagnosticsCollectionV1Request request) {
        String userCrn = crnService.getCloudbreakUser().getUserCrn();
        LOGGER.debug("collectCmDiagnostics called with userCrn '{}' for stack '{}'", userCrn, request.getStackCrn());
        return diagnosticsService.startCmDiagnostics(request, request.getStackCrn(), userCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public List<String> getCmRoles(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String stackCrn) {
        return clusterDiagnosticsService.getClusterComponents(stackCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public FlowProgressResponse getLastFlowLogProgressByResourceCrn(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String resourceCrn) {
        return progressService.getLastFlowProgressByResourceCrn(resourceCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String resourceCrn) {
        return progressService.getFlowProgressListByResourceCrn(resourceCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public OperationView getOperationProgressByResourceCrn(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String resourceCrn, boolean detailed) {
        return operationService.getOperationProgressByResourceCrn(resourceCrn, detailed);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public FlowIdentifier renewCertificate(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn) {
        Stack stack = stackOperations.getStackByCrn(crn);
        return stackOperationService.renewCertificate(stack.getId());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.ROTATE_AUTOTLS_CERT_DATAHUB)
    public CertificatesRotationV4Response rotateAutoTlsCertificatesByName(@ResourceName String name,
            @Valid CertificatesRotationV4Request rotateCertificateRequest) {
        return stackOperations.rotateAutoTlsCertificates(
                NameOrCrn.ofName(name),
                getWorkspaceIdForCurrentUser(),
                rotateCertificateRequest
        );
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ROTATE_AUTOTLS_CERT_DATAHUB)
    public CertificatesRotationV4Response rotateAutoTlsCertificatesByCrn(
            @ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn,
            @Valid CertificatesRotationV4Request rotateCertificateRequest) {
        return stackOperations.rotateAutoTlsCertificates(
                NameOrCrn.ofCrn(crn),
                getWorkspaceIdForCurrentUser(),
                rotateCertificateRequest
        );
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public UpdateRecipesV4Response refreshRecipesByName(@ResourceName String name, @Valid UpdateRecipesV4Request request) {
        return stackOperations.refreshRecipes(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public UpdateRecipesV4Response refreshRecipesByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn,
            @Valid UpdateRecipesV4Request request) {
        return stackOperations.refreshRecipes(NameOrCrn.ofCrn(crn), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public AttachRecipeV4Response attachRecipeByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn,
            @Valid AttachRecipeV4Request request) {
        return stackOperations.attachRecipe(NameOrCrn.ofName(crn), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public AttachRecipeV4Response attachRecipeByName(@ResourceName String name, @Valid AttachRecipeV4Request request) {
        return stackOperations.attachRecipe(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public DetachRecipeV4Response detachRecipeByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn,
            @Valid DetachRecipeV4Request request) {
        return stackOperations.detachRecipe(NameOrCrn.ofName(crn), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public DetachRecipeV4Response detachRecipeByName(@ResourceName String name, @Valid DetachRecipeV4Request request) {
        return stackOperations.detachRecipe(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SYNC_COMPONENT_VERSIONS_FROM_CM_DATAHUB)
    public DistroXSyncCmV1Response syncComponentVersionsFromCmByName(@ResourceName String name) {
        return launchSyncComponentVersionsFromCm(NameOrCrn.ofName(name));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SYNC_COMPONENT_VERSIONS_FROM_CM_DATAHUB)
    public DistroXSyncCmV1Response syncComponentVersionsFromCmByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @ResourceCrn String crn) {
        return launchSyncComponentVersionsFromCm(NameOrCrn.ofCrn(crn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.CHANGE_IMAGE_CATALOG_DATAHUB)
    @CheckPermissionByRequestProperty(type = NAME, path = "imageCatalog", action = DESCRIBE_IMAGE_CATALOG)
    public void changeImageCatalog(@ResourceName String name, @RequestObject @Valid @NotNull ChangeImageCatalogV4Request changeImageCatalogRequest) {
        stackOperations.changeImageCatalog(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId(),
                changeImageCatalogRequest.getImageCatalog()
        );
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_DATAHUB)
    public DistroXGenerateImageCatalogV1Response generateImageCatalog(@ResourceName String name) {
        CloudbreakImageCatalogV3 imageCatalog = stackOperations.generateImageCatalog(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId());
        return new DistroXGenerateImageCatalogV1Response(imageCatalog);
    }

    private DistroXSyncCmV1Response launchSyncComponentVersionsFromCm(NameOrCrn nameOrCrn) {
        Long workspaceId = getWorkspaceIdForCurrentUser();
        FlowIdentifier flowIdentifier = stackOperations.syncComponentVersionsFromCm(nameOrCrn, workspaceId, Set.of());
        return new DistroXSyncCmV1Response(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceName(action = RECOVER_DATAHUB)
    public RecoveryValidationV4Response getClusterRecoverableByName(@ResourceName String name) {
        return stackOperations.validateClusterRecovery(NameOrCrn.ofName(name), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceCrn(action = RECOVER_DATAHUB)
    public RecoveryValidationV4Response getClusterRecoverableByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @ResourceCrn String crn) {
        return stackOperations.validateClusterRecovery(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

    private Long getWorkspaceIdForCurrentUser() {
        return workspaceService.getForCurrentUser().getId();
    }

    private FlowIdentifier restartClusterServicesByCrn(@ValidCrn(resource = CrnResourceDescriptor.DATAHUB) @TenantAwareParam @ResourceCrn String crn) {
        return stackOperations.restartClusterServices(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

}
