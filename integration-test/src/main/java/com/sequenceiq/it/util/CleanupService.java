package com.sequenceiq.it.util;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

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
        cloudbreakClient.stackV2Endpoint()
                .getStacksInDefaultWorkspace()
                .stream()
                .filter(stack -> stack.getName().startsWith("it-"))
                .forEach(stack -> deleteStackAndWait(cloudbreakClient, String.valueOf(stack.getId())));

        cloudbreakClient.blueprintEndpoint()
                .getPrivates()
                .stream()
                .filter(blueprint -> blueprint.getName().startsWith("it-"))
                .forEach(blueprint -> deleteBlueprint(cloudbreakClient, String.valueOf(blueprint.getId())));

        cloudbreakClient.credentialEndpoint()
                .getPrivates()
                .stream()
                .filter(c -> "AZURE".equals(c.getCloudPlatform()) ? c.getName().startsWith("its") : c.getName().startsWith("its-"))
                .forEach(credential -> deleteCredential(cloudbreakClient, String.valueOf(credential.getId())));
    }

    public void deleteCredential(CloudbreakClient cloudbreakClient, String credentialId) {
        if (credentialId != null) {
            cloudbreakClient.credentialEndpoint().delete(Long.valueOf(credentialId));
        }
    }

    public void deleteBlueprint(CloudbreakClient cloudbreakClient, String blueprintId) {
        if (blueprintId != null) {
            cloudbreakClient.blueprintEndpoint().delete(Long.valueOf(blueprintId));
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

    public void deleteImageCatalog(CloudbreakClient cloudbreakClient, String name) {
        cloudbreakClient.imageCatalogEndpoint().deletePublic(name);
    }
}
