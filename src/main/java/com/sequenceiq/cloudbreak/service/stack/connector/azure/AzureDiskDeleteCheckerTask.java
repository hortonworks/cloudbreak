package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.SimpleStatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.AddInstancesFailedException;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
public class AzureDiskDeleteCheckerTask extends SimpleStatusCheckerTask<AzureDiskRemoveDeleteTaskContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDiskDeleteCheckerTask.class);
    private static final int NOT_FOUND = 404;

    @Override
    public boolean checkStatus(AzureDiskRemoveDeleteTaskContext aRRPO) {
        MDCBuilder.buildMdcContext(aRRPO.getStack());
        LOGGER.info("Checking status of remove disk '{}' on '{}' stack.", aRRPO.getName(), aRRPO.getStack().getId());
        try {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, aRRPO.getName());
            HttpResponseDecorator deleteDisk = (HttpResponseDecorator) aRRPO.getAzureClient().deleteDisk(props);
            String requestId = (String) aRRPO.getAzureClient().getRequestId(deleteDisk);
            waitForFinishing(aRRPO.getAzureClient(), requestId);
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
    public void handleTimeout(AzureDiskRemoveDeleteTaskContext azureDiskRemoveDeleteTaskContext) {
        throw new AddInstancesFailedException(String.format(
                "Something went wrong. Remove of '%s' resource unsuccess in a reasonable timeframe on '%s' stack.",
                azureDiskRemoveDeleteTaskContext.getName(), azureDiskRemoveDeleteTaskContext.getStack().getId()));
    }

    @Override
    public String successMessage(AzureDiskRemoveDeleteTaskContext azureDiskRemoveDeleteTaskContext) {
        MDCBuilder.buildMdcContext(azureDiskRemoveDeleteTaskContext.getStack());
        return String.format("Azure resource '%s' is removed success on '%s' stack",
                azureDiskRemoveDeleteTaskContext.getName(), azureDiskRemoveDeleteTaskContext.getStack().getId());
    }

    @Override
    public void handleExit(AzureDiskRemoveDeleteTaskContext azureDiskRemoveDeleteTaskContext) {
        return;
    }
}
