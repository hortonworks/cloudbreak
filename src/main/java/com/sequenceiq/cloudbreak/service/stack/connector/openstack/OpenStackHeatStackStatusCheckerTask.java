package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import org.openstack4j.api.OSClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class OpenStackHeatStackStatusCheckerTask extends StackBasedStatusCheckerTask<OpenStackContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackHeatStackStatusCheckerTask.class);
    private static final String FAILED_STATE = "FAILED";

    @Override
    public boolean checkStatus(OpenStackContext context) {
        Stack stack = context.getStack();
        MDCBuilder.buildMdcContext(stack);
        OSClient osClient = context.getOsClient();
        String heatStackId = context.getSingleResource();
        String stackName = stack.getName();
        String desiredState = context.getStatus();
        LOGGER.info("Checking OpenStack Heat stack status of: {} to reach: {}", stackName, desiredState);
        org.openstack4j.model.heat.Stack heatStack = osClient.heat().stacks().getDetails(stackName, heatStackId);
        String status = heatStack.getStatus();
        if (status.endsWith(FAILED_STATE)) {
            throw new HeatStackFailedException(heatStack.getStackStatusReason());
        }
        return status.equalsIgnoreCase(desiredState);
    }

    @Override
    public void handleTimeout(OpenStackContext context) {
        throw new HeatStackFailedException(
                String.format("Heat stack didn't reach the desired state in the given time frame, heat id: %s", context.getSingleResource()));
    }

    @Override
    public String successMessage(OpenStackContext context) {
        return "Heat stack successfully reached the desired state: " + context.getStatus();
    }

}
