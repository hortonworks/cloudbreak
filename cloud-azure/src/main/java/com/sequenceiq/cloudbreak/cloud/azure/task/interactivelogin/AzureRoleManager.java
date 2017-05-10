package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.AZURE_MANAGEMENT;

import java.io.IOException;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;

/**
 * Created by perdos on 10/18/16.
 */
@Service
public class AzureRoleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureRoleManager.class);

    private static final String OWNER_ROLE = "Owner";

    @Retryable(value = InteractiveLoginException.class, maxAttempts = 15, backoff = @Backoff(delay = 1000))
    public void assignRole(String accessToken, String subscriptionId, String principalObjectId) throws InteractiveLoginException {
        String ownerRoleNameRoleIdPair = getOwnerRoleDefinitionId(subscriptionId, accessToken);
        assignRole(accessToken, subscriptionId, ownerRoleNameRoleIdPair, principalObjectId);
    }

    private String getOwnerRoleDefinitionId(String subscriptionId, String accessToken) throws InteractiveLoginException {
        Response response = getOwnerRole(subscriptionId, accessToken);

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            String roles = response.readEntity(String.class);
            try {
                JsonNode rolesJsonNode = new ObjectMapper().readTree(roles).get("refresh_token");
                String roleDefinitionId = rolesJsonNode.get("value").get(0).get("id").asText();
                LOGGER.info("Role definition - roleId: " + roleDefinitionId);
                return roleDefinitionId;
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            if (Response.Status.FORBIDDEN.getStatusCode() == response.getStatus()) {
                throw new InteractiveLoginException("You have no permission to access Active Directory roles, please contact with your administrator");
            } else {
                String errorResponse = response.readEntity(String.class);
                LOGGER.error("get owner role definition id failed: " + errorResponse);
                try {
                    String errorMessage = new ObjectMapper().readTree(errorResponse).get("error").get("message").asText();
                    throw new InteractiveLoginException("Get 'Owner' role name and definition id request error: " + errorMessage);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }

    }

    private Response getOwnerRole(String subscriptionId, String accessToken) {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(AZURE_MANAGEMENT);
        Invocation.Builder request = resource.path("subscriptions/" + subscriptionId + "/providers/Microsoft.Authorization/roleDefinitions")
                .queryParam("$filter", "roleName%20eq%20'" + OWNER_ROLE + "'")
                .queryParam("api-version", "2015-07-01")
                .request();
        request.accept(MediaType.APPLICATION_JSON);

        request.header("Authorization", "Bearer " + accessToken);
        return request.get();
    }

    private void assignRole(String accessToken, String subscriptionId, String roleDefinitionId, String principalObjectId) throws InteractiveLoginException {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(AZURE_MANAGEMENT);
        Invocation.Builder request = resource.path("subscriptions/" + subscriptionId + "/providers/Microsoft.Authorization/roleAssignments/"
                + UUID.randomUUID().toString()).queryParam("api-version", "2015-07-01").request();
        request.accept(MediaType.APPLICATION_JSON);

        request.header("Authorization", "Bearer " + accessToken);

        JsonObject properties = new JsonObject();
        properties.addProperty("roleDefinitionId", roleDefinitionId);
        properties.addProperty("principalId", principalObjectId);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("properties", properties);

        Response response = request.put(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON));

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new InteractiveLoginException("Assign role request error - status code: " + response.getStatus()
                    + " - error message: " + response.readEntity(String.class));
        } else {
            LOGGER.info("Role assigned successfully");
        }
    }
}

