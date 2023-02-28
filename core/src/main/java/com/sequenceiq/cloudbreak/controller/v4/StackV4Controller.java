package com.sequenceiq.cloudbreak.controller.v4;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.RotateSaltPasswordRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerSyncV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.osupgrade.OrderedOSUpgradeSetRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.AttachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.DetachRecipeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.recipe.UpdateRecipesV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.CertificatesRotationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RangerRazEnabledV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatusResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.BackupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.RestoreV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.imagecatalog.GenerateImageCatalogV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.crn.CrnResourceDescriptor;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.SubnetIdWithResourceNameAndCrn;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.StackCcmUpgradeService;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.validation.ValidCrn;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.common.api.UsedSubnetWithResourceResponse;
import com.sequenceiq.common.api.UsedSubnetsByEnvironmentResponse;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.StackUpgradeOperations;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.RetryableFlowResponse;
import com.sequenceiq.flow.api.model.RetryableFlowResponse.Builder;

@Controller
@WorkspaceEntityType(Stack.class)
public class StackV4Controller extends NotificationController implements StackV4Endpoint {

    @Inject
    private StackOperations stackOperations;

    @Inject
    private StackUpgradeOperations stackUpgradeOperations;

    @Inject
    private StackOperationService stackOperationService;

    @Inject
    private StackCcmUpgradeService stackCcmUpgradeService;

    @Inject
    private RdsUpgradeService rdsUpgradeService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackViewV4Responses list(Long workspaceId, @TenantAwareParam String environmentCrn, boolean onlyDatalakes) {
        List<StackType> types = new ArrayList<>();
        if (onlyDatalakes) {
            types.add(StackType.DATALAKE);
        } else {
            types.add(StackType.DATALAKE);
            types.add(StackType.WORKLOAD);
            types.add(StackType.LEGACY);
        }
        return stackOperations.listByEnvironmentCrn(restRequestThreadLocalService.getRequestedWorkspaceId(), environmentCrn, types);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackV4Response post(Long workspaceId, @Valid StackV4Request request, @AccountId String accountId) {
        return stackOperations.post(restRequestThreadLocalService.getRequestedWorkspaceId(), restRequestThreadLocalService.getCloudbreakUser(), request, false);
    }

    @Override
    @InternalOnly
    public StackV4Response postInternal(Long workspaceId, @Valid StackV4Request request, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.post(restRequestThreadLocalService.getRequestedWorkspaceId(), restRequestThreadLocalService.getCloudbreakUser(), request, false);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackV4Response get(Long workspaceId, String name, Set<String> entries, @AccountId String accountId) {
        return stackOperations.get(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), entries, null);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackV4Response getByCrn(Long workspaceId, @TenantAwareParam String crn, Set<String> entries) {
        return stackOperations.get(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getAccountId(), entries, null);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public void delete(Long workspaceId, String name, boolean forced, @AccountId String accountId) {
        stackOperations.delete(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), forced);
    }

    @Override
    @InternalOnly
    public void deleteInternal(Long workspaceId, String name, boolean forced, @InitiatorUserCrn String initiatorUserCrn) {
        stackOperations.delete(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), forced);
    }

    @Override
    @InternalOnly
    public void updateNameAndCrn(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn, String newName, String newCrn,
            boolean retainOriginalName) {
        stackOperations.updateNameAndCrn(
                NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), newName, newCrn,
                retainOriginalName
        );
    }

