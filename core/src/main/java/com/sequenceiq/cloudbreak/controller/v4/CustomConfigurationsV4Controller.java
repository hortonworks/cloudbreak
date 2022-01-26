package com.sequenceiq.cloudbreak.controller.v4;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import com.sequenceiq.authorization.annotation.AccountIdNotNeeded;
import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.RoleTypeV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.ServiceTypeV4Response;
import com.sequenceiq.cloudbreak.converter.CustomConfigurationsToCustomConfigurationsV4ResponseConverter;
import com.sequenceiq.cloudbreak.converter.CustomConfigurationsV4RequestToCustomConfigurationsConverter;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.CustomConfigurationsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CloneCustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsService;
import com.sequenceiq.cloudbreak.validation.AllRoleTypes;
import com.sequenceiq.cloudbreak.validation.AllServiceTypes;

@Controller
public class CustomConfigurationsV4Controller implements CustomConfigurationsV4Endpoint {

    @Inject
    private CustomConfigurationsService customConfigurationsService;

    @Inject
    private CustomConfigurationsV4RequestToCustomConfigurationsConverter customConfigsRequestToCustomConfigsConverter;

    @Inject
    private CustomConfigurationsToCustomConfigurationsV4ResponseConverter customConfigsToCustomConfigsResponseConverter;

    @Override
    @DisableCheckPermissions
    public CustomConfigurationsV4Responses list() {
        List<CustomConfigurations> customConfigurationsList = customConfigurationsService.getAll(ThreadBasedUserCrnProvider.getAccountId());
        return new CustomConfigurationsV4Responses(customConfigurationsList
                .stream().map(configs -> customConfigsToCustomConfigsResponseConverter.convert(configs)).collect(Collectors.toList()));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response getByCrn(@ResourceCrn String crn) {
        return customConfigsToCustomConfigsResponseConverter.convert(customConfigurationsService.getByNameOrCrn(NameOrCrn.ofCrn(crn)));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response getByName(@ResourceName String name) {
        return customConfigsToCustomConfigsResponseConverter.convert(customConfigurationsService.getByNameOrCrn(NameOrCrn.ofName(name)));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response post(CustomConfigurationsV4Request request) {
        CustomConfigurations customConfigurations = customConfigsRequestToCustomConfigsConverter.convert(request);
        return customConfigsToCustomConfigsResponseConverter.convert(customConfigurationsService.create(customConfigurations,
                ThreadBasedUserCrnProvider.getAccountId()));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response cloneByName(@ResourceName String name, CloneCustomConfigurationsV4Request request) {
        return customConfigsToCustomConfigsResponseConverter.convert(customConfigurationsService.clone(NameOrCrn.ofName(name),
                request.getName(), request.getRuntimeVersion(), ThreadBasedUserCrnProvider.getAccountId()));
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response cloneByCrn(@ResourceCrn String crn, CloneCustomConfigurationsV4Request request) {
        return customConfigsToCustomConfigsResponseConverter.convert(customConfigurationsService.clone(NameOrCrn.ofCrn(crn),
                request.getName(), request.getRuntimeVersion(), ThreadBasedUserCrnProvider.getAccountId()));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response deleteByCrn(@ResourceCrn String crn) {
        return customConfigsToCustomConfigsResponseConverter.convert(customConfigurationsService.deleteByCrn(crn));
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response deleteByName(@ResourceName String name) {
        return customConfigsToCustomConfigsResponseConverter.convert(customConfigurationsService.deleteByName(name,
                ThreadBasedUserCrnProvider.getAccountId()));
    }

    @Override
    @AccountIdNotNeeded
    @InternalOnly
    public ServiceTypeV4Response getServiceTypes() {
        return new ServiceTypeV4Response(Arrays.stream(AllServiceTypes.values()).map(Enum::toString).collect(Collectors.toList()));
    }

    @Override
    @AccountIdNotNeeded
    @InternalOnly
    public RoleTypeV4Response getRoleTypes() {
        return new RoleTypeV4Response(Arrays.stream(AllRoleTypes.values()).map(Enum::toString).collect(Collectors.toList()));
    }
}
