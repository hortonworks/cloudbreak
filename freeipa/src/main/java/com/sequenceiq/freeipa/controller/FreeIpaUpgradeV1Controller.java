package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.EDIT_ENVIRONMENT;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.FreeIpaUpgradeV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeOptions;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeResponse;

@Controller
public class FreeIpaUpgradeV1Controller implements FreeIpaUpgradeV1Endpoint {
    //TODO introduce freeipa upgrade permission
    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = EDIT_ENVIRONMENT)
    public FreeIpaUpgradeResponse upgradeFreeIpa(@RequestObject FreeIpaUpgradeRequest request) {
        return null;
    }

    @Override
    @CheckPermissionByResourceCrn(action = EDIT_ENVIRONMENT)
    public FreeIpaUpgradeOptions getFreeIpaUpgradeOptions(@ResourceCrn String environmentCrn, String catalog) {
        return null;
    }
}
