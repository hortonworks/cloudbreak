package com.sequenceiq.it.cloudbreak.action.v1.terms;

import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.TermsPolicyDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.util.CloudbreakUtil;

public class TermsPutAction implements Action<TermsPolicyDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TermsPutAction.class);

    @Override
    public TermsPolicyDto action(TestContext testContext, TermsPolicyDto testDto, EnvironmentClient client) throws Exception {
        AzureMarketplaceTermsResponse response = setOrGetAzureMarketplaceTermsWithRetry(testDto, client);
        testDto.setResponse(response);
        Log.whenJson(LOGGER, " Terms setting updated successfully:\n", testDto.getResponse());
        return testDto;
    }

    private AzureMarketplaceTermsResponse setOrGetAzureMarketplaceTermsWithRetry(TermsPolicyDto testDto, EnvironmentClient client) {
        try {
            return setOrGetAzureMarketplaceTerms(testDto, client);
        } catch (Exception e) {
            LOGGER.warn("Failed to set/get AzureMarketplaceTerms for the first time, retrying.", e);
            CloudbreakUtil.sleep(ThreadLocalRandom.current().nextInt(1000, 3000));
            return setOrGetAzureMarketplaceTerms(testDto, client);
        }
    }

    private AzureMarketplaceTermsResponse setOrGetAzureMarketplaceTerms(TermsPolicyDto testDto, EnvironmentClient client) {
        AzureMarketplaceTermsResponse response = client.getDefaultClient(testDto.getTestContext()).azureMarketplaceTermsEndpoint().get();
        if (!response.getAccepted().equals(testDto.getRequest().getAccepted())) {
            return client.getDefaultClient(testDto.getTestContext()).azureMarketplaceTermsEndpoint().put(testDto.getRequest());
        } else {
            return response;
        }
    }
}
