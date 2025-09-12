package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DELETE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_IMAGE_CATALOG;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.DESCRIBE_RECIPE;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ENVIRONMENT_CREATE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.MIGRATE_ZOOKEEPER_TO_KRAFT;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.RECOVER_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.UPGRADE_DATAHUB;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN_LIST;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.NAME_LIST;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrnList;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceNameList;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.FilterListBasedOnPermissions;
import com.sequenceiq.authorization.annotation.FilterParam;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrnList;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.annotation.ResourceNameList;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests.DatahubDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests.StackDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.response.StackDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackAddVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.DistroXSyncCmV1Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackEndpointV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.migraterds.MigrateDatabaseV1Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.service.cluster.ClusterDiagnosticsService;
import com.sequenceiq.cloudbreak.service.diagnostics.DiagnosticsService;
import com.sequenceiq.cloudbreak.service.operation.OperationService;
import com.sequenceiq.cloudbreak.service.rotaterdscert.StackRotateRdsCertificateService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceMetadataUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.telemetry.VmLogsService;
import com.sequenceiq.cloudbreak.telemetry.converter.VmLogsToVmLogsResponseConverter;
import com.sequenceiq.common.api.diagnostics.ListDiagnosticsCollectionResponse;
import com.sequenceiq.common.api.telemetry.response.VmLogsResponse;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXGenerateImageCatalogV1Response;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXInstanceMetadataUpdateV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMaintenanceModeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXVerticalScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.MultipleInstanceDeleteRequest;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model.CmDiagnosticsCollectionV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.diagnostics.model.DiagnosticsCollectionV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.rotaterdscert.RotateRdsCertificateV1Response;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.authorization.DataHubFiltering;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXMaintenanceModeV1ToMainenanceModeV4Converter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXRepairV1RequestToClusterRepairV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXScaleV1RequestToStackScaleV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXVerticalScaleV1RequestToStackVerticalScaleV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.RotateRdsCertificateConverter;
import com.sequenceiq.distrox.v1.distrox.service.DistroXCreateService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowProgressResponse;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.flow.api.model.RetryableFlowResponse.Builder;
import com.sequenceiq.flow.api.model.operation.OperationView;
import com.sequenceiq.flow.domain.RetryableFlow;
import com.sequenceiq.flow.service.FlowProgressService;

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
    private DistroXVerticalScaleV1RequestToStackVerticalScaleV4RequestConverter verticalScaleV4RequestConverter;

    @Inject
    private DistroXRepairV1RequestToClusterRepairV4RequestConverter clusterRepairRequestConverter;

    @Inject
    private DistroXMaintenanceModeV1ToMainenanceModeV4Converter maintenanceModeConverter;

    @Inject
    private CloudbreakRestRequestThreadLocalService crnService;

    @Inject
    private DiagnosticsService diagnosticsService;

    @Inject
    private FlowProgressService progressService;

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
    private DistroXCreateService distroxCreateService;

    @Inject
    private DataHubFiltering dataHubFiltering;

    @Inject
    private StackInstanceMetadataUpdateService instanceMetadataUpdateService;

    @Inject
    private RotateRdsCertificateConverter rotateRdsCertificateConverter;

    @Inject
    private StackRotateRdsCertificateService rotateRdsCertificateService;

    @Inject
    private StackService stackService;

    @Override
    @FilterListBasedOnPermissions
    public StackViewV4Responses list(@FilterParam(DataHubFiltering.ENV_NAME) String environmentName,
            @FilterParam(DataHubFiltering.ENV_CRN) String environmentCrn) {
        return dataHubFiltering.filterDataHubs(DESCRIBE_DATAHUB, environmentName, environmentCrn);
    }

    @Override
    @FilterListBasedOnPermissions
    public StackViewV4Responses listByServiceTypes(List<String> serviceTypes) {
        Set<StackViewV4Response> result = Set.of();
        if (!CollectionUtils.isEmpty(serviceTypes)) {
            Set<StackViewV4Response> stackViewV4Responses = new HashSet<>(
                    dataHubFiltering.filterResources(Crn.safeFromString(ThreadBasedUserCrnProvider.getUserCrn()), DESCRIBE_DATAHUB, Map.of()).getResponses());
            result = stackOperations.filterByServiceTypesPresent(stackViewV4Responses, serviceTypes);
        }
        return new StackViewV4Responses(result);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentName", type = NAME, action = ENVIRONMENT_CREATE_DATAHUB)
    @CheckPermissionByRequestProperty(path = "image.catalog", type = NAME, action = DESCRIBE_IMAGE_CATALOG, skipOnNull = true)
    @CheckPermissionByRequestProperty(path = "cluster.blueprintName", type = NAME, action = DESCRIBE_CLUSTER_TEMPLATE)
    @CheckPermissionByRequestProperty(path = "allRecipes", type = NAME_LIST, action = DESCRIBE_RECIPE)
    public StackV4Response post(@RequestObject DistroXV1Request request) {
        return distroxCreateService.create(request, false);
    }

    @Override
    @InternalOnly
    public StackV4Response postInternal(@InitiatorUserCrn String initiatorUserCrn, String accountId, DistroXV1Request request) {
        return distroxCreateService.create(request, true);
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_DATAHUB)
    public StackV4Response getByName(@ResourceName String name, Set<String> entries) {
        return stackOperations.get(
                NameOrCrn.ofName(name),
                ThreadBasedUserCrnProvider.getAccountId(),
                entries,
                StackType.WORKLOAD, false);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public StackV4Response getByCrn(@ResourceCrn String crn, Set<String> entries) {
        return stackOperations.get(
                NameOrCrn.ofCrn(crn),
                ThreadBasedUserCrnProvider.getAccountId(),
                entries,
                StackType.WORKLOAD, false);
    }

    @Override
    @CheckPermissionByResourceName(action = DELETE_DATAHUB)
    public void deleteByName(@ResourceName String name, Boolean forced) {
        stackOperations.delete(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), forced);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DELETE_DATAHUB)
    public void deleteByCrn(@ResourceCrn String crn, Boolean forced) {
        stackOperations.delete(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), forced);
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
        nameOrCrns.forEach(nameOrCrn -> stackOperations.delete(nameOrCrn, ThreadBasedUserCrnProvider.getAccountId(), forced));
    }

    private void multideleteByCrn(DistroXMultiDeleteV1Request multiDeleteRequest, Boolean forced) {
        Set<NameOrCrn> nameOrCrns = multiDeleteRequest.getCrns().stream()
                .map(NameOrCrn::ofCrn)
                .collect(Collectors.toSet());
        nameOrCrns.forEach(accessDto -> stackOperations.delete(accessDto, ThreadBasedUserCrnProvider.getAccountId(), forced));
    }

    private FlowIdentifier verticalScaling(NameOrCrn nameOrCrn, DistroXVerticalScaleV1Request updateRequest) {
        Stack stack = stackService.getByNameOrCrnAndWorkspaceIdWithLists(nameOrCrn, getWorkspaceIdForCurrentUser());
        Optional<InstanceGroup> instanceGroupOptional = stack.getInstanceGroups().stream()
                .filter(instanceGroup -> updateRequest.getGroup().equals(instanceGroup.getGroupName())).findFirst();
        boolean gateway = false;
        if (instanceGroupOptional.isPresent()) {
            gateway = InstanceGroupType.isGateway(instanceGroupOptional.get().getInstanceGroupType());
        }
        StackVerticalScaleV4Request stackVerticalScaleV4Request = verticalScaleV4RequestConverter.convert(updateRequest, gateway);
        stackVerticalScaleV4Request.setStackId(stack.getId());
        return stackOperations.putVerticalScaling(stack, stackVerticalScaleV4Request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SYNC_DATAHUB)
    public FlowIdentifier syncByName(@ResourceName String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return stackOperations.sync(NameOrCrn.ofName(name), accountId, EnumSet.of(StackType.WORKLOAD));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SYNC_DATAHUB)
    public FlowIdentifier syncByCrn(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return stackOperations.sync(NameOrCrn.ofCrn(crn), accountId, EnumSet.of(StackType.WORKLOAD));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.RETRY_DATAHUB_OPERATION)
    public FlowIdentifier retryByName(@ResourceName String name) {
        return stackOperations.retry(
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
    public FlowIdentifier retryByCrn(@ResourceCrn String crn) {
        return stackOperations.retry(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.STOP_DATAHUB)
    public FlowIdentifier putStopByName(@ResourceName String name) {
        return stackOperations.putStop(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.STOP_DATAHUB)
    public FlowIdentifier putStopByCrn(@ResourceCrn String crn) {
        return stackOperations.putStop(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
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
        return stackOperations.putStart(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.START_DATAHUB)
    public FlowIdentifier putStartByCrn(@ResourceCrn String crn) {
        return stackOperations.putStart(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrnList(action = AuthorizationResourceAction.START_DATAHUB)
    public List<FlowIdentifier> restartClusterServicesByCrns(@ResourceCrnList List<String> crns, Boolean refreshRemoteDataContext) {
        List<FlowIdentifier> flowIds = new ArrayList<>();
        for (String crn : crns) {
            flowIds.add(restartClusterServicesByCrn(crn, refreshRemoteDataContext));
        }
        return flowIds;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ROTATE_SALTUSER_PASSWORD_DATAHUB)
    public FlowIdentifier rotateSaltPasswordByCrn(@ResourceCrn String crn) {
        return stackOperations.rotateSaltPassword(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), RotateSaltPasswordReason.MANUAL);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.UPDATE_SALT_DATAHUB)
    public FlowIdentifier updateSaltByCrn(@ResourceCrn String crn) {
        return stackOperations.updateSalt(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
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
    public FlowIdentifier putScalingByName(@ResourceName String name, DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperations.getResourceIdByResourceName(name));
        return stackOperations.putScaling(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), stackScaleV4Request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SCALE_DATAHUB)
    public void putScalingByCrn(@ResourceCrn String crn, DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperations.getStackByCrn(crn).getId());
        stackOperations.putScaling(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), stackScaleV4Request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING)
    public FlowIdentifier verticalScalingByName(@ResourceName String name, DistroXVerticalScaleV1Request updateRequest) {
        return verticalScaling(NameOrCrn.ofName(name), updateRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING)
    public FlowIdentifier verticalScalingByCrn(@ResourceCrn String crn, DistroXVerticalScaleV1Request updateRequest) {
        return verticalScaling(NameOrCrn.ofCrn(crn), updateRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public FlowIdentifier repairClusterByName(@ResourceName String name, DistroXRepairV1Request clusterRepairRequest) {
        return stackOperations.repairCluster(
                NameOrCrn.ofName(name),
                getWorkspaceIdForCurrentUser(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public FlowIdentifier repairClusterByCrn(@ResourceCrn String crn, DistroXRepairV1Request clusterRepairRequest) {
        return stackOperations.repairCluster(
                NameOrCrn.ofCrn(crn),
                getWorkspaceIdForCurrentUser(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public RotateRdsCertificateV1Response rotateRdsCertificateByName(@ResourceName String name) {
        return rotateRdsCertificateConverter.convert(
                rotateRdsCertificateService.rotateRdsCertificate(NameOrCrn.ofName(name),
                        ThreadBasedUserCrnProvider.getAccountId()));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public RotateRdsCertificateV1Response rotateRdsCertificateByCrn(@ResourceCrn String crn) {
        return rotateRdsCertificateConverter.convert(
                rotateRdsCertificateService.rotateRdsCertificate(NameOrCrn.ofCrn(crn),
                        ThreadBasedUserCrnProvider.getAccountId()));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public MigrateDatabaseV1Response migrateDatabaseToSslByName(@ResourceName String name) {
        return null;
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public MigrateDatabaseV1Response migrateDatabaseToSslByCrn(@ResourceCrn String crn) {
        return null;
    }

    @Override
    @CheckPermissionByRequestProperty(type = NAME, path = "environmentName", action = DESCRIBE_ENVIRONMENT)
    public GeneratedBlueprintV4Response postStackForBlueprint(@RequestObject DistroXV1Request stackRequest) {
        return stackOperations.postStackForBlueprint(stackRequestConverter.convert(stackRequest));
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_DATAHUB)
    public Object getRequestfromName(@ResourceName String name) {
        throw new UnsupportedOperationException("not supported request");
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public Object getRequestfromCrn(@ResourceCrn String crn) {
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
    public StackStatusV4Response getStatusByCrn(@ResourceCrn String crn) {
        return stackOperations.getStatusByCrn(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceCrnList(action = DESCRIBE_DATAHUB)
    public StackEndpointV4Responses getEndpointsByCrns(@ResourceCrnList List<String> crns) {
        return stackOperations.getEndpointsCrns(crns, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_DATAHUB_INSTANCE)
    public FlowIdentifier deleteInstanceByName(@ResourceName String name, Boolean forced, String instanceId) {
        return stackOperations.deleteInstance(
                NameOrCrn.ofName(name),
                ThreadBasedUserCrnProvider.getAccountId(),
                forced,
                instanceId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATAHUB_INSTANCE)
    public FlowIdentifier deleteInstanceByCrn(@ResourceCrn String crn, Boolean forced,
            String instanceId) {
        return stackOperations.deleteInstance(
                NameOrCrn.ofCrn(crn),
                ThreadBasedUserCrnProvider.getAccountId(),
                forced,
                instanceId);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_DATAHUB_INSTANCE)
    public FlowIdentifier deleteInstancesByName(@ResourceName String name, List<String> instances,
            MultipleInstanceDeleteRequest request, boolean forced) {
        return stackOperations.deleteInstances(
                NameOrCrn.ofName(name),
                ThreadBasedUserCrnProvider.getAccountId(),
                getInstances(instances, request),
                forced);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_DATAHUB_INSTANCE)
    public FlowIdentifier deleteInstancesByCrn(@ResourceCrn String crn, List<String> instances,
            MultipleInstanceDeleteRequest request, boolean forced) {
        return stackOperations.deleteInstances(
                NameOrCrn.ofCrn(crn),
                ThreadBasedUserCrnProvider.getAccountId(),
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
    public void setClusterMaintenanceModeByName(@ResourceName String name, DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperations.setClusterMaintenanceMode(
                NameOrCrn.ofName(name),
                ThreadBasedUserCrnProvider.getAccountId(),
                maintenanceModeConverter.convert(maintenanceMode));

    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SET_DATAHUB_MAINTENANCE_MODE)
    public void setClusterMaintenanceModeByCrn(@ResourceCrn String crn, DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperations.setClusterMaintenanceMode(
                NameOrCrn.ofCrn(crn),
                ThreadBasedUserCrnProvider.getAccountId(),
                maintenanceModeConverter.convert(maintenanceMode));
    }

    @Override
    @CheckPermissionByResourceName(action = DELETE_DATAHUB)
    public void deleteWithKerberosByName(@ResourceName String name, boolean forced) {
        stackOperations.delete(
                NameOrCrn.ofName(name),
                ThreadBasedUserCrnProvider.getAccountId(),
                forced);

    }

    @Override
    @CheckPermissionByResourceCrn(action = DELETE_DATAHUB)
    public void deleteWithKerberosByCrn(@ResourceCrn String crn, boolean forced) {
        stackOperations.delete(
                NameOrCrn.ofCrn(crn),
                ThreadBasedUserCrnProvider.getAccountId(),
                forced);

    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentName", type = NAME, action = DESCRIBE_ENVIRONMENT)
    public Object getCreateAwsClusterForCli(@RequestObject DistroXV1Request request) {
        throw new UnsupportedOperationException("not supported request");
    }

    @Override
    @CheckPermissionByRequestProperty(path = "stackCrn", type = CRN, action = DESCRIBE_DATAHUB)
    public FlowIdentifier collectDiagnostics(@RequestObject DiagnosticsCollectionV1Request request) {
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
    public ListDiagnosticsCollectionResponse listCollections(@ResourceCrn String crn) {
        return diagnosticsService.getDiagnosticsCollections(crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public void cancelCollections(@ResourceCrn String crn) {
        diagnosticsService.cancelDiagnosticsCollections(crn);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "stackCrn", type = CRN, action = DESCRIBE_DATAHUB)
    public FlowIdentifier collectCmDiagnostics(@RequestObject CmDiagnosticsCollectionV1Request request) {
        String userCrn = crnService.getCloudbreakUser().getUserCrn();
        LOGGER.debug("collectCmDiagnostics called with userCrn '{}' for stack '{}'", userCrn, request.getStackCrn());
        return diagnosticsService.startCmDiagnostics(request, request.getStackCrn(), userCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public List<String> getCmRoles(@ResourceCrn String stackCrn) {
        return clusterDiagnosticsService.getClusterComponents(stackCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public FlowProgressResponse getLastFlowLogProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return progressService.getLastFlowProgressByResourceCrn(resourceCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public List<FlowProgressResponse> getFlowLogsProgressByResourceCrn(@ResourceCrn String resourceCrn) {
        return progressService.getFlowProgressListByResourceCrn(resourceCrn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public OperationView getOperationProgressByResourceCrn(@ResourceCrn String resourceCrn, boolean detailed) {
        return operationService.getOperationProgressByResourceCrn(resourceCrn, detailed);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public FlowIdentifier renewCertificate(@ResourceCrn String crn) {
        Stack stack = stackOperations.getStackByCrn(crn);
        return stackOperationService.renewCertificate(stack.getId());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.ROTATE_AUTOTLS_CERT_DATAHUB)
    public CertificatesRotationV4Response rotateAutoTlsCertificatesByName(@ResourceName String name, CertificatesRotationV4Request rotateCertificateRequest) {
        return stackOperations.rotateAutoTlsCertificates(
                NameOrCrn.ofName(name),
                ThreadBasedUserCrnProvider.getAccountId(),
                rotateCertificateRequest
        );
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.ROTATE_AUTOTLS_CERT_DATAHUB)
    public CertificatesRotationV4Response rotateAutoTlsCertificatesByCrn(@ResourceCrn String crn, CertificatesRotationV4Request rotateCertificateRequest) {
        return stackOperations.rotateAutoTlsCertificates(
                NameOrCrn.ofCrn(crn),
                ThreadBasedUserCrnProvider.getAccountId(),
                rotateCertificateRequest
        );
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public UpdateRecipesV4Response refreshRecipesByName(@ResourceName String name, UpdateRecipesV4Request request) {
        return stackOperations.refreshRecipes(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public UpdateRecipesV4Response refreshRecipesByCrn(@ResourceCrn String crn, UpdateRecipesV4Request request) {
        return stackOperations.refreshRecipes(NameOrCrn.ofCrn(crn), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public AttachRecipeV4Response attachRecipeByCrn(@ResourceCrn String crn, AttachRecipeV4Request request) {
        return stackOperations.attachRecipe(NameOrCrn.ofName(crn), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public AttachRecipeV4Response attachRecipeByName(@ResourceName String name, AttachRecipeV4Request request) {
        return stackOperations.attachRecipe(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public DetachRecipeV4Response detachRecipeByCrn(@ResourceCrn String crn, DetachRecipeV4Request request) {
        return stackOperations.detachRecipe(NameOrCrn.ofName(crn), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REFRESH_RECIPES_DATAHUB)
    public DetachRecipeV4Response detachRecipeByName(@ResourceName String name, DetachRecipeV4Request request) {
        return stackOperations.detachRecipe(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId(), request);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.SYNC_COMPONENT_VERSIONS_FROM_CM_DATAHUB)
    public DistroXSyncCmV1Response syncComponentVersionsFromCmByName(@ResourceName String name) {
        return launchSyncComponentVersionsFromCm(NameOrCrn.ofName(name));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.SYNC_COMPONENT_VERSIONS_FROM_CM_DATAHUB)
    public DistroXSyncCmV1Response syncComponentVersionsFromCmByCrn(@ResourceCrn String crn) {
        return launchSyncComponentVersionsFromCm(NameOrCrn.ofCrn(crn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.CHANGE_IMAGE_CATALOG_DATAHUB)
    @CheckPermissionByRequestProperty(type = NAME, path = "imageCatalog", action = DESCRIBE_IMAGE_CATALOG)
    public void changeImageCatalog(@ResourceName String name, @RequestObject ChangeImageCatalogV4Request changeImageCatalogRequest) {
        stackOperations.changeImageCatalog(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId(),
                changeImageCatalogRequest.getImageCatalog()
        );
    }

    @Override
    @CheckPermissionByResourceName(action = DESCRIBE_DATAHUB)
    public DistroXGenerateImageCatalogV1Response generateImageCatalog(@ResourceName String name) {
        CloudbreakImageCatalogV3 imageCatalog = stackOperations.generateImageCatalog(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId());
        return new DistroXGenerateImageCatalogV1Response(imageCatalog);
    }

    private DistroXSyncCmV1Response launchSyncComponentVersionsFromCm(NameOrCrn nameOrCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = stackOperations.syncComponentVersionsFromCm(nameOrCrn, accountId, Set.of());
        return new DistroXSyncCmV1Response(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceName(action = RECOVER_DATAHUB)
    public RecoveryValidationV4Response getClusterRecoverableByName(@ResourceName String name) {
        return stackOperations.validateClusterRecovery(NameOrCrn.ofName(name), getWorkspaceIdForCurrentUser());
    }

    @Override
    @CheckPermissionByResourceCrn(action = RECOVER_DATAHUB)
    public RecoveryValidationV4Response getClusterRecoverableByCrn(@ResourceCrn String crn) {
        return stackOperations.validateClusterRecovery(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser());
    }

    private Long getWorkspaceIdForCurrentUser() {
        return workspaceService.getForCurrentUser().getId();
    }

    private FlowIdentifier restartClusterServicesByCrn(String crn, Boolean refreshRemoteDataContext) {
        return stackOperations.restartClusterServices(NameOrCrn.ofCrn(crn), getWorkspaceIdForCurrentUser(), refreshRemoteDataContext);
    }

    @Override
    @InternalOnly
    public FlowIdentifier modifyProxyInternal(@ResourceCrn String crn, String previousProxyConfigCrn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperationService.modifyProxyConfig(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), previousProxyConfigCrn);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING)
    public FlowIdentifier deleteVolumesByStackName(@ResourceName String name, StackDeleteVolumesRequest deleteRequest) {
        deleteRequest.setStackId(stackOperations.getResourceIdByResourceName(name));
        return stackOperations.putDeleteVolumes(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), deleteRequest);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING)
    public FlowIdentifier deleteVolumesByStackCrn(@ResourceCrn String crn, StackDeleteVolumesRequest deleteRequest) {
        deleteRequest.setStackId(stackOperations.getResourceIdByResourceCrn(crn));
        return stackOperations.putDeleteVolumes(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), deleteRequest);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING)
    public FlowIdentifier diskUpdateByName(@ResourceName String name, DiskUpdateRequest updateRequest) {
        return stackOperationService.stackUpdateDisks(NameOrCrn.ofName(name), updateRequest, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING)
    public FlowIdentifier diskUpdateByCrn(@ResourceCrn String crn, DiskUpdateRequest updateRequest) {
        return stackOperationService.stackUpdateDisks(NameOrCrn.ofCrn(crn), updateRequest, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING)
    public FlowIdentifier addVolumesByStackName(@ResourceName String name, StackAddVolumesRequest addVolumesRequest) {
        return stackOperations.putAddVolumes(NameOrCrn.ofName(name), addVolumesRequest, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING)
    public FlowIdentifier addVolumesByStackCrn(@ResourceCrn String crn, StackAddVolumesRequest addVolumesRequest) {
        return stackOperations.putAddVolumes(NameOrCrn.ofCrn(crn), addVolumesRequest, ThreadBasedUserCrnProvider.getAccountId());
    }

    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING)
    public FlowIdentifier updateRootVolumeByDatahubName(@ResourceName String name, DiskUpdateRequest rootDiskVolumesRequest) throws Exception {
        return stackOperationService.rootVolumeDiskUpdate(NameOrCrn.ofName(name), rootDiskVolumesRequest, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATAHUB_VERTICAL_SCALING)
    public FlowIdentifier updateRootVolumeByDatahubCrn(@ResourceCrn String crn, DiskUpdateRequest rootDiskVolumesRequest) throws Exception {
        return stackOperationService.rootVolumeDiskUpdate(NameOrCrn.ofCrn(crn), rootDiskVolumesRequest, ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByRequestProperty(action = UPGRADE_DATAHUB, type = CRN, path = "crn")
    public FlowIdentifier instanceMetadataUpdate(@RequestObject DistroXInstanceMetadataUpdateV1Request request) {
        return instanceMetadataUpdateService.updateInstanceMetadata(request.getCrn(), request.getUpdateType());
    }

    @Override
    @CheckPermissionByRequestProperty(path = "crns", type = CRN_LIST, action = DESCRIBE_DATAHUB)
    public StackDatabaseServerCertificateStatusV4Responses listDatabaseServersCertificateStatus(
            @RequestObject DatahubDatabaseServerCertificateStatusV4Request request) {
        String userCrn = ThreadBasedUserCrnProvider.getUserCrn();

        StackDatabaseServerCertificateStatusV4Request stackDatabaseServerCertificateStatusV4Request = new StackDatabaseServerCertificateStatusV4Request();
        stackDatabaseServerCertificateStatusV4Request.setCrns(request.getCrns());
        return stackOperationService.listDatabaseServersCertificateStatus(stackDatabaseServerCertificateStatusV4Request, userCrn);
    }

    @Override
    @CheckPermissionByResourceName(action = UPGRADE_DATAHUB)
    public FlowIdentifier setDefaultJavaVersionByName(@ResourceName String name, @RequestObject SetDefaultJavaVersionRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        NameOrCrn nameOrCrn = NameOrCrn.ofName(name);
        return stackOperationService.triggerSetDefaultJavaVersion(nameOrCrn, accountId, request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = UPGRADE_DATAHUB)
    public FlowIdentifier setDefaultJavaVersionByCrn(@ResourceCrn String crn, @RequestObject SetDefaultJavaVersionRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(crn);
        return stackOperationService.triggerSetDefaultJavaVersion(nameOrCrn, accountId, request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = DESCRIBE_DATAHUB)
    public List<String> listAvailableJavaVersionsByCrn(@ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(crn);
        return stackOperationService.listAvailableJavaVersions(nameOrCrn, accountId);
    }

    @Override
    @CheckPermissionByResourceName(action =  AuthorizationResourceAction.REPAIR_DATAHUB)
    public FlowIdentifier modifySeLinuxByName(@ResourceName String name, SeLinux selinuxMode) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        NameOrCrn nameOrCrn = NameOrCrn.ofName(name);
        return stackOperationService.triggerModifySELinux(nameOrCrn, accountId, selinuxMode);
    }

    @Override
    @CheckPermissionByResourceCrn(action =  AuthorizationResourceAction.REPAIR_DATAHUB)
    public FlowIdentifier modifySeLinuxByCrn(@ResourceCrn String crn, SeLinux selinuxMode) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(crn);
        return stackOperationService.triggerModifySELinux(nameOrCrn, accountId, selinuxMode);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public FlowIdentifier triggerSkuMigrationByName(@ResourceName String name, boolean force) {
        return stackOperationService.triggerSkuMigration(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), force);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.REPAIR_DATAHUB)
    public FlowIdentifier triggerSkuMigrationByCrn(@ResourceCrn String crn, boolean force) {
        return stackOperationService.triggerSkuMigration(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), force);
    }

    @Override
    @CheckPermissionByResourceCrn(action = MIGRATE_ZOOKEEPER_TO_KRAFT)
    public FlowIdentifier migrateFromZookeeperToKraft(@ResourceCrn String crn) {
        return FlowIdentifier.notTriggered();
    }
}
