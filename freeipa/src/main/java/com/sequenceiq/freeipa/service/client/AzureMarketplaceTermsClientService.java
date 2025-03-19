package com.sequenceiq.freeipa.service.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;

@Service
public class AzureMarketplaceTermsClientService {

    @Inject
    private AzureMarketplaceTermsEndpoint azureMarketplaceTermsEndpoint;

    @Inject
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    public Boolean getAccepted() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        try {
            AzureMarketplaceTermsResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> azureMarketplaceTermsEndpoint.getInAccount(accountId));
            return response.getAccepted();
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }
}
