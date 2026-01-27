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
import com.sequenceiq.environment.api.v1.environment.endpoint.CrossRealmTrustEndpoint;
import com.sequenceiq.environment.api.v1.environment.model.response.AddCrossRealmTrustResponse;
import com.sequenceiq.environment.api.v2.environment.model.request.AddCrossRealmTrustV2Request;
import com.sequenceiq.environment.credential.v1.converter.EnvironmentHybridConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.service.EnvironmentModificationService;
import com.sequenceiq.flow.api.model.FlowIdentifier;

@Controller
@Transactional(TxType.NEVER)
@AccountEntityType(Environment.class)
public class CrossRealmTrustController implements CrossRealmTrustEndpoint {

    private final EnvironmentModificationService environmentModificationService;

    private final EnvironmentHybridConverter environmentHybridConverter;

    public CrossRealmTrustController(
            EnvironmentModificationService environmentModificationService,
            EnvironmentHybridConverter environmentHybridConverter) {
        this.environmentModificationService = environmentModificationService;
        this.environmentHybridConverter = environmentHybridConverter;
    }

    @Override
    @CheckPermissionByResourceName(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public AddCrossRealmTrustResponse addTrustByName(@ResourceName String environmentName, AddCrossRealmTrustV2Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.addCrossRealmSetup(
                accountId,
                NameOrCrn.ofName(environmentName),
                request);
        return environmentHybridConverter.convertToAddCrossRealmTrustResponse(flowIdentifier);
    }

    @Override
    @CheckPermissionByResourceCrn(action = AuthorizationResourceAction.EDIT_ENVIRONMENT)
    public AddCrossRealmTrustResponse addTrustByCrn(@ResourceCrn String crn, AddCrossRealmTrustV2Request request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        FlowIdentifier flowIdentifier = environmentModificationService.addCrossRealmSetup(
                accountId,
                NameOrCrn.ofCrn(crn),
                request);
        return environmentHybridConverter.convertToAddCrossRealmTrustResponse(flowIdentifier);
    }
}
