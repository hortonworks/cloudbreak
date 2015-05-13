package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
public class AzureCloudServiceDeleteTask implements StatusCheckerTask<AzureCloudServiceDeleteTaskContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudServiceDeleteTask.class);
    private static final int NOT_FOUND = 404;

    @Override
    public boolean checkStatus(AzureCloudServiceDeleteTaskContext aRRPO) {
        LOGGER.info("Checking status of remove cloud service '{}'.", aRRPO.getName());
        try {
            Map<String, String> props = new HashMap<>();
            props.put(SERVICENAME, aRRPO.getCommonName());
            props.put(NAME, aRRPO.getName());
            AzureClient azureClient = aRRPO.getAzureClient();
            HttpResponseDecorator deleteCloudServiceResult = (HttpResponseDecorator) azureClient.deleteCloudService(props);
            String requestId = (String) azureClient.getRequestId(deleteCloudServiceResult);
            waitForFinishing(azureClient, requestId);
        } catch (HttpResponseException ex) {
            if (ex.getStatusCode() != NOT_FOUND) {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    private void waitForFinishing(AzureClient azureClient, String requestId) {
        boolean finished = azureClient.waitUntilComplete(requestId);
        if (!finished) {
            throw new AzureResourceException(String.format("Azure resource deletion timed out. Request id: '%s'", requestId));
        }
    }

    @Override
    public void handleTimeout(AzureCloudServiceDeleteTaskContext azureDiskRemoveReadyPollerObject) {
        throw new AzureResourceException(String.format(
                "Could not remove azure resource '%s', stackId '%s'; operation timed out.",
                azureDiskRemoveReadyPollerObject.getName(), azureDiskRemoveReadyPollerObject.getStack().getId()));
    }

    @Override
    public String successMessage(AzureCloudServiceDeleteTaskContext azureDiskRemoveReadyPollerObject) {
        return String.format("Azure resource '%s' is successfully removed. Stack id: '%s'",
                azureDiskRemoveReadyPollerObject.getName(), azureDiskRemoveReadyPollerObject.getStack().getId());
    }

    @Override
    public boolean exitPolling(AzureCloudServiceDeleteTaskContext azureCloudServiceDeleteTaskContext) {
        return false;
    }
}
