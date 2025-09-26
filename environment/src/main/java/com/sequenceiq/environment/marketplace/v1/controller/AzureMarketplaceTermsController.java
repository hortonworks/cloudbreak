package com.sequenceiq.environment.marketplace.v1.controller;

import static com.sequenceiq.environment.api.v1.terms.model.TermType.AZURE_MARKETPLACE_IMAGE_TERMS;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.DisableCheckPermissions;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.security.internal.AccountId;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsRequest;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;
import com.sequenceiq.environment.terms.service.TermsService;
import com.sequenceiq.notification.NotificationController;

@Controller
@Transactional(TxType.NEVER)
public class AzureMarketplaceTermsController extends NotificationController implements AzureMarketplaceTermsEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMarketplaceTermsController.class);

    private final TermsService termsService;

    public AzureMarketplaceTermsController(TermsService termsService) {
        this.termsService = termsService;
    }

    @Override
    @DisableCheckPermissions
    public AzureMarketplaceTermsResponse get() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return getInAccount(accountId);
    }

    @Override
    @InternalOnly
    public AzureMarketplaceTermsResponse getInAccount(@AccountId String accountId) {
        Boolean accepted = termsService.get(accountId, AZURE_MARKETPLACE_IMAGE_TERMS);
        LOGGER.debug("Automatic image terms acceptance setting in account {}: {}", accountId, accepted);
        return new AzureMarketplaceTermsResponse(accepted);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    public AzureMarketplaceTermsResponse put(AzureMarketplaceTermsRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("New automatic image terms acceptance setting in account {} : {}", accountId, request.getAccepted());
        termsService.updateOrCreate(request.getAccepted(), AZURE_MARKETPLACE_IMAGE_TERMS, accountId);
        return new AzureMarketplaceTermsResponse(request.getAccepted());
    }
}
