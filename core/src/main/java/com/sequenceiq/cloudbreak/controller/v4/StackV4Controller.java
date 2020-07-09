package com.sequenceiq.cloudbreak.controller.v4;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.InternalOnly;
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
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareApiModelParam;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareCrnParam;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareNameParam;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
@WorkspaceEntityType(Stack.class)
@InternalOnly
@InternalReady
public class StackV4Controller extends NotificationController implements StackV4Endpoint {

    @Inject
    private StackOperations stackOperations;

    @Override
    public StackViewV4Responses list(Long workspaceId, @TenantAwareCrnParam String environmentCrn, boolean onlyDatalakes) {
        List<StackType> types = new ArrayList<>();
        if (onlyDatalakes) {
            types.add(StackType.DATALAKE);
        } else {
            types.add(StackType.DATALAKE);
            types.add(StackType.WORKLOAD);
        }
        return stackOperations.listByEnvironmentCrn(workspaceId, environmentCrn, types);
    }

    @Override
    public StackV4Response post(Long workspaceId, @Valid @TenantAwareApiModelParam StackV4Request request) {
        return stackOperations.post(workspaceId, request, false);
    }

    @Override
    public StackV4Response get(Long workspaceId, @TenantAwareNameParam String name, Set<String> entries) {
        return stackOperations.get(NameOrCrn.ofName(name), workspaceId, entries, null);
    }

    @Override
    public StackV4Response getByCrn(Long workspaceId, String crn, Set<String> entries) {
        return stackOperations.get(NameOrCrn.ofCrn(crn), workspaceId, entries, null);
    }

    @Override
    public void delete(Long workspaceId, @TenantAwareNameParam String name, boolean forced) {
        stackOperations.delete(NameOrCrn.ofName(name), workspaceId, forced);
    }

    @Override
    public FlowIdentifier sync(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.sync(NameOrCrn.ofName(name), workspaceId);
    }

    @Override
    public FlowIdentifier retry(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.retry(NameOrCrn.ofName(name), workspaceId);
    }

    @Override
    public List<RetryableFlowResponse> listRetryableFlows(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.getRetryableFlows(name, workspaceId)
                .stream().map(retryable -> Builder.builder().setName(retryable.getName()).setFailDate(retryable.getFailDate()).build())
                .collect(Collectors.toList());
    }

    @Override
    public FlowIdentifier putStop(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.putStop(NameOrCrn.ofName(name), workspaceId);
    }

    @Override
    public FlowIdentifier putStart(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.putStart(NameOrCrn.ofName(name), workspaceId);
    }

    @Override
    public FlowIdentifier putScaling(Long workspaceId, @TenantAwareNameParam String name, @Valid StackScaleV4Request updateRequest) {
        return stackOperations.putScaling(NameOrCrn.ofName(name), workspaceId, updateRequest);
    }

    @Override
    public FlowIdentifier repairCluster(Long workspaceId, @TenantAwareNameParam String name, @Valid ClusterRepairV4Request clusterRepairRequest) {
        return stackOperations.repairCluster(NameOrCrn.ofName(name), workspaceId, clusterRepairRequest);
    }

    @Override
    public FlowIdentifier upgradeOs(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.upgradeOs(NameOrCrn.ofName(name), workspaceId);
    }

    @Override
    public UpgradeOptionV4Response checkForOsUpgrade(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.checkForOsUpgrade(NameOrCrn.ofName(name), workspaceId);
    }

    @Override
    public GeneratedBlueprintV4Response postStackForBlueprint(Long workspaceId, @TenantAwareNameParam String name, @Valid StackV4Request stackRequest) {
        return stackOperations.postStackForBlueprint(stackRequest);
    }

    @Override
    public FlowIdentifier changeImage(Long workspaceId, @TenantAwareNameParam String name, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        return stackOperations.changeImage(NameOrCrn.ofName(name), workspaceId, stackImageChangeRequest);
    }

    @Override
    public void deleteWithKerberos(Long workspaceId, @TenantAwareNameParam String name, boolean forced) {
        stackOperations.delete(NameOrCrn.ofName(name), workspaceId, forced);
    }

    @Override
    public StackV4Request getRequestfromName(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.getRequest(NameOrCrn.ofName(name), workspaceId);
    }

    @Override
    public StackStatusV4Response getStatusByName(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.getStatus(NameOrCrn.ofName(name), workspaceId);
    }

    @Override
    public FlowIdentifier deleteInstance(Long workspaceId, @TenantAwareNameParam String name, boolean forced, String instanceId) {
        return stackOperations.deleteInstance(NameOrCrn.ofName(name), workspaceId, forced, instanceId);
    }

    @Override
    public FlowIdentifier deleteMultipleInstances(Long workspaceId, @TenantAwareNameParam String name, List<String> instanceIds, boolean forced) {
        return stackOperations.deleteInstances(NameOrCrn.ofName(name), workspaceId, instanceIds, forced);
    }

    @Override
    public FlowIdentifier putPassword(Long workspaceId, @TenantAwareNameParam String name, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        return stackOperations.putPassword(NameOrCrn.ofName(name), workspaceId, userNamePasswordJson);
    }

    @Override
    public FlowIdentifier setClusterMaintenanceMode(Long workspaceId, @TenantAwareNameParam String name, @NotNull MaintenanceModeV4Request maintenanceMode) {
        return stackOperations.setClusterMaintenanceMode(NameOrCrn.ofName(name), workspaceId, maintenanceMode);
    }

    @Override
    public FlowIdentifier putCluster(Long workspaceId, @TenantAwareNameParam String name, @Valid UpdateClusterV4Request updateJson) {
        return stackOperations.putCluster(NameOrCrn.ofName(name), workspaceId, updateJson);
    }

    @Override
    public String getClusterHostsInventory(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.getClusterHostsInventory(workspaceId, name);
    }

    @Override
    public UpgradeV4Response checkForClusterUpgradeByName(Long workspaceId, @TenantAwareNameParam String name, UpgradeV4Request request) {
        return stackOperations.checkForClusterUpgrade(NameOrCrn.ofName(name), workspaceId, request);
    }

    @Override
    public FlowIdentifier upgradeClusterByName(Long workspaceId, @TenantAwareNameParam String name, String imageId) {
        return stackOperations.upgradeCluster(NameOrCrn.ofName(name), workspaceId, imageId);
    }

    @Override
    public FlowIdentifier updateSaltByName(Long workspaceId, @TenantAwareNameParam String name) {
        return stackOperations.updateSalt(NameOrCrn.ofName(name), workspaceId);
    }

    @Override
    public BackupV4Response backupDatabaseByName(Long workspaceId, @TenantAwareNameParam String name, String backupLocation, String backupId) {
        FlowIdentifier flowIdentifier =
            stackOperations.backupClusterDatabase(NameOrCrn.ofName(name), workspaceId, backupLocation, backupId);
        return new BackupV4Response(flowIdentifier);
    }

    @Override
    public RestoreV4Response restoreDatabaseByName(Long workspaceId, @TenantAwareNameParam String name, String backupLocation, String backupId) {
        FlowIdentifier flowIdentifier =
            stackOperations.restoreClusterDatabase(NameOrCrn.ofName(name), workspaceId, backupLocation, backupId);
        return new RestoreV4Response(flowIdentifier);
    }
}
