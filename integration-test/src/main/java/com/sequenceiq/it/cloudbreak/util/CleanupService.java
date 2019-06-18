package com.sequenceiq.it.cloudbreak.util;

import static com.sequenceiq.cloudbreak.client.CloudbreakUserCrnClient.CloudbreakEndpoint;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprint.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;

@Component
public class CleanupService {

    private static final Logger LOG = LoggerFactory.getLogger(CleanupService.class);

    private static final int DELETE_SLEEP = 30000;

    private boolean cleanedUp;

    @Value("${integrationtest.cleanup.retryCount}")
    private int cleanUpRetryCount;

    public synchronized void deleteTestStacksAndResources(CloudbreakEndpoint cloudbreakClient, Long workspaceId) {
        if (cleanedUp) {
            return;
        }
        LOG.error("should replace the invalid environment value on stack deletion!");
        String environment = "suchAnInvalidValueWhichShouldBeReplacedAfterApiRefactor";
        cleanedUp = true;
        cloudbreakClient.stackV4Endpoint()
                .list(workspaceId, environment, false).getResponses()
                .stream()
                .filter(stack -> stack.getName().startsWith("it-"))
                .forEach(stack -> deleteStackAndWait(cloudbreakClient, workspaceId, stack.getName()));

        cloudbreakClient.blueprintV4Endpoint()
                .list(workspaceId)
                .getResponses()
                .stream()
                .filter(blueprint -> blueprint.getName().startsWith("it-"))
                .forEach(blueprint -> deleteBlueprint(workspaceId, cloudbreakClient, blueprint.getId()));

        cloudbreakClient.recipeV4Endpoint()
                .list(workspaceId)
                .getResponses()
                .stream()
                .filter(recipe -> recipe.getName().startsWith("it-"))
                .forEach(recipe -> deleteRecipe(workspaceId, cloudbreakClient, recipe.getId()));

        cloudbreakClient.databaseV4Endpoint()
                .list(workspaceId, null, Boolean.FALSE)
                .getResponses()
                .stream()
                .filter(rds -> rds.getName().startsWith("it-"))
                .forEach(rds -> deleteRdsConfigs(workspaceId, cloudbreakClient, rds.getId()));
    }

    public void deleteBlueprint(Long workspaceId, CloudbreakEndpoint cloudbreakClient, Long blueprintId) {
        if (blueprintId != null) {
            Optional<BlueprintV4ViewResponse> response = cloudbreakClient.blueprintV4Endpoint().list(workspaceId)
                    .getResponses()
                    .stream()
                    .filter(blueprint -> blueprint.getId().equals(blueprintId))
                    .findFirst();
            if (response.isPresent()) {
                cloudbreakClient.blueprintV4Endpoint().delete(workspaceId, response.get().getName());
            }
        }
    }

    public void deleteStackAndWait(CloudbreakEndpoint cloudbreakClient, Long workspaceId, String stackName) {
        for (int i = 0; i < cleanUpRetryCount; i++) {
            if (deleteStack(cloudbreakClient, workspaceId, stackName)) {
                WaitResult waitResult = CloudbreakUtil.waitForStackStatus(cloudbreakClient, workspaceId, stackName, "DELETE_COMPLETED");
                if (waitResult == WaitResult.SUCCESSFUL) {
                    break;
                }
                try {
                    Thread.sleep(DELETE_SLEEP);
                } catch (InterruptedException e) {
                    LOG.warn("interrupted ex", e);
                }
            }
        }
    }

    public boolean deleteStack(CloudbreakEndpoint cloudbreakClient, Long workspaceId, String stackName) {
        boolean result = false;
        if (stackName != null) {
            cloudbreakClient.stackV4Endpoint().delete(workspaceId, stackName, false, false);
            result = true;
        }
        return result;
    }

    public void deleteRecipe(Long workspaceId, CloudbreakEndpoint cloudbreakClient, Long recipeId) {
        Optional<RecipeViewV4Response> response = cloudbreakClient.recipeV4Endpoint().list(workspaceId)
                .getResponses()
                .stream()
                .filter(recipe -> recipe.getId().equals(recipeId))
                .findFirst();
        if (response.isPresent()) {
            cloudbreakClient.recipeV4Endpoint().delete(workspaceId, response.get().getName());
        }
    }

    public void deleteImageCatalog(CloudbreakEndpoint cloudbreakClient, String name, Long workspaceId) {
        cloudbreakClient.imageCatalogV4Endpoint().delete(workspaceId, name);
    }

    public void deleteRdsConfigs(Long workspaceId, CloudbreakEndpoint cloudbreakClient, Long databaseId) {
        if (databaseId != null) {
            Optional<DatabaseV4Response> response = cloudbreakClient.databaseV4Endpoint().list(workspaceId, null, Boolean.FALSE)
                    .getResponses()
                    .stream()
                    .filter(database -> database.getId().equals(databaseId))
                    .findFirst();
            if (response.isPresent()) {
                cloudbreakClient.databaseV4Endpoint().delete(workspaceId, response.get().getName());
            }
        }
    }
}