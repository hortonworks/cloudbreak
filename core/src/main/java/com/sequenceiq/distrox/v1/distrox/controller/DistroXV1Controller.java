package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.dto.StackAccessDto.StackAccessDtoBuilder.aStackAccessDtoBuilder;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
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
    public StackViewV4Responses list(String environmentName, String environmentCrn) {
        StackViewV4Responses stackViewV4Responses;
        if (!Strings.isNullOrEmpty(environmentName)) {
            stackViewV4Responses =  stackOperation.listByEnvironmentName(
                    workspaceService.getForCurrentUser().getId(), environmentName, StackType.WORKLOAD);
        } else {
            stackViewV4Responses = stackOperation.listByEnvironmentCrn(
                    workspaceService.getForCurrentUser().getId(), environmentCrn, StackType.WORKLOAD);
        }
        return stackViewV4Responses;
    }

    @Override
    public StackV4Response post(@Valid DistroXV1Request request) {
        return stackOperation.post(
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(request));
    }

    @Override
    public StackV4Response getByName(String name, Set<String> entries) {
        return stackOperation.get(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                entries,
                StackType.WORKLOAD);
    }

    @Override
    public StackV4Response getByCrn(String crn, Set<String> entries) {
        return stackOperation.get(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                entries,
                StackType.WORKLOAD);
    }

    @Override
    public void deleteByName(String name, Boolean forced) {
        stackOperation.delete(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                forced,
                false);
    }

    @Override
    public void deleteByCrn(String crn, Boolean forced) {
        stackOperation.delete(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                forced,
                false);
    }

    @Override
    public void putSyncByName(String name) {
        stackOperation.putSync(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    public void putSyncByCrn(String crn) {
        stackOperation.putSync(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    public void putRetryByName(String name) {
        stackOperation.putRetry(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putRetryByCrn(String crn) {
        stackOperation.putRetry(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putStopByName(String name) {
        stackOperation.putStop(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putStopByCrn(String crn) {
        stackOperation.putStop(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putStartByName(String name) {
        stackOperation.putStart(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putStartByCrn(String crn) {
        stackOperation.putStart(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putScalingByName(String name, @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperation.getStackByName(name).getId());
        stackOperation.putScaling(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                stackScaleV4Request);
    }

    @Override
    public void putScalingByCrn(String crn, @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperation.getStackByCrn(crn).getId());
        stackOperation.putScaling(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                stackScaleV4Request);
    }

    @Override
    public void repairClusterByName(String name, @Valid DistroXRepairV1Request clusterRepairRequest) {
        stackOperation.repairCluster(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));

    }

    @Override
    public void repairClusterByCrn(String crn, @Valid DistroXRepairV1Request clusterRepairRequest) {
        stackOperation.repairCluster(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));

    }

    @Override
    public GeneratedBlueprintV4Response postStackForBlueprintByName(String name, @Valid DistroXV1Request stackRequest) {
        return stackOperation.postStackForBlueprint(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(stackRequest));
    }

    @Override
    public GeneratedBlueprintV4Response postStackForBlueprintByCrn(String crn, @Valid DistroXV1Request stackRequest) {
        return stackOperation.postStackForBlueprint(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(stackRequest));
    }

    @Override
    public DistroXV1Request getRequestfromName(String name) {
        StackV4Request stackV4Request = stackOperation.getRequest(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());
        return stackRequestConverter.convert(stackV4Request);
    }

    @Override
    public DistroXV1Request getRequestfromCrn(String crn) {
        StackV4Request stackV4Request = stackOperation.getRequest(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());
        return stackRequestConverter.convert(stackV4Request);
    }

    @Override
    public StackStatusV4Response getStatusByName(String name) {
        return stackOperation.getStatus(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    public StackStatusV4Response getStatusByCrn(String crn) {
        return stackOperation.getStatusByCrn(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    public void deleteInstanceByName(String name, Boolean forced, String instanceId) {
        stackOperation.deleteInstance(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                forced,
                instanceId);
    }

    @Override
    public void deleteInstanceByCrn(String crn, Boolean forced, String instanceId) {
        stackOperation.deleteInstance(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                forced,
                instanceId);
    }

    @Override
    public void setClusterMaintenanceModeByName(String name, @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperation.setClusterMaintenanceMode(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                maintenanceModeConverter.convert(maintenanceMode));

    }

    @Override
    public void setClusterMaintenanceModeByCrn(String crn, @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperation.setClusterMaintenanceMode(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                maintenanceModeConverter.convert(maintenanceMode));

    }

    @Override
    public void deleteWithKerberosByName(String name) {
        stackOperation.deleteWithKerberos(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                true,
                false);

    }

    @Override
    public void deleteWithKerberosByCrn(String crn) {
        stackOperation.deleteWithKerberos(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                true,
                false);

    }
}
