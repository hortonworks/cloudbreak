package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import java.util.List;

import org.openstack4j.api.OSClient;

import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.StackDependentPollerObject;

public final class OpenStackContext extends StackDependentPollerObject {

    private final List<String> resources;
    private final OSClient osClient;
    private final String status;

    public OpenStackContext(Stack stack, List<String> resources, OSClient osClient, String status) {
        super(stack);
        this.resources = resources;
        this.osClient = osClient;
        this.status = status;
    }

    public OSClient getOsClient() {
        return osClient;
    }

    public List<String> getResources() {
        return resources;
    }

    public String getSingleResource() {
        return resources.get(0);
    }

    public String getStatus() {
        return status;
    }
}
