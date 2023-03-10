package com.sequenceiq.environment.marketplace.v1.controller;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;

import com.sequenceiq.authorization.annotation.CheckPermissionByAccount;
import com.sequenceiq.authorization.annotation.InternalOnly;
import com.sequenceiq.authorization.resource.AuthorizationResourceAction;
import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsRequest;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.marketplace.service.AzureMarketplaceTermsService;
import com.sequenceiq.notification.NotificationController;

@Controller
@Transactional(TxType.NEVER)
public class AzureMarketplaceTermsController extends NotificationController implements AzureMarketplaceTermsEndpoint {

    private final AzureMarketplaceTermsService azureMarketplaceTermsService;

    private final EnvironmentService environmentService;

    public AzureMarketplaceTermsController(AzureMarketplaceTermsService azureMarketplaceTermsService, EnvironmentService environmentService) {
        this.azureMarketplaceTermsService = azureMarketplaceTermsService;
        this.environmentService = environmentService;
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public AzureMarketplaceTermsResponse get() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        return getInAccount(accountId);
    }

    @Override
    @InternalOnly
    public AzureMarketplaceTermsResponse getInAccount(String accountId) {
        Boolean accepted = azureMarketplaceTermsService.get(accountId);
        return new AzureMarketplaceTermsResponse(accepted);
    }

    @Override
    @CheckPermissionByAccount(action = AuthorizationResourceAction.POWERUSER_ONLY)
    public AzureMarketplaceTermsResponse put(@Valid AzureMarketplaceTermsRequest request) {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        azureMarketplaceTermsService.updateOrCreate(request.getAccepted(), accountId);
        return new AzureMarketplaceTermsResponse(request.getAccepted());
    }
}
