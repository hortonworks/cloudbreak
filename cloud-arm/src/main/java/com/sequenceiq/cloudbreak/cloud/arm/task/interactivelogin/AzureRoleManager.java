package com.sequenceiq.cloudbreak.cloud.arm.task.interactivelogin;

import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import com.google.gson.JsonObject;

/**
 * Created by perdos on 10/18/16.
 */
@Service
public class AzureRoleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureRoleManager.class);
    private static final String AZURE_MANAGEMENT = "https://management.azure.com";
    private static final String OWNER_ROLE = "Owner";

    @Retryable(value = IllegalStateException.class, maxAttempts = 10, backoff = @Backoff(delay = 1000))
    public void assignRole(String accessToken, String subscriptionId, String principalObjectId) {
        String ownerRoleNameRoleIdPair = getOwnerRoleDefinitionId(subscriptionId, accessToken);
        assignRole(accessToken, subscriptionId, ownerRoleNameRoleIdPair, principalObjectId);
    }

    private String getOwnerRoleDefinitionId(String subscriptionId, String accessToken) {
        Response response = getOwnerRole(subscriptionId, accessToken);

        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            String roles = response.readEntity(String.class);
            try {
                JSONObject rolesJson = new JSONObject(roles);
                String roleDefinitionId = rolesJson.getJSONArray("value").getJSONObject(0).getString("id");
                LOGGER.info("Role definition - roleId: " + roleDefinitionId);
                return roleDefinitionId;
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
        } else {
            throw new IllegalStateException("get 'Owner' role name and id request error - status code: " + response.getStatus()
                    + " - error message: " + response.readEntity(String.class));
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

    private void assignRole(String accessToken, String subscriptionId, String roleDefinitionId, String principalObjectId) {
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
            throw new IllegalStateException("Assign role request error - status code: " + response.getStatus()
                    + " - error message: " + response.readEntity(String.class));
        } else {
            LOGGER.info("Role assigned successfully");
        }
    }
}
