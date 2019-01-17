package com.sequenceiq.it.util;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.blueprints.responses.BlueprintV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.filter.DatabaseV4ListFilter;
import com.sequenceiq.cloudbreak.api.endpoint.v4.database.responses.DatabaseV4Response;
import com.sequenceiq.cloudbreak.api.endpoint.v4.recipes.responses.RecipeViewV4Response;
import com.sequenceiq.cloudbreak.client.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakUtil;
import com.sequenceiq.it.cloudbreak.WaitResult;
import com.sequenceiq.it.cloudbreak.config.ITProps;

@Component
public class CleanupService {
    private static final Logger LOG = LoggerFactory.getLogger(CleanupService.class);

    private static final int DELETE_SLEEP = 30000;

    private boolean cleanedUp;

    @Value("${integrationtest.cleanup.retryCount}")
    private int cleanUpRetryCount;

    @Inject
    private ITProps itProps;

    public synchronized void deleteTestStacksAndResources(CloudbreakClient cloudbreakClient, Long workspaceId) {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;
        cloudbreakClient.stackV2Endpoint()
                .getStacksInDefaultWorkspace()
                .stream()
                .filter(stack -> stack.getName().startsWith("it-"))
                .forEach(stack -> deleteStackAndWait(cloudbreakClient, String.valueOf(stack.getId())));

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

        cloudbreakClient.credentialV4Endpoint()
                .list(workspaceId)
                .getResponses()
                .stream()
                .filter(c -> "AZURE".equals(c.getCloudPlatform()) ? c.getName().startsWith("its") : c.getName().startsWith("its-"))
                .forEach(credential -> deleteCredential(workspaceId, cloudbreakClient, credential.getId()));

        DatabaseV4ListFilter databaseV4ListFilter = new DatabaseV4ListFilter();
        databaseV4ListFilter.setAttachGlobal(Boolean.FALSE);
        cloudbreakClient.databaseV4Endpoint()
                .list(workspaceId, databaseV4ListFilter)
                .getResponses()
                .stream()
                .filter(rds -> rds.getName().startsWith("it-"))
                .forEach(rds -> deleteRdsConfigs(workspaceId, cloudbreakClient, rds.getId()));
    }

    public void deleteCredential(Long workspaceId, CloudbreakClient cloudbreakClient, Long credentialId) {
        if (credentialId != null) {
            Optional<CredentialV4Response> response = cloudbreakClient.credentialV4Endpoint().list(workspaceId)
                    .getResponses()
                    .stream()
                    .filter(credential -> credential.getId().equals(credentialId))
                    .findFirst();
            if (response.isPresent()) {
                cloudbreakClient.credentialV4Endpoint().delete(workspaceId, response.get().getName());
            }
        }
    }

    public void deleteBlueprint(Long workspaceId, CloudbreakClient cloudbreakClient, Long blueprintId) {
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

    public void deleteStackAndWait(CloudbreakClient cloudbreakClient, String stackId) {
        for (int i = 0; i < cleanUpRetryCount; i++) {
            if (deleteStack(cloudbreakClient, stackId)) {
                WaitResult waitResult = CloudbreakUtil.waitForStackStatus(cloudbreakClient, stackId, "DELETE_COMPLETED");
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

    public boolean deleteStack(CloudbreakClient cloudbreakClient, String stackId) {
        boolean result = false;
        if (stackId != null) {
            cloudbreakClient.stackV2Endpoint().deleteById(Long.valueOf(stackId), false, false);
            result = true;
        }
        return result;
    }

    public void deleteRecipe(Long workspaceId, CloudbreakClient cloudbreakClient, Long recipeId) {
        Optional<RecipeViewV4Response> response = cloudbreakClient.recipeV4Endpoint().list(workspaceId)
                .getResponses()
                .stream()
                .filter(recipe -> recipe.getId().equals(recipeId))
                .findFirst();
        if (response.isPresent()) {
            cloudbreakClient.recipeV4Endpoint().delete(workspaceId, response.get().getName());
        }
    }

    public void deleteImageCatalog(CloudbreakClient cloudbreakClient, String name, Long workspaceId) {
        cloudbreakClient.imageCatalogV4Endpoint().delete(workspaceId, name);
    }

    public void deleteRdsConfigs(Long workspaceId, CloudbreakClient cloudbreakClient, Long databaseId) {
        if (databaseId != null) {
            Optional<DatabaseV4Response> response = cloudbreakClient.databaseV4Endpoint().list(workspaceId, new DatabaseV4ListFilter())
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