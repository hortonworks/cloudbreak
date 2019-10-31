package com.sequenceiq.cloudbreak.controller.v4;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.dto.StackAccessDto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RetryableFlowResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.distrox.v1.distrox.StackOperations;

@Controller
@WorkspaceEntityType(Stack.class)
public class StackV4Controller extends NotificationController implements StackV4Endpoint {

    @Inject
    private StackOperations stackOperations;

    @Override
    public StackViewV4Responses list(Long workspaceId, String environmentCrn, Boolean onlyDatalakes) {
        StackType type = null;
        if (onlyDatalakes) {
            type = StackType.DATALAKE;
        }
        return stackOperations.listByEnvironmentCrn(workspaceId, environmentCrn, type);
    }

    @Override
    public StackV4Response post(Long workspaceId, @Valid StackV4Request request) {
        return stackOperations.post(workspaceId, request);
    }

    @Override
    public StackV4Response get(Long workspaceId, String name, Set<String> entries) {
        return stackOperations.get(StackAccessDto.builder().withName(name).build(), workspaceId, entries, null);
    }

    @Override
    public void delete(Long workspaceId, String name, Boolean forced) {
        stackOperations.delete(StackAccessDto.builder().withName(name).build(), workspaceId, forced);
    }

    @Override
    public void sync(Long workspaceId, String name) {
        stackOperations.sync(StackAccessDto.builder().withName(name).build(), workspaceId);
    }

    @Override
    public void retry(Long workspaceId, String name) {
        stackOperations.retry(StackAccessDto.builder().withName(name).build(), workspaceId);
    }

    @Override
    public List<RetryableFlowResponse> listRetryableFlows(Long workspaceId, String name) {
        return stackOperations.getRetryableFlows(name, workspaceId)
                .stream().map(retryable -> RetryableFlowResponse.Builder.builder().setName(retryable.getName()).setFailDate(retryable.getFailDate()).build())
                .collect(Collectors.toList());
    }

    @Override
    public void putStop(Long workspaceId, String name) {
        stackOperations.putStop(StackAccessDto.builder().withName(name).build(), workspaceId);
    }

    @Override
    public void putStart(Long workspaceId, String name) {
        stackOperations.putStart(StackAccessDto.builder().withName(name).build(), workspaceId);
    }

    @Override
    public void putScaling(Long workspaceId, String name, @Valid StackScaleV4Request updateRequest) {
        stackOperations.putScaling(StackAccessDto.builder().withName(name).build(), workspaceId, updateRequest);
    }

    @Override
    public void repairCluster(Long workspaceId, String name, @Valid ClusterRepairV4Request clusterRepairRequest) {
        stackOperations.repairCluster(StackAccessDto.builder().withName(name).build(), workspaceId, clusterRepairRequest);
    }

    @Override
    public GeneratedBlueprintV4Response postStackForBlueprint(Long workspaceId, String name, @Valid StackV4Request stackRequest) {
        return stackOperations.postStackForBlueprint(StackAccessDto.builder().withName(name).build(), workspaceId, stackRequest);
    }

    @Override
    public void changeImage(Long workspaceId, String name, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        stackOperations.changeImage(StackAccessDto.builder().withName(name).build(), workspaceId, stackImageChangeRequest);
    }

    @Override
    public void deleteWithKerberos(Long workspaceId, String name, Boolean withStackDelete) {
        stackOperations.deleteWithKerberos(StackAccessDto.builder().withName(name).build(), workspaceId, withStackDelete);
    }

    @Override
    public StackV4Request getRequestfromName(Long workspaceId, String name) {
        return stackOperations.getRequest(StackAccessDto.builder().withName(name).build(), workspaceId);
    }

    @Override
    public StackStatusV4Response getStatusByName(Long workspaceId, String name) {
        return stackOperations.getStatus(StackAccessDto.builder().withName(name).build(), workspaceId);
    }

    @Override
    public void deleteInstance(Long workspaceId, String name, Boolean forced, String instanceId) {
        stackOperations.deleteInstance(StackAccessDto.builder().withName(name).build(), workspaceId, forced, instanceId);
    }

    @Override
    public void deleteMultipleInstances(Long workspaceId, String name, List<String> instanceIds, boolean forced) {
        stackOperations.deleteInstances(StackAccessDto.builder().withName(name).build(), workspaceId, instanceIds, forced);
    }

    @Override
    public void putPassword(Long workspaceId, String name, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        stackOperations.putPassword(StackAccessDto.builder().withName(name).build(), workspaceId, userNamePasswordJson);
    }

    @Override
    public void setClusterMaintenanceMode(Long workspaceId, String name, @NotNull MaintenanceModeV4Request maintenanceMode) {
        stackOperations.setClusterMaintenanceMode(StackAccessDto.builder().withName(name).build(), workspaceId, maintenanceMode);
    }

    @Override
    public void putCluster(Long workspaceId, String name, @Valid UpdateClusterV4Request updateJson) {
        stackOperations.putCluster(StackAccessDto.builder().withName(name).build(), workspaceId, updateJson);
    }

    @Override
    public String getClusterHostsInventory(Long workspaceId, String name) {
        return stackOperations.getClusterHostsInventory(workspaceId, name);
    }
}
