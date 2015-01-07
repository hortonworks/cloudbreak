package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.SimpleStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.AddInstancesFailedException;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
public class AzureCloudServiceDeleteCheckerTask extends SimpleStatusCheckerTask<AzureCloudServiceDeleteTaskContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureCloudServiceDeleteCheckerTask.class);
    private static final int NOT_FOUND = 404;

    @Autowired
    private StackRepository stackRepository;

    @Override
    public boolean checkStatus(AzureCloudServiceDeleteTaskContext aRRPO) {
        MDCBuilder.buildMdcContext(aRRPO.getStack());
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
            throw new InternalServerException("Azure resource timeout");
        }
    }

    @Override
    public void handleTimeout(AzureCloudServiceDeleteTaskContext azureDiskRemoveReadyPollerObject) {
        throw new AddInstancesFailedException(String.format(
                "Something went wrong. Remove of '%s' resource unsuccess in a reasonable timeframe on '%s' stack.",
                azureDiskRemoveReadyPollerObject.getName(), azureDiskRemoveReadyPollerObject.getStack().getId()));
    }

    @Override
    public String successMessage(AzureCloudServiceDeleteTaskContext azureDiskRemoveReadyPollerObject) {
        MDCBuilder.buildMdcContext(azureDiskRemoveReadyPollerObject.getStack());
        return String.format("Azure resource '%s' is removed success on '%s' stack",
                azureDiskRemoveReadyPollerObject.getName(), azureDiskRemoveReadyPollerObject.getStack().getId());
    }

    @Override
    public void handleExit(AzureCloudServiceDeleteTaskContext azureCloudServiceDeleteTaskContext) {
        return;
    }
}
