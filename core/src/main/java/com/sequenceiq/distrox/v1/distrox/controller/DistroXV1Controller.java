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
import com.sequenceiq.authorization.annotation.AuthorizationResource;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackScaleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.GeneratedBlueprintV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RetryableFlowResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.RetryableFlowResponse.Builder;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackStatusV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackViewV4Responses;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalReady;
import com.sequenceiq.cloudbreak.auth.security.internal.TenantAwareParam;
import com.sequenceiq.cloudbreak.exception.BadRequestException;
import com.sequenceiq.cloudbreak.retry.RetryableFlow;
import com.sequenceiq.cloudbreak.service.workspace.WorkspaceService;
import com.sequenceiq.distrox.api.v1.distrox.endpoint.DistroXV1Endpoint;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMaintenanceModeV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXRepairV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXScaleV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXV1Request;
import com.sequenceiq.distrox.api.v1.distrox.model.cluster.DistroXMultiDeleteV1Request;
import com.sequenceiq.distrox.v1.distrox.StackOperations;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXMaintenanceModeV1ToMainenanceModeV4Converter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXRepairV1RequestToClusterRepairV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXScaleV1RequestToStackScaleV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.DistroXV1RequestToStackV4RequestConverter;
import com.sequenceiq.distrox.v1.distrox.converter.cli.DelegatingRequestToCliRequestConverter;

