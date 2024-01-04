package com.sequenceiq.freeipa.service.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionHandler;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;

@Service
public class AzureMarketplaceTermsClientService {

    @Inject
    private AzureMarketplaceTermsEndpoint azureMarketplaceTermsEndpoint;

    @Inject
    private WebApplicationExceptionHandler webApplicationExceptionHandler;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Boolean getAccepted() {
        String accountId = ThreadBasedUserCrnProvider.getAccountId();
        try {
            AzureMarketplaceTermsResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> azureMarketplaceTermsEndpoint.getInAccount(accountId));
            return response.getAccepted();
        } catch (WebApplicationException e) {
            throw webApplicationExceptionHandler.handleException(e);
        }
    }
}
