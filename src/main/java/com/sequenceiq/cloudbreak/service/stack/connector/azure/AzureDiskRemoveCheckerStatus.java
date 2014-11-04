package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.AddInstancesFailedException;

import groovyx.net.http.HttpResponseDecorator;
import groovyx.net.http.HttpResponseException;

@Component
public class AzureDiskRemoveCheckerStatus implements StatusCheckerTask<AzureDiskRemoveReadyPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureDiskRemoveCheckerStatus.class);
    private static final int NOT_FOUND = 404;

    @Override
    public boolean checkStatus(AzureDiskRemoveReadyPollerObject aRRPO) {
        LOGGER.info("Checking status of remove disk '{}' on '{}' stack.", aRRPO.getName(), aRRPO.getStackId());
        try {
            Map<String, String> props = new HashMap<>();
            props.put(NAME, aRRPO.getName());
            HttpResponseDecorator deleteDisk = (HttpResponseDecorator) aRRPO.getAzureClient().deleteDisk(props);
            String requestId = (String) aRRPO.getAzureClient().getRequestId(deleteDisk);
            aRRPO.getAzureClient().waitUntilComplete(requestId);
        } catch (HttpResponseException ex) {
            if (ex.getStatusCode() != NOT_FOUND) {
                return false;
            }
        } catch (Exception ex) {
            return false;
        }
        return true;
    }

    @Override
    public void handleTimeout(AzureDiskRemoveReadyPollerObject azureDiskRemoveReadyPollerObject) {
        throw new AddInstancesFailedException(String.format(
                "Something went wrong. Remove of '%s' resource unsuccess in a reasonable timeframe on '%s' stack.",
                azureDiskRemoveReadyPollerObject.getName(), azureDiskRemoveReadyPollerObject.getStackId()));
    }

    @Override
    public String successMessage(AzureDiskRemoveReadyPollerObject azureDiskRemoveReadyPollerObject) {
        return String.format("Azure resource '%s' is removed success on '%s' stack",
                azureDiskRemoveReadyPollerObject.getName(), azureDiskRemoveReadyPollerObject.getStackId());
    }
}
