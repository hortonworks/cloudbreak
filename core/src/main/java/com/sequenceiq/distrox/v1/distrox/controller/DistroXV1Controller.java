package com.sequenceiq.distrox.v1.distrox.controller;

import static com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.dto.StackAccessDto.StackAccessDtoBuilder.aStackAccessDtoBuilder;

import java.util.Set;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.stereotype.Controller;

import com.google.common.base.Strings;
import com.sequenceiq.authorization.annotation.CheckPermissionByEnvironmentName;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.EnvironmentName;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.authorization.resource.ResourceType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
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
@AuthorizationResource(type = ResourceType.DATAHUB)
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
    @DisableCheckPermissions
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
    @CheckPermissionByEnvironmentName(action = ResourceAction.WRITE)
    public StackV4Response post(@EnvironmentName @Valid DistroXV1Request request) {
        return stackOperation.post(
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(request));
    }

    @Override
    @DisableCheckPermissions
    public StackV4Response getByName(String name, Set<String> entries) {
        return stackOperation.get(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                entries,
                StackType.WORKLOAD);
    }

    @Override
    @DisableCheckPermissions
    public StackV4Response getByCrn(String crn, Set<String> entries) {
        return stackOperation.get(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                entries,
                StackType.WORKLOAD);
    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void deleteByName(@ResourceName String name, Boolean forced) {
        stackOperation.delete(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                forced);
    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void deleteByCrn(@ResourceCrn String crn, Boolean forced) {
        stackOperation.delete(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                forced);
    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void syncByName(@ResourceName String name) {
        stackOperation.sync(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void syncByCrn(@ResourceCrn String crn) {
        stackOperation.sync(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void retryByName(@ResourceName String name) {
        stackOperation.retry(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void retryByCrn(@ResourceCrn String crn) {
        stackOperation.retry(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void putStopByName(@ResourceName String name) {
        stackOperation.putStop(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void putStopByCrn(@ResourceCrn String crn) {
        stackOperation.putStop(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void putStartByName(@ResourceName String name) {
        stackOperation.putStart(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void putStartByCrn(@ResourceCrn String crn) {
        stackOperation.putStart(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void putScalingByName(@ResourceName String name, @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperation.getStackByName(name).getId());
        stackOperation.putScaling(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                stackScaleV4Request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void putScalingByCrn(@ResourceCrn String crn, @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperation.getStackByCrn(crn).getId());
        stackOperation.putScaling(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                stackScaleV4Request);
    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void repairClusterByName(@ResourceName String name, @Valid DistroXRepairV1Request clusterRepairRequest) {
        stackOperation.repairCluster(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));

    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void repairClusterByCrn(@ResourceCrn String crn, @Valid DistroXRepairV1Request clusterRepairRequest) {
        stackOperation.repairCluster(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));

    }

    @Override
    @CheckPermissionByEnvironmentName
    public GeneratedBlueprintV4Response postStackForBlueprintByName(String name, @EnvironmentName @Valid DistroXV1Request stackRequest) {
        return stackOperation.postStackForBlueprint(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(stackRequest));
    }

    @Override
    @CheckPermissionByEnvironmentName
    public GeneratedBlueprintV4Response postStackForBlueprintByCrn(String crn, @EnvironmentName @Valid DistroXV1Request stackRequest) {
        return stackOperation.postStackForBlueprint(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(stackRequest));
    }

    @Override
    @DisableCheckPermissions
    public DistroXV1Request getRequestfromName(String name) {
        StackV4Request stackV4Request = stackOperation.getRequest(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());
        return stackRequestConverter.convert(stackV4Request);
    }

    @Override
    @DisableCheckPermissions
    public DistroXV1Request getRequestfromCrn(String crn) {
        StackV4Request stackV4Request = stackOperation.getRequest(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());
        return stackRequestConverter.convert(stackV4Request);
    }

    @Override
    @DisableCheckPermissions
    public StackStatusV4Response getStatusByName(String name) {
        return stackOperation.getStatus(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    @DisableCheckPermissions
    public StackStatusV4Response getStatusByCrn(String crn) {
        return stackOperation.getStatusByCrn(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void deleteInstanceByName(@ResourceName String name, Boolean forced, String instanceId) {
        stackOperation.deleteInstance(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                forced,
                instanceId);
    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void deleteInstanceByCrn(@ResourceCrn String crn, Boolean forced, String instanceId) {
        stackOperation.deleteInstance(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                forced,
                instanceId);
    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void setClusterMaintenanceModeByName(@ResourceName String name, @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperation.setClusterMaintenanceMode(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                maintenanceModeConverter.convert(maintenanceMode));

    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void setClusterMaintenanceModeByCrn(@ResourceCrn String crn, @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperation.setClusterMaintenanceMode(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                maintenanceModeConverter.convert(maintenanceMode));

    }

    @Override
    @CheckPermissionByResourceName(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void deleteWithKerberosByName(@ResourceName String name) {
        stackOperation.deleteWithKerberos(
                aStackAccessDtoBuilder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                true);

    }

    @Override
    @CheckPermissionByResourceCrn(action = ResourceAction.WRITE, relatedResourceClass = Stack.class)
    public void deleteWithKerberosByCrn(@ResourceCrn String crn) {
        stackOperation.deleteWithKerberos(
                aStackAccessDtoBuilder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                true);

    }
}
