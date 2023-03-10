package com.sequenceiq.cloudbreak.service.environment.marketplace;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;

@Service
public class AzureMarketplaceTermsClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMarketplaceTermsClientService.class);

    @Inject
    private AzureMarketplaceTermsEndpoint azureMarketplaceTermsEndpoint;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    public Boolean getAccepted() {
        try {
            String accountId = ThreadBasedUserCrnProvider.getAccountId();
            AzureMarketplaceTermsResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    regionAwareInternalCrnGeneratorFactory.iam().getInternalCrnForServiceAsString(),
                    () -> azureMarketplaceTermsEndpoint.getInAccount(accountId));
            return response.getAccepted();
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET Azure Marketplace Terms acceptance setting with account id, due to: '%s'", e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
