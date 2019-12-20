package com.sequenceiq.distrox.v1.distrox.controller;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Controller;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.dto.StackAccessDto;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RetryableFlowResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RetryableFlowResponse.Builder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.retry.RetryableFlow;
import com.sequenceiq.cloudbreak.service.environment.EnvironmentClientService;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMaintenanceModeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.EmptyResponse;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXImageChangeV1RequestToStackImageChangeV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXMaintenanceModeV1ToMainenanceModeV4Converter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXRepairV1RequestToClusterRepairV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXScaleV1RequestToStackScaleV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToCreateAWSClusterRequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.StackRequestToCreateAWSClusterRequestConverter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;

@Controller
@InternalReady
public class DistroXV1Controller implements DistroXV1Endpoint {

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
    private DistroXRepairV1RequestToClusterRepairV4RequestConverter clusterRepairRequestConverter;

    @Inject
    private DistroXImageChangeV1RequestToStackImageChangeV4RequestConverter imageChangeRequestConverter;

    @Inject
    private DistroXMaintenanceModeV1ToMainenanceModeV4Converter maintenanceModeConverter;

    @Inject
    private DistroXV1RequestToCreateAWSClusterRequestConverter distroXV1RequestToCreateAWSClusterRequestConverter;

    @Inject
    private StackRequestToCreateAWSClusterRequestConverter stackRequestToCreateAWSClusterRequestConverter;

    @Inject
    private EnvironmentClientService environmentClientService;

    @Override
    public StackViewV4Responses list(String environmentName, String environmentCrn) {
        StackViewV4Responses stackViewV4Responses;
        List<StackType> stackTypes = List.of(StackType.WORKLOAD);
        if (!Strings.isNullOrEmpty(environmentName)) {
            stackViewV4Responses = stackOperations.listByEnvironmentName(workspaceService.getForCurrentUser().getId(), environmentName, stackTypes);
        } else {
            stackViewV4Responses = stackOperations.listByEnvironmentCrn(workspaceService.getForCurrentUser().getId(), environmentCrn, stackTypes);
        }
        return stackViewV4Responses;
    }

    @Override
    public StackV4Response post(@Valid DistroXV1Request request) {
        return stackOperations.post(
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(request));
    }

    @Override
    public StackV4Response getByName(String name, Set<String> entries) {
        return stackOperations.get(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                entries,
                StackType.WORKLOAD);
    }

    @Override
    public StackV4Response getByCrn(@ResourceCrn String crn, Set<String> entries) {
        return stackOperations.get(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                entries,
                StackType.WORKLOAD);
    }

    @Override
    public void deleteByName(String name, Boolean forced) {
        stackOperations.delete(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                forced);
    }

    @Override
    public void deleteByCrn(String crn, Boolean forced) {
        stackOperations.delete(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                forced);
    }

    @Override
    public void deleteMultiple(DistroXMultiDeleteV1Request multiDeleteRequest, Boolean forced) {
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
        Set<StackAccessDto> stackAccessDtos = multiDeleteRequest.getNames().stream()
                .map(name -> StackAccessDto.builder().withName(name).build())
                .collect(Collectors.toSet());
        stackAccessDtos
                .forEach(stackAccessDto -> stackOperations.delete(stackAccessDto, workspaceService.getForCurrentUser().getId(), forced));
    }

    private void multideleteByCrn(DistroXMultiDeleteV1Request multiDeleteRequest, Boolean forced) {
        Set<StackAccessDto> stackAccessDtos = multiDeleteRequest.getCrns().stream()
                .map(crn -> StackAccessDto.builder().withCrn(crn).build())
                .collect(Collectors.toSet());
        stackAccessDtos
                .forEach(stackAccessDto -> stackOperations.delete(stackAccessDto, workspaceService.getForCurrentUser().getId(), forced));
    }

