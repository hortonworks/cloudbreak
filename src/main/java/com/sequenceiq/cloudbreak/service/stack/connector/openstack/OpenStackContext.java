package com.sequenceiq.cloudbreak.service.stack.connector.openstack;

import java.util.List;

import org.openstack4j.api.OSClient;

import com.sequenceiq.cloudbreak.domain.Stack;

public final class OpenStackContext {

    private final Stack stack;
    private final List<String> resources;
    private final OSClient osClient;
    private final String status;

    public OpenStackContext(Stack stack, List<String> resources, OSClient osClient, String status) {
        this.stack = stack;
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

    public Stack getStack() {
        return stack;
    }

    public String getStatus() {
        return status;
    }
}
