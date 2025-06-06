package com.sequenceiq.environment.marketplace.v1.controller;

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
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsRequest;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;
import com.sequenceiq.environment.marketplace.service.AzureMarketplaceTermsService;
import com.sequenceiq.notification.NotificationController;

@Controller
@Transactional(TxType.NEVER)
public class AzureMarketplaceTermsController extends NotificationController implements AzureMarketplaceTermsEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMarketplaceTermsController.class);

    private final AzureMarketplaceTermsService azureMarketplaceTermsService;

    public AzureMarketplaceTermsController(AzureMarketplaceTermsService azureMarketplaceTermsService) {
        this.azureMarketplaceTermsService = azureMarketplaceTermsService;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    public AzureMarketplaceTermsResponse get() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return getInAccount(accountId);
    }

    @Override
    @InternalOnly
    public AzureMarketplaceTermsResponse getInAccount(@AccountId String accountId) {
        Boolean accepted = azureMarketplaceTermsService.get(accountId);
        LOGGER.debug("Automatic image terms acceptance setting in account {}: {}", accountId, accepted);
        return new AzureMarketplaceTermsResponse(accepted);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.CREATE_ENVIRONMENT)
    public AzureMarketplaceTermsResponse put(AzureMarketplaceTermsRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        LOGGER.debug("New automatic image terms acceptance setting in account {} : {}", accountId, request.getAccepted());
        azureMarketplaceTermsService.updateOrCreate(request.getAccepted(), accountId);
        return new AzureMarketplaceTermsResponse(request.getAccepted());
    }
}
