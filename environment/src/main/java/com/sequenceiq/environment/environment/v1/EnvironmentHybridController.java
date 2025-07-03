package com.sequenceiq.environment.environment.v1;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByResourceCrn;
import com.sequenceiq.authorization.annotation.CheckPermissionByResourceName;
import com.sequenceiq.authorization.annotation.ResourceName;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.api.endpoint.v4.dto.NameOrCrn;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.ResourceCrn;
import com.sequenceiq.cloudbreak.structuredevent.rest.annotation.AccountEntityType;
import com.sequenceiq.environment.api.v1.environment.endpoint.EnvironmentHybridEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentSetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentSetupCrossRealmTrustResponse;
import com.sequenceiq.environment.credential.v1.converter.EnvironmentHybridConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishCrossRealmTrustRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishCrossRealmTrustResponse;

@Controller
@Transactional(TxType.NEVER)
@AccountEntityType(Environment.class)
public class EnvironmentHybridController implements EnvironmentHybridEndpoint {

    private final EnvironmentModificationService environmentModificationService;

    private final EnvironmentHybridConverter environmentHybridConverter;

    public EnvironmentHybridController(
            EnvironmentModificationService environmentModificationService,
            EnvironmentHybridConverter environmentHybridConverter) {
        this.environmentModificationService = environmentModificationService;
        this.environmentHybridConverter = environmentHybridConverter;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public EnvironmentSetupCrossRealmTrustResponse setupByName(@ResourceName String environmentName, EnvironmentSetupCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.setupCrossRealmSetup(
                accountId,
                NameOrCrn.ofName(environmentName),
                request);
        return environmentHybridConverter.convertToPrepareCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public EnvironmentSetupCrossRealmTrustResponse setupByCrn(@ResourceCrn String crn, EnvironmentSetupCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.setupCrossRealmSetup(
                accountId,
                NameOrCrn.ofCrn(crn),
                request);
        return environmentHybridConverter.convertToPrepareCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public FinishCrossRealmTrustResponse finishSetupByName(@ResourceName String environmentName, FinishCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.setupFinishCrossRealmSetup(
                accountId,
                NameOrCrn.ofName(environmentName));
        return environmentHybridConverter.convertToFinishCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public FinishCrossRealmTrustResponse finishSetupByCrn(@ResourceCrn String crn, FinishCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.setupFinishCrossRealmSetup(
                accountId,
                NameOrCrn.ofCrn(crn));
        return environmentHybridConverter.convertToFinishCrossRealmTrustResponse(flowIdentifier);
    }
}