@Controller
@InternalReady
@AuthorizationResource(type = AuthorizationResourceType.DATAHUB)
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
    private DistroXMaintenanceModeV1ToMainenanceModeV4Converter maintenanceModeConverter;

    @Inject
    private DelegatingRequestToCliRequestConverter delegatingRequestToCliRequestConverter;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public StackViewV4Responses list(String environmentName, String environmentCrn) {
        StackViewV4Responses stackViewV4Responses;
        List<StackType> stackTypes = List.of(StackType.WORKLOAD);
        stackViewV4Responses = Strings.isNullOrEmpty(environmentName)
                ? stackOperations.listByEnvironmentCrn(workspaceService.getForCurrentUser().getId(), environmentCrn, stackTypes)
                : stackOperations.listByEnvironmentName(workspaceService.getForCurrentUser().getId(), environmentName, stackTypes);
        return stackViewV4Responses;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public StackV4Response post(@Valid DistroXV1Request request) {
        return stackOperations.post(
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(request));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public StackV4Response getByName(String name, Set<String> entries) {
        return stackOperations.get(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId(),
                entries,
                StackType.WORKLOAD);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public StackV4Response getByCrn(@TenantAwareParam String crn, Set<String> entries) {
        return stackOperations.get(
                NameOrCrn.ofCrn(crn),
                workspaceService.getForCurrentUser().getId(),
                entries,
                StackType.WORKLOAD);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deleteByName(String name, Boolean forced) {
        stackOperations.delete(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId(), forced);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deleteByCrn(String crn, Boolean forced) {
        stackOperations.delete(NameOrCrn.ofCrn(crn), workspaceService.getForCurrentUser().getId(), forced);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
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
        Set<NameOrCrn> nameOrCrns = multiDeleteRequest.getNames().stream()
                .map(name -> NameOrCrn.ofName(name))
                .collect(Collectors.toSet());
        nameOrCrns.forEach(nameOrCrn -> stackOperations.delete(nameOrCrn, workspaceService.getForCurrentUser().getId(), forced));
    }

    private void multideleteByCrn(DistroXMultiDeleteV1Request multiDeleteRequest, Boolean forced) {
        Set<NameOrCrn> nameOrCrns = multiDeleteRequest.getCrns().stream()
                .map(NameOrCrn::ofCrn)
                .collect(Collectors.toSet());
        nameOrCrns.forEach(accessDto -> stackOperations.delete(accessDto, workspaceService.getForCurrentUser().getId(), forced));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void syncByName(String name) {
        stackOperations.sync(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void syncByCrn(String crn) {
        stackOperations.sync(NameOrCrn.ofCrn(crn), workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void retryByName(String name) {
        stackOperations.retry(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public List<RetryableFlowResponse> listRetryableFlows(String name) {
        List<RetryableFlow> retryableFlows = stackOperations.getRetryableFlows(name, workspaceService.getForCurrentUser().getId());
        return retryableFlows.stream()
                .map(retryable -> Builder.builder().setName(retryable.getName()).setFailDate(retryable.getFailDate()).build())
                .collect(Collectors.toList());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void retryByCrn(String crn) {
        stackOperations.retry(NameOrCrn.ofCrn(crn), workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putStopByName(String name) {
        stackOperations.putStop(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putStopByCrn(String crn) {
        stackOperations.putStop(NameOrCrn.ofCrn(crn), workspaceService.getForCurrentUser().getId());

    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putStopByNames(List<String> names) {
        names.forEach(this::putStopByName);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putStopByCrns(List<String> crns) {
        crns.forEach(this::putStopByCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putStartByName(String name) {
        stackOperations.putStart(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId());

    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putStartByCrn(String crn) {
        stackOperations.putStart(NameOrCrn.ofCrn(crn), workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putStartByNames(List<String> names) {
        names.forEach(this::putStartByName);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putStartByCrns(List<String> crns) {
        crns.forEach(this::putStartByCrn);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putScalingByName(String name, @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperations.getStackByName(name).getId());
        stackOperations.putScaling(NameOrCrn.ofName(name), workspaceService.getForCurrentUser().getId(), stackScaleV4Request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void putScalingByCrn(String crn, @Valid DistroXScaleV1Request updateRequest) {
        StackScaleV4Request stackScaleV4Request = scaleRequestConverter.convert(updateRequest);
        stackScaleV4Request.setStackId(stackOperations.getStackByCrn(crn).getId());
        stackOperations.putScaling(NameOrCrn.ofCrn(crn), workspaceService.getForCurrentUser().getId(), stackScaleV4Request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void repairClusterByName(String name, @Valid DistroXRepairV1Request clusterRepairRequest) {
        stackOperations.repairCluster(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void repairClusterByCrn(String crn, @Valid DistroXRepairV1Request clusterRepairRequest) {
        stackOperations.repairCluster(
                NameOrCrn.ofCrn(crn),
                workspaceService.getForCurrentUser().getId(),
                clusterRepairRequestConverter.convert(clusterRepairRequest));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public GeneratedBlueprintV4Response postStackForBlueprintByName(String name, @Valid DistroXV1Request stackRequest) {
        return stackOperations.postStackForBlueprint(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(stackRequest));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public GeneratedBlueprintV4Response postStackForBlueprintByCrn(String crn, @Valid DistroXV1Request stackRequest) {
        return stackOperations.postStackForBlueprint(
                NameOrCrn.ofCrn(crn),
                workspaceService.getForCurrentUser().getId(),
                stackRequestConverter.convert(stackRequest));
    }

    @Override
    public Object getRequestfromName(String name) {
        StackV4Request stackV4Request = getStackV4Request(NameOrCrn.ofName(name));
        return getCreateAWSClusterRequest(stackV4Request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public Object getRequestfromCrn(String crn) {
        StackV4Request stackV4Request = getStackV4Request(NameOrCrn.ofCrn(crn));
        return getCreateAWSClusterRequest(stackV4Request);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public StackStatusV4Response getStatusByName(String name) {
        return stackOperations.getStatus(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public StackStatusV4Response getStatusByCrn(String crn) {
        return stackOperations.getStatusByCrn(NameOrCrn.ofCrn(crn), workspaceService.getForCurrentUser().getId());
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deleteInstanceByName(String name, Boolean forced, String instanceId) {
        stackOperations.deleteInstance(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId(),
                forced,
                instanceId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deleteInstanceByCrn(String crn, Boolean forced, String instanceId) {
        stackOperations.deleteInstance(
                NameOrCrn.ofCrn(crn),
                workspaceService.getForCurrentUser().getId(),
                forced,
                instanceId);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deleteInstancesByName(String name, @NotEmpty List<String> instances, boolean forced) {
        stackOperations.deleteInstances(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId(),
                instances,
                forced);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deleteInstancesByCrn(String crn, @NotEmpty List<String> instances, boolean forced) {
        stackOperations.deleteInstances(
                NameOrCrn.ofCrn(crn),
                workspaceService.getForCurrentUser().getId(),
                instances,
                forced);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void setClusterMaintenanceModeByName(String name, @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperations.setClusterMaintenanceMode(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId(),
                maintenanceModeConverter.convert(maintenanceMode));

    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void setClusterMaintenanceModeByCrn(String crn, @NotNull DistroXMaintenanceModeV1Request maintenanceMode) {
        stackOperations.setClusterMaintenanceMode(
                NameOrCrn.ofCrn(crn),
                workspaceService.getForCurrentUser().getId(),
                maintenanceModeConverter.convert(maintenanceMode));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deleteWithKerberosByName(String name, boolean forced) {
        stackOperations.delete(
                NameOrCrn.ofName(name),
                workspaceService.getForCurrentUser().getId(),
                forced);

    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.WRITE)
    public void deleteWithKerberosByCrn(String crn, boolean forced) {
        stackOperations.delete(
                NameOrCrn.ofCrn(crn),
                workspaceService.getForCurrentUser().getId(),
                forced);

    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.READ)
    public Object getCreateAwsClusterForCli(DistroXV1Request request) {
        return delegatingRequestToCliRequestConverter.convertDistroX(request);
    }

    private StackV4Request getStackV4Request(NameOrCrn nameOrCrn) {
        return stackOperations.getRequest(nameOrCrn, workspaceService.getForCurrentUser().getId());
    }

    private Object getCreateAWSClusterRequest(StackV4Request stackV4Request) {
        return delegatingRequestToCliRequestConverter.convertStack(stackV4Request);
    }

}
