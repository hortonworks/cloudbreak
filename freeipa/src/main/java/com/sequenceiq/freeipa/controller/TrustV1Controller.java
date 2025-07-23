package com.sequenceiq.freeipa.controller;

import static com.sequenceiq.authorization.resource.AuthorizationResourceAction.ADMIN_FREEIPA;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.cloudbreak.auth.security.internal.RequestObject;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.freeipa.api.v1.freeipa.crossrealm.TrustV1Endpoint;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.PrepareCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.commands.TrustSetupCommandsResponse;
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
    @InternalOnly
    public PrepareCrossRealmTrustResponse setup(@RequestObject PrepareCrossRealmTrustRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return trustSetupService.setupTrust(accountId, request);
    }

    @Override
    @InternalOnly
    public FinishSetupCrossRealmTrustResponse finishSetup(@RequestObject FinishSetupCrossRealmTrustRequest request) {
        String accountId = crnService.getCurrentAccountId();
        return trustSetupService.finishTrustSetup(accountId, request);
    }

    @Override
    @CheckPermissionByResourceCrn(action = ADMIN_FREEIPA)
    public TrustSetupCommandsResponse getTrustSetupCommands(@ResourceCrn String environmentCrn) {
        String accountId = crnService.getCurrentAccountId();
        return trustSetupService.getTrustSetupCommands(accountId, environmentCrn);
    }
}
