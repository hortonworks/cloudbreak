package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.GRAPH_API_VERSION;
import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.GRAPH_WINDOWS;

import java.io.IOException;

import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sequenceiq.cloudbreak.cloud.azure.AzureApplicationCreationView;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCredentialAppCreationCommand;
import com.sequenceiq.cloudbreak.retry.RetryException;

/**
 * Created by perdos on 10/18/16.
 */
@Service
public class ApplicationCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationCreator.class);

    @Inject
    private AzureCredentialAppCreationCommand appCreationCommand;

    public AzureApplication createApplication(String accessToken, String tenantId, String deploymentAddress) throws InteractiveLoginException {

        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(GRAPH_WINDOWS + tenantId);
        Builder request = resource.path("/applications").queryParam("api-version", GRAPH_API_VERSION).request();
        request.accept(MediaType.APPLICATION_JSON);
        request.header("Authorization", "Bearer " + accessToken);
        AzureApplicationCreationView appCreationView = appCreationCommand.generateJSON(deploymentAddress);
        try (Response response = request.post(Entity.entity(appCreationView.getAppCreationRequestPayload(), MediaType.APPLICATION_JSON))) {
            if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
                String application = response.readEntity(String.class);
                try {
                    JsonNode applicationJson = new ObjectMapper().readTree(application);
                    String appId = applicationJson.get("appId").asText();
                    String objectId = applicationJson.get("objectId").asText();
                    LOGGER.info("Application created with appId: " + appId);
                    return new AzureApplication(appId, objectId, appCreationView);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                String errorResponse = response.readEntity(String.class);
                try {
                    String errorMessage = new ObjectMapper().readTree(errorResponse).get("odata.error").get("message").get("value").asText();
                    throw new InteractiveLoginException("AD Application creation error: " + errorMessage);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    @Retryable(value = RetryException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public void waitApplicationCreated(String accessToken, String tenantId, String objectId) {
        LOGGER.info("Checking the existence of the application: '{}'", objectId);
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(GRAPH_WINDOWS + tenantId);
        Builder request = resource.path("/applications/" + objectId).queryParam("api-version", GRAPH_API_VERSION).request();
        request.accept(MediaType.APPLICATION_JSON);
        request.header("Authorization", "Bearer " + accessToken);
        try (Response response = request.get()) {
            if (response.getStatus() != HttpStatus.SC_OK) {
                throw new RetryException("App with objectId (" + objectId + ") hasn't been created yet");
            }
        }
    }
}
