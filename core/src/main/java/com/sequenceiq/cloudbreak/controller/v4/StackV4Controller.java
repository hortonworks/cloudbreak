package com.sequenceiq.cloudbreak.controller.v4;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests.StackDatabaseServerCertificateStatusV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.requests.StackV4SecretRotationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.rotation.response.StackDatabaseServerCertificateStatusV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.CertificatesRotationV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ChangeImageCatalogV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ExternalDatabaseManageDatabaseUserV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.RotateSaltPasswordRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.SetDefaultJavaVersionRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackVerticalScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.cluster.cm.ClouderaManagerSyncV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.imdupdate.StackInstanceMetadataUpdateV4Request;
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
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.migraterds.MigrateDatabaseV1Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.AttachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.DetachRecipeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recipe.UpdateRecipesV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.recovery.RecoveryValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.rotaterdscert.StackRotateRdsCertificateV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.RdsUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackCcmUpgradeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.StackOutboundTypeValidationV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.api.model.RotateSaltPasswordReason;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.InitiatorUserCrn;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.cloud.model.catalog.CloudbreakImageCatalogV3;
import com.sequenceiq.cloudbreak.common.database.TargetMajorVersion;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.SubnetIdWithResourceNameAndCrn;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.rotaterdscert.StackRotateRdsCertificateService;
import com.sequenceiq.cloudbreak.service.stack.StackInstanceMetadataUpdateService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackRotationService;
import com.sequenceiq.cloudbreak.service.upgrade.ccm.StackCcmUpgradeService;
import com.sequenceiq.cloudbreak.service.upgrade.defaultoutbound.StackDefaultOutboundUpgradeService;
import com.sequenceiq.cloudbreak.service.upgrade.rds.RdsUpgradeService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.common.api.UsedSubnetWithResourceResponse;
import com.sequenceiq.common.api.UsedSubnetsByEnvironmentResponse;
import com.sequenceiq.common.model.SeLinux;
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
    private StackDefaultOutboundUpgradeService defaultOutboundUpgradeService;

    @Inject
    private RdsUpgradeService rdsUpgradeService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private StackRotationService stackRotationService;

    @Inject
    private StackInstanceMetadataUpdateService instanceMetadataUpdateService;

    @Inject
    private StackRotateRdsCertificateService rotateRdsCertificateService;

    @Inject
    private StackService stackService;

    @Inject
    private ClusterService clusterService;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackViewV4Responses list(Long workspaceId, @ResourceCrn String environmentCrn, boolean onlyDatalakes) {
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
    public StackV4Response post(Long workspaceId, StackV4Request request, @AccountId String accountId) {
        return stackOperations.post(restRequestThreadLocalService.getRequestedWorkspaceId(), restRequestThreadLocalService.getCloudbreakUser(), request, false);
    }

    @Override
    @InternalOnly
    public StackV4Response postInternal(Long workspaceId, StackV4Request request, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.post(restRequestThreadLocalService.getRequestedWorkspaceId(), restRequestThreadLocalService.getCloudbreakUser(), request, false);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackV4Response get(Long workspaceId, String name, Set<String> entries, @AccountId String accountId) {
        return stackOperations.get(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), entries, null, false);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackV4Response getWithResources(Long workspaceId, String name, Set<String> entries, @AccountId String accountId) {
        return stackOperations.get(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), entries, null, true);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackV4Response getByCrn(Long workspaceId, @ResourceCrn String crn, Set<String> entries) {
        return stackOperations.get(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), entries, null, false);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public void delete(Long workspaceId, String name, boolean forced, @AccountId String accountId) {
        stackOperations.delete(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), forced);
    }

    @Override
    @InternalOnly
    public void deleteInternal(Long workspaceId, String name, boolean forced, @InitiatorUserCrn String initiatorUserCrn) {
        stackOperations.delete(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), forced);
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
    public void updateLoadBalancerPEMDNS(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        stackOperations.updateLoadBalancerPEMDNS(restRequestThreadLocalService.getRequestedWorkspaceId(), NameOrCrn.ofName(name));
    }

    @Override
    @InternalOnly
    public void updateLoadBalancerIPADNS(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        stackOperations.updateLoadBalancerIPADNS(restRequestThreadLocalService.getRequestedWorkspaceId(), NameOrCrn.ofName(name));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier sync(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.sync(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), EnumSet.of(StackType.WORKLOAD, StackType.DATALAKE));
    }

    @Override
    @InternalOnly
    public FlowIdentifier syncCm(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn, ClouderaManagerSyncV4Request syncRequest) {
        return stackOperations.syncComponentVersionsFromCm(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(),
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
        return stackOperations.putStop(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @InternalOnly
    public FlowIdentifier putStopInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.putStop(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putStart(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.putStart(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @InternalOnly
    public FlowIdentifier putStartInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.putStart(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @InternalOnly
    public FlowIdentifier rotateSaltPasswordInternal(Long workspaceId, String crn, RotateSaltPasswordRequest request,
            @InitiatorUserCrn String initiatorUserCrn) {
        RotateSaltPasswordReason rotateSaltPasswordReason = RotateSaltPasswordReason.valueOf(request.getReason().name());
        return stackOperations.rotateSaltPassword(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), rotateSaltPasswordReason);
    }

    @Override
    @InternalOnly
    public SaltPasswordStatusResponse getSaltPasswordStatus(Long workspaceId, @ResourceCrn String crn) {
        SaltPasswordStatusResponse response = new SaltPasswordStatusResponse();
        SaltPasswordStatus saltPasswordStatus = stackOperations.getSaltPasswordStatus(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
        response.setStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.SaltPasswordStatus.valueOf(saltPasswordStatus.name()));
        return response;
    }

    @Override
    @InternalOnly
    public FlowIdentifier modifyProxyConfigInternal(Long workspaceId, String crn, String previousProxyConfigCrn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.modifyProxyConfig(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), previousProxyConfigCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putScaling(Long workspaceId, String name, StackScaleV4Request updateRequest, @AccountId String accountId) {
        return stackOperations.putScaling(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), updateRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier repairCluster(Long workspaceId, String name, ClusterRepairV4Request clusterRepairRequest,
            @AccountId String accountId) {
        return stackOperations.repairCluster(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), clusterRepairRequest);
    }

    @Override
    @InternalOnly
    public FlowIdentifier repairClusterInternal(Long workspaceId, String name, ClusterRepairV4Request clusterRepairRequest,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.repairCluster(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), clusterRepairRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier upgradeOs(Long workspaceId, String name, @AccountId String accountId, Boolean keepVariant) {
        return stackUpgradeOperations.upgradeOs(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), Boolean.TRUE.equals(keepVariant));
    }

    @InternalOnly
    public FlowIdentifier upgradeOsByUpgradeSetsInternal(Long workspaceId, @ResourceCrn String crn, OrderedOSUpgradeSetRequest orderedOsUpgradeSetRequest) {
        return stackUpgradeOperations.upgradeOsByUpgradeSets(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId(),
                orderedOsUpgradeSetRequest);
    }

    @Override
    @InternalOnly
    public FlowIdentifier upgradeOsInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn, Boolean keepVariant) {
        return stackUpgradeOperations.upgradeOs(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), Boolean.TRUE.equals(keepVariant));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public UpgradeOptionV4Response checkForOsUpgrade(Long workspaceId, String name, @AccountId String accountId) {
        throw new UnsupportedOperationException("Please use the new upgrade endpoint with dry-run option.");
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public GeneratedBlueprintV4Response postStackForBlueprint(Long workspaceId, String name, StackV4Request stackRequest,
            @AccountId String accountId) {
        return stackOperations.postStackForBlueprint(stackRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier changeImage(Long workspaceId, String name, StackImageChangeV4Request stackImageChangeRequest,
            @AccountId String accountId) {
        return stackOperations.changeImage(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), stackImageChangeRequest);
    }

    @Override
    @InternalOnly
    public FlowIdentifier changeImageInternal(Long workspaceId, String name, StackImageChangeV4Request stackImageChangeRequest,
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
    public FlowIdentifier deleteInstance(Long workspaceId, String name, boolean forced, String instanceId, @AccountId String accountId) {
        return stackOperations.deleteInstance(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), forced, instanceId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier deleteMultipleInstances(Long workspaceId, String name, List<String> instanceIds, boolean forced, @AccountId String accountId) {
        return stackOperations.deleteInstances(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(),
                new HashSet<>(instanceIds), forced);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putPassword(Long workspaceId, String name, UserNamePasswordV4Request userNamePasswordJson, @AccountId String accountId) {
        return stackOperations.putPassword(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), userNamePasswordJson);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier setClusterMaintenanceMode(Long workspaceId, String name, MaintenanceModeV4Request maintenanceMode, @AccountId String accountId) {
        return stackOperations.setClusterMaintenanceMode(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), maintenanceMode);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putCluster(Long workspaceId, String name, UpdateClusterV4Request updateJson, @AccountId String accountId) {
        return stackOperations.putCluster(name, accountId, updateJson);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public String getClusterHostsInventory(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.getClusterHostsInventory(restRequestThreadLocalService.getRequestedWorkspaceId(), name);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public UpgradeV4Response checkForClusterUpgradeByName(Long workspaceId, String name, UpgradeV4Request request, @AccountId String accountId) {
        return stackUpgradeOperations.checkForClusterUpgrade(accountId, NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(),
                request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier upgradeClusterByName(Long workspaceId, String name, String imageId, @AccountId String accountId) {
        return stackUpgradeOperations.upgradeCluster(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), imageId, Boolean.FALSE);
    }

    @Override
    @InternalOnly
    public FlowIdentifier upgradeClusterByNameInternal(Long workspaceId, String name, String imageId, @InitiatorUserCrn String initiatorUserCrn,
            Boolean rollingUpgradeEnabled) {
        return stackUpgradeOperations.upgradeCluster(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(), imageId, rollingUpgradeEnabled);
    }

    @Override
    @InternalOnly
    public FlowIdentifier prepareClusterUpgradeByCrnInternal(Long workspaceId, String crn, String imageId, @InitiatorUserCrn String initiatorUserCrn) {
        return stackUpgradeOperations.prepareClusterUpgrade(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), imageId);
    }

    @Override
    @InternalOnly
    public void checkUpgradeRdsByClusterNameInternal(Long workspaceId, @ResourceName String clusterName, TargetMajorVersion targetMajorVersion,
            @InitiatorUserCrn String initiatorUserCrn) {
        rdsUpgradeService.checkUpgradeRds(NameOrCrn.ofName(clusterName), targetMajorVersion);
    }

    @Override
    @InternalOnly
    public RdsUpgradeV4Response upgradeRdsByClusterNameInternal(Long workspaceId, @ResourceName String clusterName, TargetMajorVersion targetMajorVersion,
            @InitiatorUserCrn String initiatorUserCrn, boolean forced) {
        return rdsUpgradeService.upgradeRds(NameOrCrn.ofName(clusterName), targetMajorVersion, forced);
    }

    @Override
    @InternalOnly
    public StackCcmUpgradeV4Response upgradeCcmByNameInternal(Long workspaceId, @ResourceName String name, @InitiatorUserCrn String initiatorUserCrn) {
        return stackCcmUpgradeService.upgradeCcm(NameOrCrn.ofName(name));
    }

    @Override
    @InternalOnly
    public StackCcmUpgradeV4Response upgradeCcmByCrnInternal(Long workspaceId, String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackCcmUpgradeService.upgradeCcm(NameOrCrn.ofCrn(crn));
    }

    @Override
    @InternalOnly
    public int getNotCcmUpgradedStackCount(Long workspaceId, String envCrn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackCcmUpgradeService.getNotUpgradedStackCount(envCrn);
    }

    @Override
    @InternalOnly
    public StackOutboundTypeValidationV4Response validateStackOutboundTypes(Long workspaceId, String envCrn, @InitiatorUserCrn String initiatorUserCrn) {
        return defaultOutboundUpgradeService.getStacksWithOutboundType(workspaceId, envCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier updateSaltByName(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.updateSalt(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId());
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
    public FlowIdentifier updatePillarConfigurationByCrn(Long workspaceId, @ResourceCrn String crn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return stackOperations.updatePillarConfiguration(NameOrCrn.ofCrn(crn), accountId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    @SuppressWarnings("ParameterNumber")
    public BackupV4Response backupDatabaseByName(Long workspaceId, String name, String backupLocation, String backupId, List<String> skipDatabaseNames,
            @AccountId String accountId, int databaseMaxDurationInMin, boolean dryRun) {
        FlowIdentifier flowIdentifier = stackOperations.backupClusterDatabase(NameOrCrn.ofName(name),
                restRequestThreadLocalService.getRequestedWorkspaceId(), backupLocation, backupId, true, skipDatabaseNames,
                databaseMaxDurationInMin, dryRun);
        return new BackupV4Response(flowIdentifier);
    }

    @Override
    @InternalOnly
    @SuppressWarnings("ParameterNumber")
    public BackupV4Response backupDatabaseByNameInternal(Long workspaceId, String name, String backupId, String backupLocation,
            boolean closeConnections, List<String> skipDatabaseNames, @InitiatorUserCrn String initiatorUserCrn, int databaseMaxDurationInMin,
            boolean dryRun) {
        FlowIdentifier flowIdentifier = stackOperations.backupClusterDatabase(NameOrCrn.ofName(name),
                restRequestThreadLocalService.getRequestedWorkspaceId(), backupLocation, backupId, closeConnections, skipDatabaseNames,
                databaseMaxDurationInMin, dryRun);
        return new BackupV4Response(flowIdentifier);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    @SuppressWarnings("ParameterNumber")
    public RestoreV4Response restoreDatabaseByName(Long workspaceId, String name, String backupLocation, String backupId,
            @AccountId String accountId, int databaseMaxDurationInMin, boolean dryRun) {
        FlowIdentifier flowIdentifier = stackOperations.restoreClusterDatabase(NameOrCrn.ofName(name),
                restRequestThreadLocalService.getRequestedWorkspaceId(), backupLocation, backupId, databaseMaxDurationInMin, dryRun);
        return new RestoreV4Response(flowIdentifier);
    }

    @Override
    @InternalOnly
    @SuppressWarnings("ParameterNumber")
    public RestoreV4Response restoreDatabaseByNameInternal(Long workspaceId, String name, String backupLocation, String backupId,
            @InitiatorUserCrn String initiatorUserCrn, int databaseMaxDurationInMin, boolean dryRun) {
        FlowIdentifier flowIdentifier = stackOperations.restoreClusterDatabase(NameOrCrn.ofName(name),
                restRequestThreadLocalService.getRequestedWorkspaceId(), backupLocation, backupId, databaseMaxDurationInMin, dryRun);
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
    public UpdateRecipesV4Response refreshRecipes(Long workspaceId, UpdateRecipesV4Request request, String name,
            @AccountId String accountId) {
        return stackOperations.refreshRecipes(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @InternalOnly
    public UpdateRecipesV4Response refreshRecipesInternal(Long workspaceId, UpdateRecipesV4Request request, String name,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.refreshRecipes(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public AttachRecipeV4Response attachRecipe(Long workspaceId, AttachRecipeV4Request request, String name,
            @AccountId String accountId) {
        return stackOperations.attachRecipe(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @InternalOnly
    public AttachRecipeV4Response attachRecipeInternal(Long workspaceId, AttachRecipeV4Request request, String name,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.attachRecipe(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public DetachRecipeV4Response detachRecipe(Long workspaceId, DetachRecipeV4Request request, String name, @AccountId String accountId) {
        return stackOperations.detachRecipe(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @InternalOnly
    public DetachRecipeV4Response detachRecipeInternal(Long workspaceId, DetachRecipeV4Request request, String name,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.detachRecipe(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @InternalOnly
    public CertificatesRotationV4Response rotateAutoTlsCertificates(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn,
            CertificatesRotationV4Request certificatesRotationV4Request) {
        return stackOperations.rotateAutoTlsCertificates(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId(),
                certificatesRotationV4Request);
    }

    @Override
    @InternalOnly
    public FlowIdentifier renewCertificate(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return stackOperationService.renewCertificate(name, accountId);
    }

    @Override
    @InternalOnly
    public FlowIdentifier renewInternalCertificate(Long workspaceId, @ResourceCrn String crn) {
        try {
            return stackOperationService.renewInternalCertificate(crn);
        } catch (Exception e) {
            throw new BadRequestException(e.getMessage());
        }
    }

    @Override
    @InternalOnly
    public FlowIdentifier updateLoadBalancersInternal(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.updateLoadBalancers(NameOrCrn.ofName(name), ThreadBasedUserCrnProvider.getAccountId());
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
    public FlowIdentifier reRegisterClusterProxyConfig(Long workspaceId, String crn, boolean skipFullReRegistration, String originalCrn,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperationService.reRegisterClusterProxyConfig(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(),
                skipFullReRegistration, originalCrn);
    }

    @Override
    @InternalOnly
    public FlowIdentifier verticalScalingByName(Long workspaceId, String name, @InitiatorUserCrn String initiatorUserCrn,
            StackVerticalScaleV4Request updateRequest) {
        Stack stack = stackService.getByNameOrCrnAndWorkspaceIdWithLists(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
        return stackOperations.putVerticalScaling(stack, updateRequest);
    }

    @Override
    @InternalOnly
    public UsedSubnetsByEnvironmentResponse getUsedSubnetsByEnvironment(Long workspaceId, @ResourceCrn String environmentCrn) {
        List<SubnetIdWithResourceNameAndCrn> allUsedSubnets = stackOperations.getUsedSubnetsByEnvironment(environmentCrn);
        return new UsedSubnetsByEnvironmentResponse(allUsedSubnets
                .stream().map(s -> new UsedSubnetWithResourceResponse(s.getName(), s.getSubnetId(), s.getResourceCrn(), s.getType().name()))
                .collect(Collectors.toList()));
    }

    @CheckPermissionByResourceName(action = AuthorizationResourceAction.BACKUP_DATALAKE)
    public void determineDatalakeDataSizes(Long workspaceId, @ResourceName String name, String operationId) {
        stackOperations.determineDatalakeDataSizes(restRequestThreadLocalService.getRequestedWorkspaceId(), NameOrCrn.ofName(name), operationId);
    }

    @Override
    @InternalOnly
    public FlowIdentifier rotateSecrets(Long workspaceId, StackV4SecretRotationRequest request, @InitiatorUserCrn String initiatorUserCrn) {
        return stackRotationService.rotateSecrets(request.getCrn(), List.of(request.getSecret()), request.getExecutionType(), request.getAdditionalProperties());
    }

    @Override
    @InternalOnly
    public FlowIdentifier refreshEntitlementParams(Long workspaceId, String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperations.refreshEntitlementParams(crn);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DATALAKE_HORIZONTAL_SCALING)
    public FlowIdentifier rollingRestartServices(Long workspaceId, @ResourceCrn String crn, boolean restartStaleServices) {
        return stackOperationService.triggerServicesRollingRestart(crn, restartStaleServices);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier triggerSkuMigration(Long workspaceId, String name, boolean force, @InitiatorUserCrn String initiatorUserCrn) {
        Crn userCrn = Crn.ofUser(initiatorUserCrn);
        return stackOperationService.triggerSkuMigration(NameOrCrn.ofName(name), userCrn.getAccountId(), force);
    }

    @Override
    @InternalOnly
    public FlowIdentifier instanceMetadataUpdate(Long workspaceId, @InitiatorUserCrn String initiatorUserCrn,
            StackInstanceMetadataUpdateV4Request request) {
        return instanceMetadataUpdateService.updateInstanceMetadata(request.getCrn(), request.getUpdateType());
    }

    @Override
    @InternalOnly
    public StackRotateRdsCertificateV4Response rotateRdsCertificateByCrnInternal(Long workspaceId, String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return rotateRdsCertificateService.rotateRdsCertificate(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @InternalOnly
    public MigrateDatabaseV1Response migrateDatabaseByCrnInternal(Long workspaceId, String crn, @InitiatorUserCrn String initiatorUserCrn) {
        return null;
    }

    @Override
    @InternalOnly
    public void validateRotateRdsCertificateByCrnInternal(Long workspaceId, String crn, @InitiatorUserCrn String initiatorUserCrn) {
        rotateRdsCertificateService.validateThatRotationIsTriggerable(ThreadBasedUserCrnProvider.getAccountId(), crn);
    }

    @Override
    @InternalOnly
    public StackDatabaseServerCertificateStatusV4Responses internalListDatabaseServersCertificateStatus(Long workspaceId,
            StackDatabaseServerCertificateStatusV4Request request, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperationService.listDatabaseServersCertificateStatus(request, ThreadBasedUserCrnProvider.getUserCrn());
    }

    @Override
    @InternalOnly
    public FlowIdentifier setDefaultJavaVersionByCrnInternal(Long workspaceId, String crn, SetDefaultJavaVersionRequest request,
            @InitiatorUserCrn String initiatorUserCrn) {
        NameOrCrn nameOrCrn = NameOrCrn.ofCrn(crn);
        return stackOperationService.triggerSetDefaultJavaVersion(nameOrCrn, ThreadBasedUserCrnProvider.getAccountId(), request);
    }

    @Override
    @InternalOnly
    public void validateDefaultJavaVersionUpdateByCrnInternal(Long workspaceId, @ResourceCrn String crn, SetDefaultJavaVersionRequest request) {
        stackOperationService.validateDefaultJavaVersionUpdate(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), request);
    }

    @Override
    @InternalOnly
    public List<String> listAvailableJavaVersionsByCrnInternal(Long workspaceId, @ResourceCrn String crn) {
        return stackOperationService.listAvailableJavaVersions(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId());
    }

    @Override
    @InternalOnly
    public FlowIdentifier updateRootVolumeByStackCrnInternal(Long workspaceId, @ResourceCrn String crn, DiskUpdateRequest rootDiskVolumesRequest,
            @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperationService.rootVolumeDiskUpdate(NameOrCrn.ofCrn(crn), rootDiskVolumesRequest, ThreadBasedUserCrnProvider.getAccountId());
    }

    @InternalOnly
    @Override
    public FlowIdentifier manageDatabaseUser(Long workspaceId, ExternalDatabaseManageDatabaseUserV4Request rq, @InitiatorUserCrn String initiatorUserCrn) {
        return stackOperationService.manageDatabaseUser(rq.getCrn(), rq.getDbUser(), rq.getDbType(), rq.getOperation());
    }

    @InternalOnly
    @Override
    public FlowIdentifier modifySeLinuxByCrn(Long workspaceId, @ResourceCrn String crn, SeLinux selinuxMode) {
        return stackOperationService.triggerModifySELinux(NameOrCrn.ofCrn(crn), ThreadBasedUserCrnProvider.getAccountId(), selinuxMode);
    }

    @InternalOnly
    @Override
    public FlowIdentifier updatePublicDnsEntriesByCrn(Long workspaceId, @ResourceCrn String crn, @InitiatorUserCrn String initiatorUserCrn) {
        Crn userCrn = Crn.ofUser(initiatorUserCrn);
        return stackOperationService.triggerUpdatePublicDnsEntries(NameOrCrn.ofCrn(crn), userCrn.getAccountId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.DESCRIBE_ENCRYPTION_PROFILE)
    public List<String> getClustersNamesByEncrytionProfile(Long workspaceId, String encryptionProfileName, @AccountId String accountId) {
        return clusterService.getAllClusterNamesUsingEncrytionProfile(encryptionProfileName, accountId);
    }
}
