package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import org.openstack4j.api.OSClient;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackContext;

public class ConsoleOutputContext extends StackContext {

    private final String instanceId;
    private final OSClient osClient;

    public ConsoleOutputContext(OSClient osClient, Stack stack, String instanceId) {
        super(stack);
        this.osClient = osClient;
        this.instanceId = instanceId;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public OSClient getOsClient() {
        return osClient;
    }
}
