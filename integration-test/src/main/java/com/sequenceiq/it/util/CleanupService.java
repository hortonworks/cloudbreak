package com.sequenceiq.it.util;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.users.WorkspaceResponse;
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

    public synchronized void deleteTestStacksAndResources(CloudbreakClient cloudbreakClient) {
        if (cleanedUp) {
            return;
        }
        cleanedUp = true;
        cloudbreakClient.workspaceV3Endpoint().getAll()
                .stream()
                .filter(workspace -> workspace.getName().startsWith("its-"))
                .map(WorkspaceResponse::getId)
                .forEach(workspaceId -> cloudbreakClient.stackV3Endpoint().listByWorkspace(workspaceId).stream()
                                .filter(stack -> stack.getName().startsWith("it-"))
                                .forEach(stack -> deleteStackAndWait(cloudbreakClient, workspaceId, stack.getName()))
                );

        cloudbreakClient.blueprintEndpoint()
                .getPrivates()
                .stream()
                .filter(blueprint -> blueprint.getName().startsWith("it-"))
                .forEach(blueprint -> deleteBlueprint(cloudbreakClient, String.valueOf(blueprint.getId())));

        cloudbreakClient.recipeEndpoint()
                .getPrivates()
                .stream()
                .filter(recipe -> recipe.getName().startsWith("it-"))
                .forEach(recipe -> deleteRecipe(cloudbreakClient, recipe.getId()));

        cloudbreakClient.workspaceV3Endpoint().getAll()
                .stream()
                .filter(workspace -> workspace.getName().startsWith("its-"))
                .map(WorkspaceResponse::getId)
                .forEach(workspaceId -> cloudbreakClient.credentialV3Endpoint().listByWorkspace(workspaceId).stream()
                        .filter(c -> "AZURE".equals(c.getCloudPlatform()) ? c.getName().startsWith("its") : c.getName().startsWith("its-"))
                        .forEach(credential -> deleteCredential(cloudbreakClient, workspaceId, credential.getName()))
                );

        cloudbreakClient.rdsConfigEndpoint()
                .getPrivates()
                .stream()
                .filter(rds -> rds.getName().startsWith("it-"))
                .forEach(rds -> deleteRdsConfigs(cloudbreakClient, rds.getId().toString()));

        cloudbreakClient.workspaceV3Endpoint().getAll()
                .stream()
                .filter(workspace -> workspace.getName().startsWith("its-"))
                .forEach(workspace -> deleteWorkspace(cloudbreakClient, workspace.getName()));
    }

    public void deleteWorkspace(CloudbreakClient cloudbreakClient, String name) {
        if (name != null) {
            cloudbreakClient.workspaceV3Endpoint().deleteByName(name);
        }
    }

    public void deleteCredential(CloudbreakClient cloudbreakClient, Long workspaceId, String credentialName) {
        if (credentialName != null && workspaceId != null) {
            cloudbreakClient.credentialV3Endpoint().deleteInWorkspace(workspaceId, credentialName);
        }
    }

    public void deleteBlueprint(CloudbreakClient cloudbreakClient, String blueprintId) {
        if (blueprintId != null) {
            cloudbreakClient.blueprintEndpoint().delete(Long.valueOf(blueprintId));
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
        if (stackName != null && workspaceId != null) {
            cloudbreakClient.stackV3Endpoint().deleteInWorkspace(workspaceId, stackName, false, false);
            result = true;
        }
        return result;
    }

    public void deleteRecipe(CloudbreakClient cloudbreakClient, Long recipeId) {
        cloudbreakClient.recipeEndpoint().delete(recipeId);
    }

    public void deleteImageCatalog(CloudbreakClient cloudbreakClient, String name) {
        cloudbreakClient.imageCatalogEndpoint().deletePublic(name);
    }

    public void deleteRdsConfigs(CloudbreakClient cloudbreakClient, String rdsConfigId) {
        if (rdsConfigId != null) {
            cloudbreakClient.rdsConfigEndpoint().delete(Long.valueOf(rdsConfigId));
        }
    }
}
