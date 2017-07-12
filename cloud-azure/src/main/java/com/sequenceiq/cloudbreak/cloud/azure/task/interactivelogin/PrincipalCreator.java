package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.GRAPH_API_VERSION;
import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.GRAPH_WINDOWS;

import java.io.IOException;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.microsoft.azure.management.graphrbac.implementation.ServicePrincipalInner;

/**
 * Created by perdos on 10/18/16.
 */
@Service
public class PrincipalCreator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PrincipalCreator.class);

    @Retryable(value = InteractiveLoginException.class, maxAttempts = 15, backoff = @Backoff(delay = 1000))
    public ServicePrincipalInner createServicePrincipal(String accessToken, String appId, String tenantId) throws InteractiveLoginException {
        Response response = createServicePrincipalWithGraph(accessToken, appId, tenantId);

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            String principal = response.readEntity(String.class);

            try {
                ObjectMapper objectMapper = new ObjectMapper();
                objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                ServicePrincipalInner sp = objectMapper.readValue(principal, ServicePrincipalInner.class);
                String objectId = sp.objectId();
                LOGGER.info("Service principal created with objectId: " + objectId);
                return sp;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            String errorResponse = response.readEntity(String.class);
            LOGGER.error("create service principal failed: " + errorResponse);
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
        Invocation.Builder request = resource.path("servicePrincipals").queryParam("api-version", GRAPH_API_VERSION).request();
        request.accept(MediaType.APPLICATION_JSON);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("appId", appId);
        jsonObject.addProperty("accountEnabled", true);

        request.header("Authorization", "Bearer " + accessToken);
        return request.post(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON));
    }
}
