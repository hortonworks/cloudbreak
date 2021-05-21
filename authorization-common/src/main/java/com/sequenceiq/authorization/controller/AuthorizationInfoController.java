package com.sequenceiq.authorization.controller;

import java.util.Set;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.info.AuthorizationInfoEndpoint;
import com.sequenceiq.authorization.info.model.ApiAuthorizationInfo;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.authorization.service.info.ApiPermissionInfoGenerator;

@Controller
public class AuthorizationInfoController implements AuthorizationInfoEndpoint {

    @Inject
    private ApiPermissionInfoGenerator apiPermissionInfoGenerator;

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public Set<ApiAuthorizationInfo> info() {
        return apiPermissionInfoGenerator.generateApiMethodsWithRequiredPermission();
    }
}
