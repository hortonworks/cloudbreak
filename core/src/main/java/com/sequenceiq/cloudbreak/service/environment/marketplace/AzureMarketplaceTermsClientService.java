package com.sequenceiq.cloudbreak.service.environment.marketplace;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.environment.api.v1.marketplace.endpoint.AzureMarketplaceTermsEndpoint;
import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;

@Service
public class AzureMarketplaceTermsClientService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureMarketplaceTermsClientService.class);

    @Inject
    private AzureMarketplaceTermsEndpoint azureMarketplaceTermsEndpoint;

    public Boolean getAccepted(String resourceCrn) {
        String accountId = Crn.safeFromString(resourceCrn).getAccountId();
        try {
            AzureMarketplaceTermsResponse response = ThreadBasedUserCrnProvider.doAsInternalActor(
                    () -> azureMarketplaceTermsEndpoint.getInAccount(accountId));
            return response.getAccepted();
        } catch (NotFoundException e) {
            String message = String.format("Azure Marketplace Terms acceptance setting not found for account id: %s", accountId);
            LOGGER.warn(message, e);
            return false;
        } catch (WebApplicationException | ProcessingException | IllegalStateException e) {
            String message = String.format("Failed to GET Azure Marketplace Terms acceptance setting for account id: %s, due to: '%s'",
                    accountId, e.getMessage());
            LOGGER.error(message, e);
            throw new CloudbreakServiceException(message, e);
        }
    }
}
