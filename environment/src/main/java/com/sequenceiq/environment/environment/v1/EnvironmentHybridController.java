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
import com.sequenceiq.environment.api.v1.environment.model.request.CancelCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.FinishSetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.RepairCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SetupCrossRealmTrustRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.SetupCrossRealmTrustResponse;
import com.sequenceiq.environment.credential.v1.converter.EnvironmentHybridConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.CancelCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.FinishSetupCrossRealmTrustResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.crossrealm.RepairCrossRealmTrustResponse;

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
    public SetupCrossRealmTrustResponse setupByName(@ResourceName String environmentName, SetupCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.setupCrossRealmSetup(
                accountId,
                NameOrCrn.ofName(environmentName),
                request);
        return environmentHybridConverter.convertToPrepareCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public SetupCrossRealmTrustResponse setupByCrn(@ResourceCrn String crn, SetupCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.setupCrossRealmSetup(
                accountId,
                NameOrCrn.ofCrn(crn),
                request);
        return environmentHybridConverter.convertToPrepareCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public FinishSetupCrossRealmTrustResponse finishSetupByName(@ResourceName String environmentName, FinishSetupCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.setupFinishCrossRealmSetup(
                accountId,
                NameOrCrn.ofName(environmentName));
        return environmentHybridConverter.convertToFinishCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public FinishSetupCrossRealmTrustResponse finishSetupByCrn(@ResourceCrn String crn, FinishSetupCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.setupFinishCrossRealmSetup(
                accountId,
                NameOrCrn.ofCrn(crn));
        return environmentHybridConverter.convertToFinishCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public CancelCrossRealmTrustResponse cancelByName(@ResourceName String environmentName, CancelCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.cancelCrossRealmSetup(
                accountId,
                NameOrCrn.ofName(environmentName));
        return environmentHybridConverter.convertToCancelCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public CancelCrossRealmTrustResponse cancelByCrn(@ResourceCrn String crn, CancelCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.cancelCrossRealmSetup(
                accountId,
                NameOrCrn.ofCrn(crn));
        return environmentHybridConverter.convertToCancelCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public RepairCrossRealmTrustResponse repairByName(@ResourceName String environmentName, RepairCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.repairCrossRealmSetup(
                accountId,
                NameOrCrn.ofName(environmentName));
        return environmentHybridConverter.convertToRepairCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public RepairCrossRealmTrustResponse repairByCrn(@ResourceCrn String crn, RepairCrossRealmTrustRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.repairCrossRealmSetup(
                accountId,
                NameOrCrn.ofCrn(crn));
        return environmentHybridConverter.convertToRepairCrossRealmTrustResponse(flowIdentifier);
    }
}
