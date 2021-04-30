package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

import java.net.URI;
import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.rest.AzureRestOperationsService;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

@Service
public class AzureImageTermsSignerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureImageTermsSignerService.class);

    private static final String READ_ERROR_MESSAGE_TEMPLATE = "Error when retrieving if marketplace image terms and conditions are signed for %s.";

    private static final String SIGN_ERROR_MESSAGE_TEMPLATE = "Error when signing marketplace image terms and conditions for %s.";

    private static final String SIGN_ERROR_MESSAGE_HINTS = "Please try again. Alternatively you can also sign it manually, please refer to azure " +
            "documentation at https://docs.microsoft.com/en-us/cli/azure/vm/image/terms?view=azure-cli-latest.";

    private static final String SIGN_URL_AZ_TEMPLATE = "https://management.azure.com/subscriptions/%s/providers/Microsoft.MarketplaceOrdering/offerTypes/" +
            "virtualmachine/publishers/%s/offers/%s/plans/%s/agreements/current?api-version=2015-06-01";

    @Inject
    private AzureRestOperationsService azureRestOperationsService;

    @Retryable(maxAttempts = 15, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public boolean isSigned(String subscriptionId, AzureMarketplaceImage azureMarketplaceImage, AzureClient azureClient) {
        URI signUri = getSignUri(subscriptionId, azureMarketplaceImage);
        ErrorMessageBuilder errorMessageBuilder = new ErrorMessageBuilder(String.format(READ_ERROR_MESSAGE_TEMPLATE, azureMarketplaceImage))
                .withPostfix("Please try again.");
        String token = getToken(azureClient, errorMessageBuilder);
        try {
            AzureImageTerms azureImageTerms = azureRestOperationsService.httpGet(signUri, AzureImageTerms.class, token);
            LOGGER.debug("Image terms and conditions received for image {} is : {}", azureMarketplaceImage, azureImageTerms);
            return azureImageTerms.getProperties().isAccepted();
        } catch (Exception e) {
            String message = errorMessageBuilder.buildWithReason(e.getMessage());
            LOGGER.warn(message);
            throw new CloudConnectorException(message, e);
        }
    }

    /**
     * Should not yet be used.
     *
     * @param subscriptionId        Azure subscription id
     * @param azureMarketplaceImage The marketplace image to be signed
     * @param azureClient           Azure client
     */
    @Retryable(maxAttempts = 15, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public void sign(String subscriptionId, AzureMarketplaceImage azureMarketplaceImage, AzureClient azureClient) {
        URI signUri = getSignUri(subscriptionId, azureMarketplaceImage);
        ErrorMessageBuilder errorMessageBuilder =
                new ErrorMessageBuilder(String.format(SIGN_ERROR_MESSAGE_TEMPLATE, azureMarketplaceImage)).withPostfix(SIGN_ERROR_MESSAGE_HINTS);
        String token = getToken(azureClient, errorMessageBuilder);

        try {
            AzureImageTerms azureImageTerms = azureRestOperationsService.httpGet(signUri, AzureImageTerms.class, token);
            LOGGER.debug("Image terms and conditions received for image {} is : {}", azureMarketplaceImage, azureImageTerms);

            azureImageTerms.getProperties().setAccepted(true);

            AzureImageTerms responseImageTerms = azureRestOperationsService.httpPut(signUri, azureImageTerms, AzureImageTerms.class, token);
            LOGGER.debug("Image terms and conditions received for image {} is : {}", azureMarketplaceImage, responseImageTerms);
        } catch (Exception e) {
            String message =
                    errorMessageBuilder.buildWithReason(String.format("error when signing vm image terms and conditions, message is '%s'", e.getMessage()));
            LOGGER.warn(message);
            throw new CloudConnectorException(message, e);
        }
    }

    private String getToken(AzureClient azureClient, ErrorMessageBuilder errorMessageBuilder) {
        Optional<String> tokenOptional = azureClient.getAccessToken();
        if (tokenOptional.isEmpty()) {
            throw new CloudConnectorException(errorMessageBuilder.buildWithReason("could not get access token"));
        }
        return tokenOptional.get();
    }

    private URI getSignUri(String subscriptionId, AzureMarketplaceImage azureMarketplaceImage) {
        String signUrl = String.format(SIGN_URL_AZ_TEMPLATE,
                subscriptionId, azureMarketplaceImage.getPublisherId(), azureMarketplaceImage.getOfferId(), azureMarketplaceImage.getPlanId());
        return URI.create(signUrl);
    }

    private static class ErrorMessageBuilder {
        private final String prefix;

        private String postfix = "";

        private ErrorMessageBuilder(String prefix) {
            this.prefix = prefix;
        }

        private ErrorMessageBuilder withPostfix(String postfix) {
            this.postfix = postfix;
            return this;
        }

        private String buildWithReason(String reason) {
            return String.format("%s Reason: %s. %s", prefix, reason, postfix);
        }
    }
}
