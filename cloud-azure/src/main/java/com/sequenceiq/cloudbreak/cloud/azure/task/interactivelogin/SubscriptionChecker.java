package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.AZURE_MANAGEMENT;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.management.resources.SubscriptionState;
import com.sequenceiq.cloudbreak.cloud.azure.AzureSubscription;
import com.sequenceiq.cloudbreak.cloud.azure.AzureSubscriptionListResult;

@Service
public class SubscriptionChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(SubscriptionChecker.class);

    public void checkSubscription(String subscriptionId, String accessToken) throws InteractiveLoginException {
        if (subscriptionId == null) {
            throw new InteractiveLoginException("Parameter subscriptionId is required and cannot be null.");
        }
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(AZURE_MANAGEMENT);
        Builder request = resource.path("/subscriptions/" + subscriptionId)
                .queryParam("api-version", "2016-06-01")
                .request();
        request.accept(MediaType.APPLICATION_JSON);

        request.header("Authorization", "Bearer " + accessToken);
        try (Response response = request.get()) {
            if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
                AzureSubscription subscription = response.readEntity(AzureSubscription.class);
                if (!subscription.getState().equals(SubscriptionState.ENABLED)) {
                    throw new InteractiveLoginException("Subscription is in incorrect state:" + "" + subscription.getState());
                }
                LOGGER.debug("Subscription definitions successfully retrieved:" + subscription.getDisplayName());
            } else {
                String errorResponse = response.readEntity(String.class);
                try {
                    String errorMessage = new ObjectMapper().readTree(errorResponse).get("error").get("message").asText();
                    LOGGER.info("Subscription retrieve error:" + errorMessage);
                    throw new InteractiveLoginException("Error with the subscription id: " + subscriptionId + " message: " + errorMessage);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    public List<AzureSubscription> getSubscriptions(String accessToken) throws InteractiveLoginException {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(AZURE_MANAGEMENT);
        Builder request = resource.path("/subscriptions")
                .queryParam("api-version", "2016-06-01")
                .request();
        request.accept(MediaType.APPLICATION_JSON);
        request.header("Authorization", "Bearer " + accessToken);
        Response response = request.get();
        return collectSubscriptions(accessToken, response);
    }

    public List<AzureSubscription> getNextSetOfSubscriptions(String link, String accessToken) throws InteractiveLoginException {
        Client client = ClientBuilder.newClient();
        Builder request = client.target(link).request();
        request.accept(MediaType.APPLICATION_JSON);
        request.header("Authorization", "Bearer " + accessToken);
        Response response = request.get();
        return collectSubscriptions(accessToken, response);
    }

    private List<AzureSubscription> collectSubscriptions(String accessToken, Response response) throws InteractiveLoginException {
        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
            AzureSubscriptionListResult azureSubscriptionListResult = response.readEntity(AzureSubscriptionListResult.class);
            List<AzureSubscription> subscriptionList = azureSubscriptionListResult.getValue();
            if (azureSubscriptionListResult.getNextLink() != null) {
                subscriptionList.addAll(getNextSetOfSubscriptions(azureSubscriptionListResult.getNextLink(), accessToken));
            }
            return subscriptionList;
        } else {
            String errorResponse = response.readEntity(String.class);
            try {
                String errorMessage = new ObjectMapper().readTree(errorResponse).get("error").get("message").asText();
                LOGGER.info("Subscription retrieve error:" + errorMessage);
                throw new InteractiveLoginException("Error with the subscriptions, message: " + errorMessage);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
