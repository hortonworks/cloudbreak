package com.sequenceiq.it.util;

import java.util.Optional;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.clusterdefinition.responses.ClusterDefinitionV4ViewResponse;
import com.sequenceiq.cloudbreak.api.endpoint.v4.credentials.responses.CredentialV4Response;
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
        LOG.error("should replace the invalid environment value on stack deletion!");
        String environment = "suchAnInvalidValueWhichShouldBeReplacedAfterApiRefactor";
        cleanedUp = true;
        cloudbreakClient.stackV4Endpoint()
                .list(workspaceId, environment, false).getResponses()
                .stream()
                .filter(stack -> stack.getName().startsWith("it-"))
                .forEach(stack -> deleteStackAndWait(cloudbreakClient, workspaceId, stack.getName()));

        cloudbreakClient.clusterDefinitionV4Endpoint()
                .list(workspaceId)
                .getResponses()
                .stream()
                .filter(clusterDefinition -> clusterDefinition.getName().startsWith("it-"))
                .forEach(clusterDefinition -> deleteClusterDefinition(workspaceId, cloudbreakClient, clusterDefinition.getId()));

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

        cloudbreakClient.databaseV4Endpoint()
                .list(workspaceId, null, Boolean.FALSE)
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

    public void deleteClusterDefinition(Long workspaceId, CloudbreakClient cloudbreakClient, Long clusterDefinitionId) {
        if (clusterDefinitionId != null) {
            Optional<ClusterDefinitionV4ViewResponse> response = cloudbreakClient.clusterDefinitionV4Endpoint().list(workspaceId)
                    .getResponses()
                    .stream()
                    .filter(clusterDefinition -> clusterDefinition.getId().equals(clusterDefinitionId))
                    .findFirst();
            if (response.isPresent()) {
                cloudbreakClient.clusterDefinitionV4Endpoint().delete(workspaceId, response.get().getName());
            }
        }
    }

    public void deleteStackAndWait(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName) {
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

    public boolean deleteStack(CloudbreakClient cloudbreakClient, Long workspaceId, String stackName) {
        boolean result = false;
        if (stackName != null) {
            cloudbreakClient.stackV4Endpoint().delete(workspaceId, stackName, false, false);
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