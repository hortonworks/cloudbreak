package com.sequenceiq.cloudbreak.controller.v4;

import java.util.List;

import javax.inject.Inject;


import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import org.springframework.stereotype.Controller;

import com.sequenceiq.cloudbreak.api.endpoint.v4.CustomConfigurationsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CloneCustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.requests.CustomConfigurationsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.responses.CustomConfigurationsV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.CustomConfigurations;
import com.sequenceiq.cloudbreak.service.customconfigs.CustomConfigurationsService;

@Controller
public class CustomConfigurationsV4Controller implements CustomConfigurationsV4Endpoint {

    @Inject
    private CustomConfigurationsService customConfigurationsService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    @DisableCheckPermissions
    public CustomConfigurationsV4Responses list() {
        List<CustomConfigurations> customConfigurationsList = customConfigurationsService.getAll(ThreadBasedUserCrnProvider.getAccountId());
        return new CustomConfigurationsV4Responses(converterUtil.convertAll(customConfigurationsList, CustomConfigurationsV4Response.class));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response getByCrn(@ResourceCrn String crn) {
        return converterUtil.convert(customConfigurationsService.getByNameOrCrn(NameOrCrn.ofCrn(crn)), CustomConfigurationsV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response getByName(@ResourceName String name) {
        return converterUtil.convert(customConfigurationsService.getByNameOrCrn(NameOrCrn.ofName(name)), CustomConfigurationsV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response post(CustomConfigurationsV4Request request) {
        CustomConfigurations customConfigurations = converterUtil.convert(request, CustomConfigurations.class);
        return converterUtil.convert(customConfigurationsService.create(customConfigurations, ThreadBasedUserCrnProvider.getAccountId()),
                CustomConfigurationsV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response cloneByName(@ResourceName String name, CloneCustomConfigurationsV4Request request) {
        return converterUtil.convert(customConfigurationsService.clone(NameOrCrn.ofName(name),
                request.getName(), request.getRuntimeVersion(), ThreadBasedUserCrnProvider.getAccountId()), CustomConfigurationsV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response cloneByCrn(@ResourceCrn String crn, CloneCustomConfigurationsV4Request request) {
        return converterUtil.convert(customConfigurationsService.clone(NameOrCrn.ofCrn(crn),
                request.getName(), request.getRuntimeVersion(), ThreadBasedUserCrnProvider.getAccountId()), CustomConfigurationsV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response deleteByCrn(@ResourceCrn String crn) {
        return converterUtil.convert(customConfigurationsService.deleteByCrn(crn), CustomConfigurationsV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_CUSTOM_CONFIGS)
    public CustomConfigurationsV4Response deleteByName(@ResourceName String name) {
        return converterUtil.convert(customConfigurationsService.deleteByName(name, ThreadBasedUserCrnProvider.getAccountId()),
                CustomConfigurationsV4Response.class);
    }
}
