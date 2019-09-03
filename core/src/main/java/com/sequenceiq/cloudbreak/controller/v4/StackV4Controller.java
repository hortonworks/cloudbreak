package com.sequenceiq.cloudbreak.controller.v4;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.dto.StackAccessDto.StackAccessDtoBuilder.aStackAccessDtoBuilder;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.StackV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.ClusterRepairV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.MaintenanceModeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackImageChangeV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UpdateClusterV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.UserNamePasswordV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.workspace.controller.WorkspaceEntityType;
import com.sequenceiq.distrox.v1.distrox.StackOperation;

@Controller
@WorkspaceEntityType(Stack.class)
public class StackV4Controller extends NotificationController implements StackV4Endpoint {

    @Inject
    private StackOperation stackOperation;

    @Override
    public StackViewV4Responses list(Long workspaceId, String environmentCrn, Boolean onlyDatalakes) {
        StackType type = null;
        if (onlyDatalakes) {
            type = StackType.DATALAKE;
        }
        return stackOperation.listByEnvironmentCrn(workspaceId, environmentCrn, type);
    }

    @Override
    public StackV4Response post(Long workspaceId, @Valid StackV4Request request) {
        return stackOperation.post(workspaceId, request);
    }

    @Override
    public StackV4Response get(Long workspaceId, String name, Set<String> entries) {
        return stackOperation.get(aStackAccessDtoBuilder().withName(name).build(), workspaceId, entries, null);
    }

    @Override
    public void delete(Long workspaceId, String name, Boolean forced) {
        stackOperation.delete(aStackAccessDtoBuilder().withName(name).build(), workspaceId, forced);
    }

    @Override
    public void sync(Long workspaceId, String name) {
        stackOperation.sync(aStackAccessDtoBuilder().withName(name).build(), workspaceId);
    }

    @Override
    public void retry(Long workspaceId, String name) {
        stackOperation.retry(aStackAccessDtoBuilder().withName(name).build(), workspaceId);
    }

    @Override
    public void putStop(Long workspaceId, String name) {
        stackOperation.putStop(aStackAccessDtoBuilder().withName(name).build(), workspaceId);
    }

    @Override
    public void putStart(Long workspaceId, String name) {
        stackOperation.putStart(aStackAccessDtoBuilder().withName(name).build(), workspaceId);
    }

    @Override
    public void putScaling(Long workspaceId, String name, @Valid StackScaleV4Request updateRequest) {
        stackOperation.putScaling(aStackAccessDtoBuilder().withName(name).build(), workspaceId, updateRequest);
    }

    @Override
    public void repairCluster(Long workspaceId, String name, @Valid ClusterRepairV4Request clusterRepairRequest) {
        stackOperation.repairCluster(aStackAccessDtoBuilder().withName(name).build(), workspaceId, clusterRepairRequest);
    }

    @Override
    public GeneratedBlueprintV4Response postStackForBlueprint(Long workspaceId, String name, @Valid StackV4Request stackRequest) {
        return stackOperation.postStackForBlueprint(aStackAccessDtoBuilder().withName(name).build(), workspaceId, stackRequest);
    }

    @Override
    public void changeImage(Long workspaceId, String name, @Valid StackImageChangeV4Request stackImageChangeRequest) {
        stackOperation.changeImage(aStackAccessDtoBuilder().withName(name).build(), workspaceId, stackImageChangeRequest);
    }

    @Override
    public void deleteWithKerberos(Long workspaceId, String name, Boolean withStackDelete) {
        stackOperation.deleteWithKerberos(aStackAccessDtoBuilder().withName(name).build(), workspaceId, withStackDelete);
    }

    @Override
    public StackV4Request getRequestfromName(Long workspaceId, String name) {
        return stackOperation.getRequest(aStackAccessDtoBuilder().withName(name).build(), workspaceId);
    }

    @Override
    public StackStatusV4Response getStatusByName(Long workspaceId, String name) {
        return stackOperation.getStatus(aStackAccessDtoBuilder().withName(name).build(), workspaceId);
    }

    @Override
    public void deleteInstance(Long workspaceId, String name, Boolean forced, String instanceId) {
        stackOperation.deleteInstance(aStackAccessDtoBuilder().withName(name).build(), workspaceId, forced, instanceId);
    }

    @Override
    public void putPassword(Long workspaceId, String name, @Valid UserNamePasswordV4Request userNamePasswordJson) {
        stackOperation.putPassword(aStackAccessDtoBuilder().withName(name).build(), workspaceId, userNamePasswordJson);
    }

    @Override
    public void setClusterMaintenanceMode(Long workspaceId, String name, @NotNull MaintenanceModeV4Request maintenanceMode) {
        stackOperation.setClusterMaintenanceMode(aStackAccessDtoBuilder().withName(name).build(), workspaceId, maintenanceMode);
    }

    @Override
    public void putCluster(Long workspaceId, String name, @Valid UpdateClusterV4Request updateJson) {
        stackOperation.putCluster(aStackAccessDtoBuilder().withName(name).build(), workspaceId, updateJson);
    }

    @Override
    public String getClusterHostsInventory(Long workspaceId, String name) {
        return stackOperation.getClusterHostsInventory(workspaceId, name);
    }
}
