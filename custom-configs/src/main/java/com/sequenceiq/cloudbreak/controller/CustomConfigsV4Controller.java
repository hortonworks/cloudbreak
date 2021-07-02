package com.sequenceiq.cloudbreak.controller;

import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.CustomConfigsV4Endpoint;
import com.sequenceiq.cloudbreak.api.endpoint.requests.CloneCustomConfigsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.requests.CustomConfigsV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.responses.CustomConfigsV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.responses.CustomConfigsV4Responses;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.api.util.ConverterUtil;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.domain.CustomConfigs;
import com.sequenceiq.cloudbreak.service.CustomConfigsService;

@Controller
public class CustomConfigsV4Controller implements CustomConfigsV4Endpoint {

    @Inject
    private CustomConfigsService customConfigsService;

    @Inject
    private ConverterUtil converterUtil;

    @Override
    @DisableCheckPermissions
    public CustomConfigsV4Responses list() {
        List<CustomConfigs> customConfigsList = customConfigsService.getAll();
        return new CustomConfigsV4Responses(converterUtil.convertAll(customConfigsList, CustomConfigsV4Response.class));
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
//    @DisableCheckPermissions
    public CustomConfigsV4Response getByCrn(@ResourceCrn  String crn) {
        return converterUtil.convert(customConfigsService.getByCrn(crn), CustomConfigsV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DESCRIBE_CLUSTER_TEMPLATE)
//    @DisableCheckPermissions
    public CustomConfigsV4Response getByName(@ResourceName String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return converterUtil.convert(customConfigsService.getByName(name, accountId), CustomConfigsV4Response.class);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_CLUSTER_TEMPLATE)
//    @DisableCheckPermissions
    public CustomConfigsV4Response post(CustomConfigsV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        CustomConfigs customConfigs = converterUtil.convert(request, CustomConfigs.class);

        return converterUtil.convert(customConfigsService.create(customConfigs, accountId), CustomConfigsV4Response.class);
    }

    @Override
//    @DisableCheckPermissions
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.CREATE_CLUSTER_TEMPLATE)
    public CustomConfigsV4Response cloneByName(@ResourceName String name, CloneCustomConfigsV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return converterUtil.convert(customConfigsService.clone(NameOrCrn.ofName(name),
                request.getCustomConfigsName(), accountId), CustomConfigsV4Response.class);
    }

    @Override
//    @DisableCheckPermissions
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.CREATE_CLUSTER_TEMPLATE)
    public CustomConfigsV4Response cloneByCrn(@ResourceCrn String crn, CloneCustomConfigsV4Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return converterUtil.convert(customConfigsService.clone(NameOrCrn.ofCrn(crn),
                request.getCustomConfigsName(), accountId), CustomConfigsV4Response.class);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.DELETE_CLUSTER_TEMPLATE)
//    @DisableCheckPermissions
    public CustomConfigsV4Response deleteByCrn(@ResourceCrn String crn) {
        return converterUtil.convert(customConfigsService.deleteByCrn(crn), CustomConfigsV4Response.class);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.DELETE_CLUSTER_TEMPLATE)
//    @DisableCheckPermissions
    public CustomConfigsV4Response deleteByName(@ResourceName String name) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return converterUtil.convert(customConfigsService.deleteByName(name, accountId), CustomConfigsV4Response.class);
    }

    @Override
    @DisableCheckPermissions
    public CustomConfigsV4Responses deleteMultiple(Set<String> names) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return new CustomConfigsV4Responses(converterUtil.convertAll(customConfigsService.deleteMultiple(names, accountId), CustomConfigsV4Response.class));
    }
}
