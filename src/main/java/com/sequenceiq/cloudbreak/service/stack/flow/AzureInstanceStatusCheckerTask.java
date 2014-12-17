package com.sequenceiq.cloudbreak.service.stack.flow;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.controller.InternalServerException;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.StatusCheckerTask;
import com.sequenceiq.cloudbreak.service.stack.connector.azure.AzureStackUtil;

@Component
@Scope("prototype")
public class AzureInstanceStatusCheckerTask implements StatusCheckerTask<AzureInstances> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInstanceStatusCheckerTask.class);

    @Autowired
    private StackRepository stackRepository;

    @Override
    public boolean checkStatus(AzureInstances instances) {
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
    public void handleTimeout(AzureInstances t) {
        throw new InternalServerException(String.format("Azure instances could not reach the desired status: %s on stack.", t));
    }

    @Override
    public boolean exitPoller(AzureInstances azureInstances) {
        try {
            stackRepository.findById(azureInstances.getStack().getId());
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

    @Override
    public String successMessage(AzureInstances t) {
        MDCBuilder.buildMdcContext(t.getStack());
        return String.format("Azure instances successfully reached status: %s on stack.", t.getStatus());
    }

}