    @Override
    @InternalOnly
    public void updateLoadBalancerDNS(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        stackOperations.updateLoadBalancerDNS(restRequestThreadLocalService.getRequestedWorkspaceId(), NameOrCrn.ofName(name));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier sync(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.sync(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId());
    }

    @Override
    @InternalOnly
    public FlowIdentifier syncCm(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn, ClouderaManagerSyncV4Request syncRequest) {
        return stackOperations.syncComponentVersionsFromCm(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(),
                syncRequest.getCandidateImageUuids());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier retry(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.retry(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public List<RetryableFlowResponse> listRetryableFlows(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.getRetryableFlows(name, restRequestThreadLocalService.getRequestedWorkspaceId())
                .stream().map(retryable -> Builder.builder().setName(retryable.getName()).setFailDate(retryable.getFailDate()).build())
                .collect(Collectors.toList());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putStop(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.putStop(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId());
    }

    @Override
    @InternalOnly
    public FlowIdentifier putStopInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.putStop(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putStart(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.putStart(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId());
    }

    @Override
    @InternalOnly
    public FlowIdentifier putStartInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.putStart(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId());
    }

    @Override
    @InternalOnly
    public FlowIdentifier rotateSaltPasswordInternal(Long workspaceId, String crn, RotateSaltPasswordRequest request,
            @InitiatorUserCrn String initiatorUserCrn) {
        RotateSaltPasswordReason rotateSaltPasswordReason = RotateSaltPasswordReason.valueOf(request.getReason().name());
        return stackOperations.rotateSaltPassword(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getAccountId(), rotateSaltPasswordReason);
    }

    @Override
    @InternalOnly
    public SaltPasswordStatusResponse getSaltPasswordStatus(Long workspaceId, @TenantAwareParam String crn) {
        SaltPasswordStatusResponse response = new SaltPasswordStatusResponse();
        SaltPasswordStatus saltPasswordStatus = stackOperations.getSaltPasswordStatus(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getAccountId());
        response.setStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus.valueOf(saltPasswordStatus.name()));
        return response;
    }

    @Override
    @InternalOnly
    public FlowIdentifier modifyProxyConfigInternal(Long workspaceId, String crn, String previousProxyConfigCrn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.modifyProxyConfig(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getAccountId(), previousProxyConfigCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putScaling(Long workspaceId, String name, @Valid StackScaleV4Request updateRequest, @AccountId String accountId) {
        return stackOperations.putScaling(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), updateRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier repairCluster(Long workspaceId, String name, @Valid ClusterRepairV4Request clusterRepairRequest,
            @AccountId String accountId) {
        return stackOperations.repairCluster(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), clusterRepairRequest);
    }

    @Override
    @InternalOnly
    public FlowIdentifier repairClusterInternal(Long workspaceId, String name, @Valid ClusterRepairV4Request clusterRepairRequest,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.repairCluster(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), clusterRepairRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier upgradeOs(Long workspaceId, String name, @AccountId String accountId, Boolean keepVariant) {
        return stackUpgradeOperations.upgradeOs(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), Boolean.TRUE.equals(keepVariant));
    }

    @InternalOnly
    public FlowIdentifier upgradeOsByUpgradeSetsInternal(Long workspaceId, @TenantAwareParam @ResourceCrn String crn,
            OrderedOSUpgradeSetRequest orderedOsUpgradeSetRequest) {
        return stackUpgradeOperations.upgradeOsByUpgradeSets(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId(),
                orderedOsUpgradeSetRequest);
    }

    @Override
    @InternalOnly
    public FlowIdentifier upgradeOsInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn, Boolean keepVariant) {
        return stackUpgradeOperations.upgradeOs(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), Boolean.TRUE.equals(keepVariant));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public UpgradeOptionV4Response checkForOsUpgrade(Long workspaceId, String name, @AccountId String accountId) {
        return stackUpgradeOperations.checkForOsUpgrade(NameOrCrn.ofName(name), restRequestThreadLocalService.getCloudbreakUser(),
                restRequestThreadLocalService.getAccountId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public GeneratedBlueprintV4Response postStackForBlueprint(Long workspaceId, String name, @Valid StackV4Request stackRequest,
            @AccountId String accountId) {
        return stackOperations.postStackForBlueprint(stackRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier changeImage(Long workspaceId, String name, @Valid StackImageChangeV4Request stackImageChangeRequest,
            @AccountId String accountId) {
        return stackOperations.changeImage(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), stackImageChangeRequest);
    }

    @Override
    @InternalOnly
    public FlowIdentifier changeImageInternal(Long workspaceId, String name, @Valid StackImageChangeV4Request stackImageChangeRequest,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.changeImage(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), stackImageChangeRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public void deleteWithKerberos(Long workspaceId, String name, boolean forced, @AccountId String accountId) {
        stackOperations.delete(NameOrCrn.ofName(name), accountId, forced);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackV4Request getRequestfromName(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.getRequest(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackStatusV4Response getStatusByName(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.getStatus(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier deleteInstance(Long workspaceId, String name, boolean forced, String instanceId,
            @AccountId String accountId) {
        return stackOperations.deleteInstance(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), forced, instanceId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier deleteMultipleInstances(Long workspaceId, String name, @NotEmpty List<String> instanceIds, boolean forced,
            @AccountId String accountId) {
        return stackOperations.deleteInstances(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(),
                new HashSet<>(instanceIds), forced);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putPassword(Long workspaceId, String name, @Valid UserNamePasswordV4Request userNamePasswordJson,
            @AccountId String accountId) {
        return stackOperations.putPassword(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), userNamePasswordJson);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier setClusterMaintenanceMode(Long workspaceId, String name, @NotNull MaintenanceModeV4Request maintenanceMode,
            @AccountId String accountId) {
        return stackOperations.setClusterMaintenanceMode(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), maintenanceMode);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putCluster(Long workspaceId, String name, @Valid UpdateClusterV4Request updateJson, @AccountId String accountId) {
        return stackOperations.putCluster(name, accountId, updateJson);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public String getClusterHostsInventory(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.getClusterHostsInventory(restRequestThreadLocalService.getRequestedWorkspaceId(), name);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public UpgradeV4Response checkForClusterUpgradeByName(Long workspaceId, String name, @NotNull UpgradeV4Request request, @AccountId String accountId) {
        return stackUpgradeOperations.checkForClusterUpgrade(accountId, NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(),
                request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier upgradeClusterByName(Long workspaceId, String name, String imageId, @AccountId String accountId) {
        return stackUpgradeOperations.upgradeCluster(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), imageId, Boolean.FALSE);
    }

    @Override
    @InternalOnly
    public FlowIdentifier upgradeClusterByNameInternal(Long workspaceId, String name, String imageId, @InitiatorUserCrn String initiatorUserCrn,
            Boolean rollingUpgradeEnabled) {
        return stackUpgradeOperations.upgradeCluster(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), imageId, rollingUpgradeEnabled);
    }

    @Override
    @InternalOnly
    public FlowIdentifier prepareClusterUpgradeByCrnInternal(Long workspaceId, String crn, String imageId, @InitiatorUserCrn String initiatorUserCrn) {
        return stackUpgradeOperations.prepareClusterUpgrade(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getAccountId(), imageId);
    }

    @Override
    @InternalOnly
    public void checkUpgradeRdsByClusterNameInternal(Long workspaceId, @NotEmpty @ResourceName String clusterName,
            TargetMajorVersion targetMajorVersion,
            @InitiatorUserCrn @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) String initiatorUserCrn) {
        rdsUpgradeService.checkUpgradeRds(NameOrCrn.ofName(clusterName), targetMajorVersion);
    }

    @Override
    @InternalOnly
    public RdsUpgradeV4Response upgradeRdsByClusterNameInternal(Long workspaceId, @NotEmpty @ResourceName String clusterName,
            TargetMajorVersion targetMajorVersion,
            @InitiatorUserCrn @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) String initiatorUserCrn) {
        return rdsUpgradeService.upgradeRds(NameOrCrn.ofName(clusterName), targetMajorVersion);
    }

    @Override
    @InternalOnly
    public StackCcmUpgradeV4Response upgradeCcmByNameInternal(Long workspaceId, @NotEmpty @ResourceName String name,
            @InitiatorUserCrn @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) String initiatorUserCrn) {
        return stackCcmUpgradeService.upgradeCcm(NameOrCrn.ofName(name));
    }

    @Override
    @InternalOnly
    public StackCcmUpgradeV4Response upgradeCcmByCrnInternal(Long workspaceId,
            @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.DATAHUB, CrnResourceDescriptor.DATALAKE}) String crn,
            @InitiatorUserCrn @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) String initiatorUserCrn) {
        return stackCcmUpgradeService.upgradeCcm(NameOrCrn.ofCrn(crn));
    }

    @Override
    @InternalOnly
    public int getNotCcmUpgradedStackCount(Long workspaceId,
            @NotEmpty @ValidCrn(resource = CrnResourceDescriptor.ENVIRONMENT) String envCrn,
            @InitiatorUserCrn @NotEmpty @ValidCrn(resource = {CrnResourceDescriptor.USER, CrnResourceDescriptor.MACHINE_USER}) String initiatorUserCrn) {
        return stackCcmUpgradeService.getNotUpgradedStackCount(envCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier updateSaltByName(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.updateSalt(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId());
    }

    /**
     * @deprecated Use updatePillarConfigurationByCrn instead
     */
    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    @Deprecated
    public FlowIdentifier updatePillarConfigurationByName(Long workspaceId, String name) {
        throw new BadRequestException("Updating pillar config information by name is deprecated.  Please use update pillar config by CRN.");
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier updatePillarConfigurationByCrn(Long workspaceId, @TenantAwareParam String crn) {
        String accountId = restRequestThreadLocalService.getAccountId();
        return stackOperations.updatePillarConfiguration(NameOrCrn.ofCrn(crn), accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public BackupV4Response backupDatabaseByName(Long workspaceId, String name, String backupLocation, String backupId, List<String> skipDatabaseNames,
            @AccountId String accountId) {
        FlowIdentifier flowIdentifier = stackOperations.backupClusterDatabase(NameOrCrn.ofName(name),
                restRequestThreadLocalService.getRequestedWorkspaceId(), backupLocation, backupId, true, skipDatabaseNames);
        return new BackupV4Response(flowIdentifier);
    }

    @Override
    @InternalOnly
    public BackupV4Response backupDatabaseByNameInternal(Long workspaceId, String name, String backupId, String backupLocation,
            boolean closeConnections, List<String> skipDatabaseNames, @InitiatorUserCrn String initiatorUserCrn) {
        FlowIdentifier flowIdentifier = stackOperations.backupClusterDatabase(NameOrCrn.ofName(name),
                restRequestThreadLocalService.getRequestedWorkspaceId(), backupLocation, backupId, closeConnections, skipDatabaseNames);
        return new BackupV4Response(flowIdentifier);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public RestoreV4Response restoreDatabaseByName(Long workspaceId, String name, String backupLocation, String backupId,
            @AccountId String accountId) {
        FlowIdentifier flowIdentifier = stackOperations.restoreClusterDatabase(NameOrCrn.ofName(name),
                restRequestThreadLocalService.getRequestedWorkspaceId(), backupLocation, backupId);
        return new RestoreV4Response(flowIdentifier);
    }

    @Override
    @InternalOnly
    public RestoreV4Response restoreDatabaseByNameInternal(Long workspaceId, String name, String backupLocation, String backupId,
            @InitiatorUserCrn String initiatorUserCrn) {
        FlowIdentifier flowIdentifier = stackOperations.restoreClusterDatabase(NameOrCrn.ofName(name),
                restRequestThreadLocalService.getRequestedWorkspaceId(), backupLocation, backupId);
        return new RestoreV4Response(flowIdentifier);
    }

    @Override
    @InternalOnly
    public RecoveryV4Response recoverClusterByNameInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        FlowIdentifier flowIdentifier = stackOperations.recoverCluster(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        return new RecoveryV4Response(flowIdentifier);
    }

    @Override
    @InternalOnly
    public RecoveryValidationV4Response getClusterRecoverableByNameInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.validateClusterRecovery(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public UpdateRecipesV4Response refreshRecipes(Long workspaceId, @Valid UpdateRecipesV4Request request, String name,
            @AccountId String accountId) {
        return stackOperations.refreshRecipes(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @InternalOnly
    public UpdateRecipesV4Response refreshRecipesInternal(Long workspaceId, @Valid UpdateRecipesV4Request request, String name,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.refreshRecipes(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public AttachRecipeV4Response attachRecipe(Long workspaceId, @Valid AttachRecipeV4Request request, String name,
            @AccountId String accountId) {
        return stackOperations.attachRecipe(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @InternalOnly
    public AttachRecipeV4Response attachRecipeInternal(Long workspaceId, @Valid AttachRecipeV4Request request, String name,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.attachRecipe(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public DetachRecipeV4Response detachRecipe(Long workspaceId, @Valid DetachRecipeV4Request request, String name, @AccountId String accountId) {
        return stackOperations.detachRecipe(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @InternalOnly
    public DetachRecipeV4Response detachRecipeInternal(Long workspaceId, @Valid DetachRecipeV4Request request, String name,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.detachRecipe(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @InternalOnly
    public CertificatesRotationV4Response rotateAutoTlsCertificates(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn,
            @Valid CertificatesRotationV4Request certificatesRotationV4Request) {
        return stackOperations.rotateAutoTlsCertificates(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(),
                certificatesRotationV4Request);
    }

    @Override
    @InternalOnly
    public FlowIdentifier renewCertificate(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        String accountId = restRequestThreadLocalService.getAccountId();
        return stackOperationService.renewCertificate(name, accountId);
    }

    @Override
    @InternalOnly
    public FlowIdentifier renewInternalCertificate(Long workspaceId, @TenantAwareParam String crn) {
        try {
            return stackOperationService.renewInternalCertificate(crn);
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    @InternalOnly
    public FlowIdentifier updateLoadBalancersInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.updateLoadBalancers(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId());
    }

    @Override
    @InternalOnly
    public void changeImageCatalogInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn,
            ChangeImageCatalogV4Request changeImageCatalogRequest) {
        stackOperations.changeImageCatalog(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(),
                changeImageCatalogRequest.getImageCatalog());
    }

    @Override
    @InternalOnly
    public RangerRazEnabledV4Response rangerRazEnabledInternal(Long workspaceId, String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return new RangerRazEnabledV4Response(stackOperationService.rangerRazEnabled(crn));
    }

    @Override
    @InternalOnly
    public GenerateImageCatalogV4Response generateImageCatalogInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        CloudbreakImageCatalogV3 imageCatalog = stackOperations.generateImageCatalog(NameOrCrn.ofName(name),
                restRequestThreadLocalService.getRequestedWorkspaceId());
        return new GenerateImageCatalogV4Response(imageCatalog);
    }

    @Override
    @InternalOnly
    public FlowIdentifier reRegisterClusterProxyConfig(Long workspaceId, String crn, String originalCrn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperationService.reRegisterClusterProxyConfig(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getAccountId(), originalCrn);
    }

    @Override
    @InternalOnly
    public FlowIdentifier verticalScalingByName(
            Long workspaceId,
            String name,
            @InitiatorUserCrn String initiatorUserCrn,
            @Valid StackVerticalScaleV4Request updateRequest) {
        return stackOperations.putVerticalScaling(NameOrCrn.ofName(name), restRequestThreadLocalService.getAccountId(), updateRequest);
    }

    @Override
    @InternalOnly
    public UsedSubnetsByEnvironmentResponse getUsedSubnetsByEnvironment(Long workspaceId, String environmentCrn) {
        List<SubnetIdWithResourceNameAndCrn> allUsedSubnets = stackOperations.getUsedSubnetsByEnvironment(environmentCrn);
        return new UsedSubnetsByEnvironmentResponse(allUsedSubnets
                .stream().map(s -> new UsedSubnetWithResourceResponse(s.getName(), s.getSubnetId(), s.getResourceCrn(), s.getType().name()))
                .collect(Collectors.toList()));
    }

    @CheckPermissionByResourceName(action = AuthorizationResourceAction.BACKUP_DATALAKE)
    public void determineDatalakeDataSizes(Long workspaceId, @ResourceName String name, String operationId) {
        stackOperations.determineDatalakeDataSizes(restRequestThreadLocalService.getRequestedWorkspaceId(), NameOrCrn.ofName(name), operationId);
    }
}
