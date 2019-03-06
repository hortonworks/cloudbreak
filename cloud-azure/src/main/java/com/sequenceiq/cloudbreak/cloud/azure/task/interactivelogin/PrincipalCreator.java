package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.GRAPH_API_VERSION;
import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.GRAPH_WINDOWS;
import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.LOGIN_MICROSOFTONLINE;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status.Family;

import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.microsoft.azure.management.graphrbac.implementation.ServicePrincipalInner;
import com.sequenceiq.cloudbreak.retry.RetryException;

/**
 * Created by perdos on 10/18/16.
 */
@Service
public class PrincipalCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrincipalCreator.class);

    @Retryable(value = InteractiveLoginException.class, maxAttempts = 15, backoff = @Backoff(delay = 1000))
    public ServicePrincipalInner createServicePrincipal(String accessToken, String appId, String tenantId) throws InteractiveLoginException {
        Response response = createServicePrincipalWithGraph(accessToken, appId, tenantId);

        if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
            String principal = response.readEntity(String.class);

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                ServicePrincipalInner sp = objectMapper.readValue(principal, ServicePrincipalInner.class);
                String objectId = sp.objectId();
                LOGGER.debug("Service principal created with objectId: " + objectId);
                return sp;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            String errorResponse = response.readEntity(String.class);
            LOGGER.info("Create service principal failed: " + errorResponse);
            try {
                String errorMessage = new ObjectMapper().readTree(errorResponse).get("odata.error").get("message").get("value").asText();
                throw new InteractiveLoginException("Service principal creation error: " + errorMessage);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private Response createServicePrincipalWithGraph(String accessToken, String appId, String tenantId) {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(GRAPH_WINDOWS + tenantId);
        Builder request = resource.path("servicePrincipals").queryParam("api-version", GRAPH_API_VERSION).request();
        request.accept(MediaType.APPLICATION_JSON);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("appId", appId);
        jsonObject.addProperty("accountEnabled", true);

        request.header("Authorization", "Bearer " + accessToken);
        return request.post(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON));
    }

    @Retryable(value = RetryException.class, maxAttempts = 20, backoff = @Backoff(delay = 1000, multiplier = 2, maxDelay = 10000))
    public void waitPrincipalCreated(String accessToken, String objectId, String tenantId, AzureApplication app) {
        LOGGER.info("Checking the availability of the service principal: '{}'", objectId);
        Client client = ClientBuilder.newClient();
        checkTheExistenceOnGraph(accessToken, objectId, tenantId, client);
        checkTheAvailabilityWithResourceLogin(objectId, tenantId, app, client);
    }

    private void checkTheExistenceOnGraph(String accessToken, String objectId, String tenantId, Client client) {
        WebTarget resource = client.target(GRAPH_WINDOWS + tenantId);
        Builder request = resource.path("servicePrincipals/" + objectId).queryParam("api-version", GRAPH_API_VERSION).request();
        request.accept(MediaType.APPLICATION_JSON);
        request.header("Authorization", "Bearer " + accessToken);
        try (Response response = request.get()) {
            if (response.getStatus() != HttpStatus.SC_OK) {
                throw new RetryException("Principal with objectId (" + objectId + ") hasn't been created yet");
            }
        }
    }

    private void checkTheAvailabilityWithResourceLogin(String objectId, String tenantId, AzureApplication app, Client client) {
        WebTarget loginResource = client.target(LOGIN_MICROSOFTONLINE + tenantId);
        Map<String, String> formData = new HashMap<>();
        formData.put("grant_type", "client_credentials");
        formData.put("client_id", app.getAppId());
        formData.put("client_secret", app.getAzureApplicationCreationView().getAppSecret());
        formData.put("resource", app.getAzureApplicationCreationView().getAppIdentifierURI());
        try (Response loginResponse = loginResource.path("/oauth2/token")
                .request()
                .accept(MediaType.APPLICATION_JSON)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(Entity.form(new MultivaluedHashMap<>(formData)))) {
            if (loginResponse.getStatus() != HttpStatus.SC_OK) {
                throw new RetryException("Principal with objectId (" + objectId + ") hasn't been available yet");
            }
        }
    }
}
