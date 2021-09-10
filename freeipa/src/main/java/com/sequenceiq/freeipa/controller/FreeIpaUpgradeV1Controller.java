package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.UPGRADE_FREEIPA;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import javax.inject.Inject;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.RequestObject;
import com.sequenceiq.authorization.annotation.ResourceCrn;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.FreeIpaUpgradeV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeOptions;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.upgrade.model.FreeIpaUpgradeResponse;
import com.sequenceiq.freeipa.service.upgrade.UpgradeService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
public class FreeIpaUpgradeV1Controller implements FreeIpaUpgradeV1Endpoint {

    @Inject
    private UpgradeService upgradeService;

    @Inject
    private CrnService crnService;

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = UPGRADE_FREEIPA)
    public FreeIpaUpgradeResponse upgradeFreeIpa(@RequestObject FreeIpaUpgradeRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return upgradeService.upgradeFreeIpa(accountId, request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = UPGRADE_FREEIPA)
    public FreeIpaUpgradeOptions getFreeIpaUpgradeOptions(@ResourceCrn String environmentCrn, String catalog) {
        String accountId = crnService.getCurrentAccountId();
        return upgradeService.collectUpgradeOptions(accountId, environmentCrn, catalog);
    }
}
