package com.sequenceiq.cloudbreak.cloud.openstack.nativ.context;

import org.openstack4j.api.OSClient;

import com.sequenceiq.cloudbreak.cloud.event.context.ResourceBuilderContext;

public class OpenStackContext  extends ResourceBuilderContext {
    private OSClient osClient;

    public OpenStackContext(String name, String region, int parallelResourceRequest, boolean build) {
        super(name, region, parallelResourceRequest, build);
    }
}
