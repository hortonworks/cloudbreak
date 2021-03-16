package com.sequenceiq.authorization.controller;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.info.AuthorizationUtilEndpoint;
import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Request;
import com.sequenceiq.authorization.info.model.CheckResourceRightsV4Response;
import com.sequenceiq.authorization.info.model.CheckRightOnResourcesV4Request;
import com.sequenceiq.authorization.info.model.CheckRightOnResourcesV4Response;
import com.sequenceiq.authorization.info.model.CheckRightV4Request;
import com.sequenceiq.authorization.info.model.CheckRightV4Response;
import com.sequenceiq.authorization.service.UtilAuthorizationService;

@Controller
public class AuthorizationUtilController implements AuthorizationUtilEndpoint {

    @Inject
    private UtilAuthorizationService utilAuthorizationService;

    @Override
    @DisableCheckPermissions
    public CheckRightV4Response checkRightInAccount(CheckRightV4Request checkRightV4Request) {
        return utilAuthorizationService.checkRights(checkRightV4Request);
    }

    @Override
    @DisableCheckPermissions
    public CheckResourceRightsV4Response checkRightByCrn(CheckResourceRightsV4Request checkResourceRightsV4Request) {
        return utilAuthorizationService.checkRightsOnResources(checkResourceRightsV4Request);
    }

    @Override
    @DisableCheckPermissions
    public CheckRightOnResourcesV4Response checkRightOnResources(CheckRightOnResourcesV4Request checkRightOnResourcesV4Request) {
        return utilAuthorizationService.checkRightOnResources(checkRightOnResourcesV4Request);
    }
}
