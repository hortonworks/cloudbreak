package com.sequenceiq.environment.terms.v1;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.environment.api.v1.terms.endpoint.TermsEndpoint;
import com.sequenceiq.environment.api.v1.terms.model.TermType;
import com.sequenceiq.environment.api.v1.terms.model.TermsRequest;
import com.sequenceiq.environment.api.v1.terms.model.TermsResponse;
import com.sequenceiq.environment.terms.service.TermsService;

@Controller
@Transactional(TxType.NEVER)
public class TermsController implements TermsEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(TermsController.class);

    private final TermsService termsService;

    public TermsController(TermsService termsService) {
        this.termsService = termsService;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    public TermsResponse get(TermType termType) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return getInAccount(accountId, termType);
    }

    @Override
    @InternalOnly
    public TermsResponse getInAccount(@AccountId String accountId, TermType termType) {
        Boolean accepted = termsService.get(accountId, termType);
        LOGGER.debug("{} terms acceptance setting in account {}: {}", termType, accountId, accepted);
        return new TermsResponse(accepted, termType);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    public TermsResponse put(TermsRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("New terms acceptance setting in account {} : {}", accountId, request.getAccepted());
        termsService.updateOrCreate(request.getAccepted(), request.getTermType(), accountId);
        return new TermsResponse(request.getAccepted(), request.getTermType());
    }
}
