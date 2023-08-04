package com.sequenceiq.it.cloudbreak.action.v1.terms;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.environment.api.v1.marketplace.model.AzureMarketplaceTermsResponse;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.TermsPolicyDto;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;

public class TermsPutAction implements Action<TermsPolicyDto, EnvironmentClient> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TermsPutAction.class);

    @Override
    public TermsPolicyDto action(TestContext testContext, TermsPolicyDto testDto, EnvironmentClient client) throws Exception {
        AzureMarketplaceTermsResponse response = client.getDefaultClient().azureMarketplaceTermsEndpoint().get();
        if (!response.getAccepted().equals(testDto.getRequest().getAccepted())) {
            response = client.getDefaultClient().azureMarketplaceTermsEndpoint().put(testDto.getRequest());
        }
        testDto.setResponse(response);
        Log.whenJson(LOGGER, " Terms setting updated successfully:\n", testDto.getResponse());
        return testDto;
    }
}
