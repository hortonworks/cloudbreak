package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.AZURE_MANAGEMENT;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.sequenceiq.cloudbreak.cloud.azure.AzurePermission;
import com.sequenceiq.cloudbreak.cloud.azure.AzureRoleDefinition;
import com.sequenceiq.cloudbreak.cloud.azure.AzureRoleDefinitionListResponse;
import com.sequenceiq.cloudbreak.cloud.azure.AzureRoleDefinitionProperties;

/**
 * Created by perdos on 10/18/16.
 */
@Service
public class AzureRoleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureRoleManager.class);

    private static final String CONTRIBUTOR_ROLE = "Contributor";

    public String handleRoleOperations(String accessToken, String subscriptionId, String roleName, String roleType) throws
            InteractiveLoginUnrecoverableException, InteractiveLoginException {
        String roleId;
        if (roleName == null || roleType == null) {
            throw new InteractiveLoginUnrecoverableException("Role name or type is missing!");
        }
        RoleType type = RoleType.valueOf(roleType);
        AzureRoleDefinition existingRole;

        switch (type) {
            // Scenario #1: assign 'Contributor' role
            case CONTRIBUTOR:
                roleId = getContributorRoleDefinitionId(subscriptionId, accessToken);
                break;
            // Scenario #2: create custom role
            case CREATE_CUSTOM:
                existingRole = getRoleDefinitonByName(subscriptionId, accessToken, roleName);
                if (existingRole != null) {
                    LOGGER.error("Role already exists in Azure with the name: " + roleName);
                    throw new InteractiveLoginUnrecoverableException("Role already exists in Azure with the name: " + roleName);
                }
                roleId = createRole(accessToken, subscriptionId, roleName);
                break;
            // Scenario #3: reuse already existing role
            case REUSE_EXISTING:
                existingRole = getRoleDefinitonByName(subscriptionId, accessToken, roleName);
                if (existingRole == null) {
                    LOGGER.error("Role does not exist in Azure with the name: " + roleName);
                    throw new InteractiveLoginUnrecoverableException("Role does not exist in Azure with the name: " + roleName);
                }
                if (!validateRoleActions(existingRole)) {
                    LOGGER.error(existingRole.getProperties().getRoleName()
                            + " role does not have enough privileges to be used by Cloudbreak! Please contact the documentation for more information!");
                    throw new InteractiveLoginUnrecoverableException(existingRole.getProperties().getRoleName()
                            + " role does not have enough privileges to be used by Cloudbreak! Please contact the documentation for more information!");
                }
                roleId = existingRole.getId();
                break;
            default:
                throw new InteractiveLoginUnrecoverableException("Role type " + roleType + " is invalid!");
        }
        return roleId;
    }

    @Retryable(value = InteractiveLoginException.class, maxAttempts = 15, backoff = @Backoff(delay = 1000))
    public void assignRole(String accessToken, String subscriptionId, String roleDefinitionId, String principalObjectId) throws InteractiveLoginException {
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
            String errorMsg = response.readEntity(String.class);
            LOGGER.error("Assign role request error - status code: " + response.getStatus()
                    + " - error message: " + errorMsg);
            throw new InteractiveLoginException("Assign role request error - status code: " + response.getStatus()
                    + " - error message: " + errorMsg);
        } else {
            LOGGER.info("Role assigned successfully");
        }
    }

    private String getContributorRoleDefinitionId(String subscriptionId, String accessToken) throws InteractiveLoginException {
        List<AzureRoleDefinition> roleDefinitions = getRoleDefinitions(subscriptionId, accessToken, "roleName%20eq%20'" + CONTRIBUTOR_ROLE + "'");
        if (roleDefinitions.size() == 1) {
            return roleDefinitions.get(0).getId();
        } else {
            throw new InteractiveLoginException("Get 'Contributor' role name and definition id request returned: " + roleDefinitions.size() + " responses!");
        }
    }

    private AzureRoleDefinition getRoleDefinitonByName(String subscriptionId, String accessToken, String roleName) throws InteractiveLoginException {
        List<AzureRoleDefinition> roleList = getRoleDefinitions(subscriptionId, accessToken, "roleName%20eq%20'" + roleName + "'");
        if (roleList.size() > 0) {
            return roleList.get(0);
        }
        return null;
    }

    private List<AzureRoleDefinition> getCustomRoleDefinitons(String subscriptionId, String accessToken) throws InteractiveLoginException {
        return getRoleDefinitions(subscriptionId, accessToken, "type%20eq%20'CustomRole'");
    }

    private List<AzureRoleDefinition> getRoleDefinitions(String subscriptionId, String accessToken, String filter) throws InteractiveLoginException {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(AZURE_MANAGEMENT);
        Invocation.Builder request = resource.path("subscriptions/" + subscriptionId + "/providers/Microsoft.Authorization/roleDefinitions")
                .queryParam("$filter", filter)
                .queryParam("api-version", "2015-07-01")
                .request();
        request.accept(MediaType.APPLICATION_JSON);

        request.header("Authorization", "Bearer " + accessToken);
        Response response = request.get();
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
            AzureRoleDefinitionListResponse azureRoleDefinitionListResponse = response.readEntity(AzureRoleDefinitionListResponse.class);
            LOGGER.info("Role definitions retrieved:" + azureRoleDefinitionListResponse.getValue());
            return azureRoleDefinitionListResponse.getValue();

        } else {
            if (Response.Status.FORBIDDEN.getStatusCode() == response.getStatus()) {
                throw new InteractiveLoginException("You have no permission to access Active Directory roles, please contact with your administrator");
            } else {
                String errorResponse = response.readEntity(String.class);
                LOGGER.error("Get role definition request with filter: " + filter + " failed:" + errorResponse);
                try {
                    String errorMessage = new ObjectMapper().readTree(errorResponse).get("error").get("message").asText();
                    throw new InteractiveLoginException("Get role definition request with filter: " + filter + " failed: " + errorMessage);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    private String createRole(String accessToken, String subscriptionId, String roleName) throws InteractiveLoginException {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(AZURE_MANAGEMENT);
        String uuid = UUID.randomUUID().toString();
        String roleDefinitionId = "subscriptions/" + subscriptionId + "/providers/Microsoft.Authorization/roleDefinitions/"
                + uuid;
        Invocation.Builder request = resource.path(roleDefinitionId).queryParam("api-version", "2015-07-01").request();
        request.accept(MediaType.APPLICATION_JSON);

        request.header("Authorization", "Bearer " + accessToken);

        String customRole = "";
        try {
            customRole = new ObjectMapper().writeValueAsString(assembleCustomRole(subscriptionId, roleName));
        } catch (JsonProcessingException e) {
            LOGGER.error("Create role request processing error", e);
            throw new InteractiveLoginException("Create role request error - " + e.getMessage());
        }

        Response response = request.put(Entity.entity(customRole, MediaType.APPLICATION_JSON));

        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            String errorMsg = response.readEntity(String.class);
            LOGGER.error("Create role request error - status code: " + response.getStatus()
                    + " - error message: " + errorMsg);
            throw new InteractiveLoginException("Create role request error - status code: " + response.getStatus()
                    + " - error message: " + errorMsg);
        }
        LOGGER.info("Role " + roleName +  " created successfully");
        return roleDefinitionId;
    }

    private AzureRoleDefinition assembleCustomRole(String subscriptionId, String roleName) {

        String uuid = UUID.randomUUID().toString();
        AzureRoleDefinition role = new AzureRoleDefinition();
        role.setName(uuid);

        AzureRoleDefinitionProperties properties = new AzureRoleDefinitionProperties();
        properties.setRoleName(roleName);
        properties.setDescription("Custom role for cluster management via Cloudbreak client");
        properties.setAssignableScopes(Arrays.asList("/subscriptions/" + subscriptionId));
        properties.setType("CustomRole");
        AzurePermission permission = new AzurePermission();
        permission.setActions(Arrays.asList(
                "Microsoft.Authorization/*/read",
                "Microsoft.DataLakeStore/accounts/read",
                "Microsoft.Compute/*",
                "Microsoft.Network/*",
                "Microsoft.Storage/*",
                "Microsoft.Resources/*"));
        properties.setPermissions(Arrays.asList(permission));
        role.setProperties(properties);
        return role;
    }

    //CHECKSTYLE:OFF
    private boolean validateRoleActions(AzureRoleDefinition role) {
        Set<String> actions = new HashSet<>();
        Set<String> notActions = new HashSet<>();
        for (AzurePermission permission : role.getProperties().getPermissions()) {
            actions.addAll(permission.getActions());
            notActions.addAll(permission.getNotActions());
        }
        if (actions.contains("*")) {
            if (!notActions.contains("Microsoft.Compute/*")
                    && !notActions.contains("Microsoft.Authorization/*/read")
                    && !notActions.contains("Microsoft.DataLakeStore/accounts/read")
                    && !notActions.contains("Microsoft.Network/*")
                    && !notActions.contains("Microsoft.Storage/*")
                    && !notActions.contains("Microsoft.Resources/*")) {
                return true;
            }
        } else if (actions.contains("Microsoft.Compute/*")
                && actions.contains("Microsoft.Authorization/*/read")
                && actions.contains("Microsoft.DataLakeStore/accounts/read")
                && actions.contains("Microsoft.Network/*")
                && actions.contains("Microsoft.Storage/*")
                && actions.contains("Microsoft.Resources/*")) {
            return true;
        }

        return false;
    }
    //CHECKSTYLE:ON

    public enum RoleType {
        CONTRIBUTOR,
        CREATE_CUSTOM,
        REUSE_EXISTING
    }
}

