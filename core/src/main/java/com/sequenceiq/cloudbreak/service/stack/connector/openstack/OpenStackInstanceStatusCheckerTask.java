package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import java.util.List;

import org.openstack4j.api.OSClient;
import org.openstack4j.model.compute.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackBasedStatusCheckerTask;

@Component
public class OpenStackInstanceStatusCheckerTask extends StackBasedStatusCheckerTask<OpenStackContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackInstanceStatusCheckerTask.class);

    @Override
    public boolean checkStatus(OpenStackContext context) {
        Stack stack = context.getStack();
        OSClient osClient = context.getOsClient();
        List<String> resources = context.getResources();
        String desiredState = context.getStatus();
        LOGGER.info("Checking {} OpenStack instances to reach: {} state", resources.size(), desiredState);
        for (String resource : resources) {
            Server instance = osClient.compute().servers().get(resource);
            String status = instance.getStatus().toString();
            if (!status.equalsIgnoreCase(desiredState)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void handleTimeout(OpenStackContext context) {
        throw new OpenStackOperationTimeoutException("OpenStack instances didn't reach the desired state in the given time frame");
    }

    @Override
    public String successMessage(OpenStackContext context) {
        return "OpenStack instances successfully reached the desired state: " + context.getStatus();
    }

}
