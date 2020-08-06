package com.sequenceiq.cloudbreak.controller.v4;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.tags.upgrade.UpgradeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RetryableFlowResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RetryableFlowResponse.Builder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.BackupV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.dr.RestoreV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeOptionV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.upgrade.UpgradeV4Response;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.CloudbreakRestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
@WorkspaceEntityType(Stack.class)
@InternalReady
@AuthorizationResource
public class StackV4Controller extends NotificationController implements StackV4Endpoint {

    @Inject
    private StackOperations stackOperations;

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
        }
        return stackOperations.listByEnvironmentCrn(restRequestThreadLocalService.getRequestedWorkspaceId(), environmentCrn, types);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackV4Response post(Long workspaceId, @Valid StackV4Request request, @AccountId String accountId) {
        return stackOperations.post(restRequestThreadLocalService.getRequestedWorkspaceId(), request, false);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackV4Response get(Long workspaceId, String name, Set<String> entries, @AccountId String accountId) {
        return stackOperations.get(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), entries, null);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public StackV4Response getByCrn(Long workspaceId, @TenantAwareParam String crn, Set<String> entries) {
        return stackOperations.get(NameOrCrn.ofCrn(crn), restRequestThreadLocalService.getRequestedWorkspaceId(), entries, null);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public void delete(Long workspaceId, String name, boolean forced, @AccountId String accountId) {
        stackOperations.delete(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), forced);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier sync(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.sync(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
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
        return stackOperations.putStop(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putStart(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.putStart(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putScaling(Long workspaceId, String name, @Valid StackScaleV4Request updateRequest, @AccountId String accountId) {
        return stackOperations.putScaling(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), updateRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier repairCluster(Long workspaceId, String name, @Valid ClusterRepairV4Request clusterRepairRequest,
            @AccountId String accountId) {
        return stackOperations.repairCluster(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), clusterRepairRequest);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier upgradeOs(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.upgradeOs(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public UpgradeOptionV4Response checkForOsUpgrade(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.checkForOsUpgrade(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
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
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public void deleteWithKerberos(Long workspaceId, String name, boolean forced, @AccountId String accountId) {
        stackOperations.delete(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), forced);
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
        return stackOperations.deleteInstance(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), forced, instanceId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier deleteMultipleInstances(Long workspaceId, String name, @NotEmpty List<String> instanceIds, boolean forced,
            @AccountId String accountId) {
        return stackOperations.deleteInstances(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), instanceIds, forced);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putPassword(Long workspaceId, String name, @Valid UserNamePasswordV4Request userNamePasswordJson,
            @AccountId String accountId) {
        return stackOperations.putPassword(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), userNamePasswordJson);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier setClusterMaintenanceMode(Long workspaceId, String name, @NotNull MaintenanceModeV4Request maintenanceMode,
            @AccountId String accountId) {
        return stackOperations.setClusterMaintenanceMode(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), maintenanceMode);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier putCluster(Long workspaceId, String name, @Valid UpdateClusterV4Request updateJson,
            @AccountId String accountId) {
        return stackOperations.putCluster(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), updateJson);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public String getClusterHostsInventory(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.getClusterHostsInventory(restRequestThreadLocalService.getRequestedWorkspaceId(), name);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public UpgradeV4Response checkForClusterUpgradeByName(Long workspaceId, String name, UpgradeV4Request request,
            @AccountId String accountId) {
        return stackOperations.checkForClusterUpgrade(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier upgradeClusterByName(Long workspaceId, String name, String imageId, @AccountId String accountId) {
        return stackOperations.upgradeCluster(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId(), imageId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier updateSaltByName(Long workspaceId, String name, @AccountId String accountId) {
        return stackOperations.updateSalt(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public FlowIdentifier updatePillarConfigurationByName(Long workspaceId, String name,
            @AccountId String accountId) {
        return stackOperations.updatePillarConfiguration(NameOrCrn.ofName(name), restRequestThreadLocalService.getRequestedWorkspaceId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public BackupV4Response backupDatabaseByName(Long workspaceId, String name, String backupLocation, String backupId,
            @AccountId String accountId) {
        FlowIdentifier flowIdentifier = stackOperations.backupClusterDatabase(NameOrCrn.ofName(name),
                restRequestThreadLocalService.getRequestedWorkspaceId(), backupLocation, backupId);
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
}
