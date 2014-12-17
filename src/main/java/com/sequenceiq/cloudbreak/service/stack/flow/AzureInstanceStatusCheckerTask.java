package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

@Component
@Scope("prototype")
public class AzureInstanceStatusCheckerTask implements StatusCheckerTask<AzureInstancesPollerObject> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInstanceStatusCheckerTask.class);

    @Override
    public boolean checkStatus(AzureInstancesPollerObject instances) {
        MDCBuilder.buildMdcContext(instances.getStack());
        AzureClient azureClient = instances.getAzureClient();
        for (String instance : instances.getInstances()) {
            Map<String, String> vmContext = AzureStackUtil.createVMContext(instance);
            String status = instances.getStatus();
            if (!status.equals(azureClient.getVirtualMachineState(vmContext))) {
                LOGGER.info("Azure instance is not in {} status on stack.", status);
                return false;
            }
        }
        return true;
    }

    @Override
    public void handleTimeout(AzureInstancesPollerObject t) {
        throw new InternalServerException(String.format("Azure instances could not reach the desired status: %s on stack.", t));
    }

    @Override
    public String successMessage(AzureInstancesPollerObject t) {
        MDCBuilder.buildMdcContext(t.getStack());
        return String.format("Azure instances successfully reached status: %s on stack.", t.getStatus());
    }

    @Override
    public void handleExit(AzureInstancesPollerObject azureInstances) {
        return;
    }

}