    @Override
    public void syncByName(String name) {
        stackOperations.sync(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    public void syncByCrn(String crn) {
        stackOperations.sync(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    public void retryByName(String name) {
        stackOperations.retry(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public List<RetryableFlowResponse> listRetryableFlows(String name) {
        List<RetryableFlow> retryableFlows = stackOperations.getRetryableFlows(name, workspaceService.getForCurrentUser().getId());
        return retryableFlows.stream()
                .map(retryable -> Builder.builder().setName(retryable.getName()).setFailDate(retryable.getFailDate()).build())
                .collect(Collectors.toList());
    }

    @Override
    public void retryByCrn(String crn) {
        stackOperations.retry(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putStopByName(String name) {
        stackOperations.putStop(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putStopByCrn(String crn) {
        stackOperations.putStop(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putStopByNames(List<String> names) {
        names.forEach(this::putStopByName);
    }

    @Override
    public void putStopByCrns(List<String> crns) {
        crns.forEach(this::putStopByCrn);
    }

    @Override
    public void putStartByName(String name) {
        stackOperations.putStart(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putStartByCrn(String crn) {
        stackOperations.putStart(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());

    }

    @Override
    public void putStartByNames(List<String> names) {
        names.forEach(this::putStartByName);
    }

    @Override
    public void putStartByCrns(List<String> crns) {
        crns.forEach(this::putStartByCrn);
    }

    @Override
    public void putScalingByName(String name, @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperations.getStackByName(name).getId());
        stackOperations.putScaling(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                stackScaleV4Request);
    }

    @Override
    public void putScalingByCrn(String crn, @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperations.getStackByCrn(crn).getId());
        stackOperations.putScaling(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                stackScaleV4Request);
    }

    @Override
    public void repairClusterByName(String name, @Valid DistroXRepairV1Request clusterRepairRequest) {
        stackOperations.repairCluster(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));

    }

    @Override
    public void repairClusterByCrn(String crn, @Valid DistroXRepairV1Request clusterRepairRequest) {
        stackOperations.repairCluster(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));

    }

    @Override
    public GeneratedBlueprintV4Response postStackForBlueprintByName(String name, @Valid DistroXV1Request stackRequest) {
        return stackOperations.postStackForBlueprint(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(stackRequest));
    }

    @Override
    public GeneratedBlueprintV4Response postStackForBlueprintByCrn(String crn, @Valid DistroXV1Request stackRequest) {
        return stackOperations.postStackForBlueprint(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(stackRequest));
    }

    @Override
    public Object getRequestfromName(String name) {
        StackV4Request stackV4Request = getStackV4Request(StackAccessDto.builder().withName(name).build());
        return getCreateAWSClusterRequest(stackV4Request);
    }

    @Override
    public Object getRequestfromCrn(String crn) {
        StackV4Request stackV4Request = getStackV4Request(StackAccessDto.builder().withCrn(crn).build());
        return getCreateAWSClusterRequest(stackV4Request);
    }

    @Override
    public StackStatusV4Response getStatusByName(String name) {
        return stackOperations.getStatus(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    public StackStatusV4Response getStatusByCrn(String crn) {
        return stackOperations.getStatusByCrn(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    public void deleteInstanceByName(String name, Boolean forced, String instanceId) {
        stackOperations.deleteInstance(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                forced,
                instanceId);
    }

    @Override
    public void deleteInstanceByCrn(String crn, Boolean forced, String instanceId) {
        stackOperations.deleteInstance(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                forced,
                instanceId);
    }

    @Override
    public void deleteInstancesByName(String name, @NotEmpty List<String> instances, boolean forced) {
        stackOperations.deleteInstances(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                instances,
                forced);
    }

    @Override
    public void deleteInstancesByCrn(String crn, @NotEmpty List<String> instances, boolean forced) {
        stackOperations.deleteInstances(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                instances,
                forced);
    }

    @Override
    public void setClusterMaintenanceModeByName(String name, @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperations.setClusterMaintenanceMode(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                maintenanceModeConverter.convert(maintenanceMode));

    }

    @Override
    public void setClusterMaintenanceModeByCrn(String crn, @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperations.setClusterMaintenanceMode(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                maintenanceModeConverter.convert(maintenanceMode));
    }

    @Override
    public void deleteWithKerberosByName(String name, boolean forced) {
        stackOperations.delete(
                StackAccessDto.builder().withName(name).build(),
                workspaceService.getForCurrentUser().getId(),
                forced);

    }

    @Override
    public void deleteWithKerberosByCrn(String crn, boolean forced) {
        stackOperations.delete(
                StackAccessDto.builder().withCrn(crn).build(),
                workspaceService.getForCurrentUser().getId(),
                forced);

    }

    @Override
    public Object getCreateAwsClusterForCli(DistroXV1Request request) {
        DetailedEnvironmentResponse env = environmentClientService.getByName(request.getEnvironmentName());
        if (!CloudPlatform.AWS.name().equals(env.getCloudPlatform())) {
            return new EmptyResponse();
        }
        return distroXV1RequestToCreateAWSClusterRequestConverter.convert(request);
    }

    private StackV4Request getStackV4Request(StackAccessDto stackAccessDto) {
        return stackOperations.getRequest(stackAccessDto, workspaceService.getForCurrentUser().getId());
    }

    private Object getCreateAWSClusterRequest(StackV4Request stackV4Request) {
        DetailedEnvironmentResponse env = environmentClientService.getByCrn(stackV4Request.getEnvironmentCrn());
        if (!CloudPlatform.AWS.name().equals(env.getCloudPlatform())) {
            return new EmptyResponse();
        }
        return stackRequestToCreateAWSClusterRequestConverter.convert(stackV4Request);
    }
}
