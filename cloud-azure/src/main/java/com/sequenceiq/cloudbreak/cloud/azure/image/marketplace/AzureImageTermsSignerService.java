package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import java.net.URI;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Service
public class AzureImageTermsSignerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureImageTermsSignerService.class);

    @Inject
    private RestOperationsService restOperationsService;

    public void sign(String subscriptionId, AzureMarketplaceImage azureMarketplaceImage, AzureClient azureClient) {
        signWithAzUrlGetPut(subscriptionId, azureMarketplaceImage, azureClient);

    }

    private void signWithAzUrlGetPut(String subscriptionId, AzureMarketplaceImage azureMarketplaceImage, AzureClient azureClient) {
        String signUrlAzTemplate = "https://management.azure.com/subscriptions/%s/providers/Microsoft.MarketplaceOrdering/offerTypes/virtualmachine/" +
                "publishers/%s/offers/%s/plans/%s/agreements/current?api-version=2015-06-01";
        String signUrl = String.format(signUrlAzTemplate,
                subscriptionId, azureMarketplaceImage.getPublisherId(), azureMarketplaceImage.getOfferId(), azureMarketplaceImage.getPlanId());
        URI signUri = URI.create(signUrl);

        Optional<String> tokenOptional = azureClient.getAccessToken();
        if (tokenOptional.isEmpty()) {
            throw new CloudConnectorException("Could not get access token when trying to sign terms and conditions.");
        }

        try {
            AzureImageTerms azureImageTerms = restOperationsService.httpGet(signUri, AzureImageTerms.class, tokenOptional.get());
            LOGGER.debug("Image terms and conditions received for image {} is : {}", azureMarketplaceImage, azureImageTerms);

            azureImageTerms.getProperties().setAccepted(true);

            AzureImageTerms responseImageTerms = restOperationsService.httpPut(signUri, azureImageTerms, AzureImageTerms.class, tokenOptional.get());
            LOGGER.debug("Image terms and conditions received for image {} is : {}", azureMarketplaceImage, responseImageTerms);
        } catch (Exception e) {
            String message = String.format("Exception occurred when signing vm image terms and conditions, method azure-cli url. Message is %s",
                    e.getMessage());
            LOGGER.warn(message);
            throw new CloudConnectorException(message, e);
        }

    }

    private void signWithUrl1Post(String subscriptionId, AzureMarketplaceImage azureMarketplaceImage, AzureClient azureClient) {
        String signUrlTemplate = "https://management.azure.com/subscriptions/%s/providers/Microsoft.MarketplaceOrdering/agreements/%s/offers/%s/" +
                "plans/%s/sign?api-version=2015-06-01";
        String signUrl = String.format(signUrlTemplate,
                subscriptionId, azureMarketplaceImage.getPublisherId(), azureMarketplaceImage.getOfferId(), azureMarketplaceImage.getPlanId());
        URI signUri = URI.create(signUrl);

        Optional<String> tokenOptional = azureClient.getAccessToken();
        if (tokenOptional.isEmpty()) {
            throw new CloudConnectorException("Could not get access token when trying to sign terms and conditions.");
        }

        try {
            restOperationsService.httpGet(signUri, Void.class, tokenOptional.get());
        } catch (Exception e) {
            LOGGER.warn("Exception when signing vm image terms and conditions, method url1.");
            throw new CloudConnectorException("Exception when signing vm image terms and conditions, method url1.", e);
        }
    }

}
