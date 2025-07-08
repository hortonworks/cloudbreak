package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ADMIN_FREEIPA;
import static com.sequenceiq.authorization.resource.AuthorizationVariableType.CRN;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByRequestProperty;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.freeipa.api.v1.freeipa.crossrealm.TrustV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustResponse;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.crossrealm.TrustSetupService;
import com.sequenceiq.freeipa.util.CrnService;

@Controller
@AccountEntityType(Stack.class)
public class TrustV1Controller implements TrustV1Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(TrustV1Controller.class);

    @Inject
    private CrnService crnService;

    @Inject
    private TrustSetupService trustSetupService;

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = ADMIN_FREEIPA)
    public PrepareCrossRealmTrustResponse setup(@RequestObject PrepareCrossRealmTrustRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return trustSetupService.setupTrust(accountId, request);
    }

    @Override
    @CheckPermissionByRequestProperty(path = "environmentCrn", type = CRN, action = ADMIN_FREEIPA)
    public FinishSetupCrossRealmTrustResponse finishSetup(@RequestObject FinishSetupCrossRealmTrustRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return trustSetupService.finishTrustSetup(accountId, request);
    }
}
