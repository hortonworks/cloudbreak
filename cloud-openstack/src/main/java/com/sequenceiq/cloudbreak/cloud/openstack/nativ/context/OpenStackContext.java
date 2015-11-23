package com.sequenceiq.cloudbreak.cloud.openstack.nativ.context;

import org.openstack4j.api.OSClient;

import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class OpenStackContext extends ResourceBuilderContext {
    private OSClient osClient;

    public OpenStackContext(String name, Location location, int parallelResourceRequest, boolean build) {
        super(name, location, parallelResourceRequest, build);
    }
}
