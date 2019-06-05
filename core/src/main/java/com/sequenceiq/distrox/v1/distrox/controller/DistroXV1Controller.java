package com.sequenceiq.distrox.v1.distrox.controller;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMaintenanceModeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperation;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXImageChangeV1RequestToStackImageChangeV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXMaintenanceModeV1ToMainenanceModeV4Converter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXRepairV1RequestToClusterRepairV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXScaleV1RequestToStackScaleV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;

@Controller
public class DistroXV1Controller implements DistroXV1Endpoint {

    @Inject
    private StackOperation stackOperation;

    @Inject
    private WorkspaceService workspaceService;

    @Inject
    private DistroXV1RequestToStackV4RequestConverter stackRequestConverter;

    @Inject
    private DistroXScaleV1RequestToStackScaleV4RequestConverter scaleRequestConverter;

    @Inject
    private DistroXRepairV1RequestToClusterRepairV4RequestConverter clusterRepairRequestConverter;

    @Inject
    private DistroXImageChangeV1RequestToStackImageChangeV4RequestConverter imageChangeRequestConverter;

    @Inject
    private DistroXMaintenanceModeV1ToMainenanceModeV4Converter maintenanceModeConverter;

    @Override
    public StackViewV4Responses list(String environment, Boolean onlyDatalakes) {
        return stackOperation.list(workspaceService.getForCurrentUser().getId(), environment, onlyDatalakes);
    }

    @Override
    public StackV4Response post(@Valid DistroXV1Request request) {
        return stackOperation.post(workspaceService.getForCurrentUser().getId(), stackRequestConverter.convert(request));
    }

    @Override
    public StackV4Response get(String name, Set<String> entries) {
        return stackOperation.get(workspaceService.getForCurrentUser().getId(), name, entries);
    }

    @Override
    public void delete(String name, Boolean forced) {
        stackOperation.delete(workspaceService.getForCurrentUser().getId(), name, forced, false);
    }

    @Override
    public void putSync(String name) {
        stackOperation.putSync(workspaceService.getForCurrentUser().getId(), name);
    }

    @Override
    public void putRetry(String name) {
        stackOperation.putRetry(workspaceService.getForCurrentUser().getId(), name);
    }

    @Override
    public void putStop(String name) {
        stackOperation.putStop(workspaceService.getForCurrentUser().getId(), name);
    }

    @Override
    public void putStart(String name) {
        stackOperation.putStart(workspaceService.getForCurrentUser().getId(), name);
    }

    @Override
    public void putScaling(String name, @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperation.getStackByName(name).getId());
        stackOperation.putScaling(workspaceService.getForCurrentUser().getId(), name, stackScaleV4Request);
    }

    @Override
    public void repairCluster(String name, @Valid DistroXRepairV1Request clusterRepairRequest) {
        stackOperation.repairCluster(workspaceService.getForCurrentUser().getId(), name, clusterRepairRequestConverter.convert(clusterRepairRequest));
    }

    @Override
    public GeneratedBlueprintV4Response postStackForBlueprint(String name, @Valid DistroXV1Request stackRequest) {
        return stackOperation.postStackForBlueprint(workspaceService.getForCurrentUser().getId(), name, stackRequestConverter.convert(stackRequest));
    }

    @Override
    public DistroXV1Request getRequestfromName(String name) {
        StackV4Request stackV4Request = stackOperation.getRequestfromName(workspaceService.getForCurrentUser().getId(), name);
        return stackRequestConverter.convert(stackV4Request);
    }

    @Override
    public StackStatusV4Response getStatusByName(String name) {
        return stackOperation.getStatusByName(workspaceService.getForCurrentUser().getId(), name);
    }

    @Override
    public void deleteInstance(String name, Boolean forced, String instanceId) {
        stackOperation.deleteInstance(workspaceService.getForCurrentUser().getId(), name, forced, instanceId);
    }

    @Override
    public void setClusterMaintenanceMode(String name, @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperation.setClusterMaintenanceMode(workspaceService.getForCurrentUser().getId(), name, maintenanceModeConverter.convert(maintenanceMode));
    }

    @Override
    public void deleteWithKerberos(String name) {
        stackOperation.deleteWithKerberos(workspaceService.getForCurrentUser().getId(), name, true, false);
    }
}
