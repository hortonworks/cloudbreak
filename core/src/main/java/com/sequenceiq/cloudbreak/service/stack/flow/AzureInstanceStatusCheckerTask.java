package com.sequenceiq.cloudbreak.service.stack.flow;

import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.NAME;
import static com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil.SERVICENAME;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.service.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class AzureInstanceStatusCheckerTask extends StackBasedStatusCheckerTask<AzureInstances> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInstanceStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AzureInstances instances) {
        AzureClient azureClient = instances.getAzureClient();
        for (String instance : instances.getInstances()) {
            Map<String, String> vmContext = createVMContext(instance);
            String status = instances.getStatus();
            if (!status.equals(azureClient.getVirtualMachineState(vmContext))) {
                LOGGER.info("Azure instance is not in {} status on stack.", status);
                return false;
            }
        }
        return true;
    }

    private Map<String, String> createVMContext(String vmName) {
        Map<String, String> context = new HashMap<>();
        context.put(SERVICENAME, vmName);
        context.put(NAME, vmName);
        return context;
    }

    @Override
    public void handleTimeout(AzureInstances t) {
        throw new CloudbreakServiceException(String.format("Azure instances could not reach the desired status: %s on stack.", t));
    }

    @Override
    public String successMessage(AzureInstances t) {
        return String.format("Azure instances successfully reached status: %s on stack.", t.getStatus());
    }

}
