package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

public class AzureInstanceStatusCheckerTask implements StatusCheckerTask<AzureInstances> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInstanceStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AzureInstances instances) {
        AzureClient azureClient = instances.getAzureClient();
        for (String instance : instances.getInstances()) {
            Map<String, String> vmContext = AzureStackUtil.createVMContext(instance);
            String status = instances.getStatus();
            if (!status.equals(azureClient.getVirtualMachineState(vmContext))) {
                LOGGER.info("Azure instance is not in {} status on stack: {}", status, instances.getStackId());
                return false;
            }
        }
        return true;
    }

    @Override
    public void handleTimeout(AzureInstances t) {
        throw new InternalServerException(String.format("Azure instances could not reach the desired status: %s on stack: %s", t, t.getStackId()));
    }

    @Override
    public String successMessage(AzureInstances t) {
        return String.format("Azure instances successfully reached status: %s on stack: %s", t.getStatus(), t.getStackId());
    }

}
