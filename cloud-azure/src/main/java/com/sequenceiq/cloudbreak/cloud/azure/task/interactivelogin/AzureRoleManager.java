package com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin;

import static com.sequenceiq.cloudbreak.cloud.azure.task.interactivelogin.AzureInteractiveLoginStatusCheckerTask.AZURE_MANAGEMENT;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.Response.Status.Family;

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
                    LOGGER.info("Role already exists in Azure with the name: " + roleName);
                    throw new InteractiveLoginUnrecoverableException("Role already exists in Azure with the name: " + roleName);
                }
                roleId = createRole(accessToken, subscriptionId, roleName);
                break;
            // Scenario #3: reuse already existing role
            case REUSE_EXISTING:
                existingRole = getRoleDefinitonByName(subscriptionId, accessToken, roleName);
                if (existingRole == null) {
                    LOGGER.info("Role does not exist in Azure with the name: " + roleName);
                    throw new InteractiveLoginUnrecoverableException("Role does not exist in Azure with the name: " + roleName);
                }
                if (!validateRoleActions(existingRole)) {
                    LOGGER.info(existingRole.getProperties().getRoleName()
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
        Builder request = resource.path("subscriptions/" + subscriptionId + "/providers/Microsoft.Authorization/roleAssignments/"
                + UUID.randomUUID()).queryParam("api-version", "2015-07-01").request();
        request.accept(MediaType.APPLICATION_JSON);

        request.header("Authorization", "Bearer " + accessToken);

        JsonObject properties = new JsonObject();
        properties.addProperty("roleDefinitionId", roleDefinitionId);
        properties.addProperty("principalId", principalObjectId);

        JsonObject jsonObject = new JsonObject();
        jsonObject.add("properties", properties);

        try (Response response = request.put(Entity.entity(jsonObject.toString(), MediaType.APPLICATION_JSON))) {
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                String errorResponse = response.readEntity(String.class);
                LOGGER.info("Assign role request error - status code: {} - error message: {}", response.getStatus(), errorResponse);
                if (response.getStatusInfo().getStatusCode() == Status.FORBIDDEN.getStatusCode()) {
                    throw new InteractiveLoginException("You don't have enough permissions to assign roles, please contact with your administrator");
                } else {
                    try {
                        String errorMessage = new ObjectMapper().readTree(errorResponse).get("error").get("message").asText();
                        throw new InteractiveLoginException("Failed to assing role: " + errorMessage);
                    } catch (IOException e) {
                        throw new InteractiveLoginException("Failed to assing role (status " + response.getStatus() + "): " + errorResponse);
                    }
                }
            } else {
                LOGGER.debug("Role assigned successfully. subscriptionId '{}', roleDefinitionId {}, principalObjectId {}",
                        subscriptionId, roleDefinitionId, principalObjectId);
            }
        }
    }

    private String getContributorRoleDefinitionId(String subscriptionId, String accessToken) throws InteractiveLoginException {
        List<AzureRoleDefinition> roleDefinitions = getRoleDefinitions(subscriptionId, accessToken, "roleName%20eq%20'" + CONTRIBUTOR_ROLE + '\'');
        if (roleDefinitions.size() == 1) {
            return roleDefinitions.get(0).getId();
        } else {
            throw new InteractiveLoginException("Get 'Contributor' role name and definition id request returned: " + roleDefinitions.size() + " responses!");
        }
    }

    private AzureRoleDefinition getRoleDefinitonByName(String subscriptionId, String accessToken, String roleName) throws InteractiveLoginException {
        List<AzureRoleDefinition> roleList = getRoleDefinitions(subscriptionId, accessToken, "roleName%20eq%20'" + roleName + '\'');
        if (!roleList.isEmpty()) {
            return roleList.get(0);
        }
        return null;
    }

    private List<AzureRoleDefinition> getRoleDefinitions(String subscriptionId, String accessToken, String filter) throws InteractiveLoginException {
        Client client = ClientBuilder.newClient();
        WebTarget resource = client.target(AZURE_MANAGEMENT);
        Builder request = resource.path("subscriptions/" + subscriptionId + "/providers/Microsoft.Authorization/roleDefinitions")
                .queryParam("$filter", filter)
                .queryParam("api-version", "2015-07-01")
                .request();
        request.accept(MediaType.APPLICATION_JSON);

        request.header("Authorization", "Bearer " + accessToken);
        try (Response response = request.get()) {
            if (response.getStatusInfo().getFamily() == Family.SUCCESSFUL) {
                AzureRoleDefinitionListResponse azureRoleDefinitionListResponse = response.readEntity(AzureRoleDefinitionListResponse.class);
                LOGGER.debug("Role definitions retrieved:" + azureRoleDefinitionListResponse.getValue());
                return azureRoleDefinitionListResponse.getValue();
            } else {
                String errorResponse = response.readEntity(String.class);
                LOGGER.info("Get role definition request with filter: {}, failed: {}", filter, errorResponse);
                if (Status.FORBIDDEN.getStatusCode() == response.getStatus()) {
                    throw new InteractiveLoginException("You have no permission to access Active Directory roles, please contact with your administrator");
                } else {
                    try {
                        String errorMessage = new ObjectMapper().readTree(errorResponse).get("error").get("message").asText();
                        throw new InteractiveLoginException("Get role definition request with filter: " + filter + " failed: " + errorMessage);
                    } catch (IOException e) {
                        throw new InteractiveLoginException("Get role definition request with filter: " + filter + " failed:" + errorResponse);
                    }
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
        Builder request = resource.path(roleDefinitionId).queryParam("api-version", "2015-07-01").request();
        request.accept(MediaType.APPLICATION_JSON);

        request.header("Authorization", "Bearer " + accessToken);

        String customRole;
        try {
            customRole = new ObjectMapper().writeValueAsString(assembleCustomRole(subscriptionId, roleName));
        } catch (JsonProcessingException e) {
            LOGGER.info("Create role request processing error", e);
            throw new InteractiveLoginException("Create role request error - " + e.getMessage());
        }

        try (Response response = request.put(Entity.entity(customRole, MediaType.APPLICATION_JSON))) {
            if (response.getStatusInfo().getFamily() != Family.SUCCESSFUL) {
                String errorResponse = response.readEntity(String.class);
                LOGGER.info("Create role request error - status code: {} - error message: {}", response.getStatus(), errorResponse);
                if (Status.FORBIDDEN.getStatusCode() == response.getStatus()) {
                    throw new InteractiveLoginException("You don't have enough permissions to create role, please contact with your administrator");
                } else {
                    try {
                        String errorMessage = new ObjectMapper().readTree(errorResponse).get("error").get("message").asText();
                        throw new InteractiveLoginException("Create role request error - status code: "
                                + response.getStatus() + " - error message: " + errorMessage);
                    } catch (IOException e) {
                        throw new InteractiveLoginException("Create role request error - status code: "
                                + response.getStatus() + " - error message: " + errorResponse);
                    }
                }
            }
        }
        LOGGER.debug("Role " + roleName +  " created successfully");
        return roleDefinitionId;
    }

    private AzureRoleDefinition assembleCustomRole(String subscriptionId, String roleName) {

        String uuid = UUID.randomUUID().toString();
        AzureRoleDefinition role = new AzureRoleDefinition();
        role.setName(uuid);

        AzureRoleDefinitionProperties properties = new AzureRoleDefinitionProperties();
        properties.setRoleName(roleName);
        properties.setDescription("Custom role for cluster management via Cloudbreak client");
        properties.setAssignableScopes(Collections.singletonList("/subscriptions/" + subscriptionId));
        properties.setType("CustomRole");
        AzurePermission permission = new AzurePermission();
        permission.setActions(Arrays.asList(
                "Microsoft.Authorization/*/read",
                "Microsoft.DataLakeStore/accounts/read",
                "Microsoft.Compute/*",
                "Microsoft.Network/*",
                "Microsoft.Storage/*",
                "Microsoft.Resources/*"));
        properties.setPermissions(Collections.singletonList(permission));
        role.setProperties(properties);
        return role;
    }

    //CHECKSTYLE:OFF
    private boolean validateRoleActions(AzureRoleDefinition role) {
        Collection<String> actions = new HashSet<>();
        Collection<String> notActions = new HashSet<>();
        for (AzurePermission permission : role.getProperties().getPermissions()) {
            actions.addAll(permission.getActions());
            notActions.addAll(permission.getNotActions());
        }
        return actions.contains("*")
                ? !notActions.contains("Microsoft.Compute/*")
                && !notActions.contains("Microsoft.Authorization/*/read")
                && !notActions.contains("Microsoft.DataLakeStore/accounts/read")
                && !notActions.contains("Microsoft.Network/*")
                && !notActions.contains("Microsoft.Storage/*")
                && !notActions.contains("Microsoft.Resources/*")
                : actions.contains("Microsoft.Compute/*")
                && actions.contains("Microsoft.Authorization/*/read")
                && actions.contains("Microsoft.DataLakeStore/accounts/read")
                && actions.contains("Microsoft.Network/*")
                && actions.contains("Microsoft.Storage/*")
                && actions.contains("Microsoft.Resources/*");

    }
    //CHECKSTYLE:ON

    public enum RoleType {
        CONTRIBUTOR,
        CREATE_CUSTOM,
        REUSE_EXISTING
    }
}

