package com.sequenceiq.cloudbreak.cloud.openstack.context;

import java.util.ArrayList;
import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.openstack.common.OpenStackConstants;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;

@Service
public class OpenStackContextBuilder implements ResourceContextBuilder<OpenStackContext> {
    private static final int PARALLEL_RESOURCE_REQUEST = 30;

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenStackContextBuilder.class);

    @Override
    public OpenStackContext contextInit(CloudContext cloudContext, AuthenticatedContext auth, Network network, boolean build) {
        OpenStackContext openStackContext = new OpenStackContext(cloudContext.getName(), cloudContext.getLocation(),
                PARALLEL_RESOURCE_REQUEST, build);
        openStackContext.putParameter(OpenStackConstants.FLOATING_IP_IDS, Collections.synchronizedList(new ArrayList<String>()));
        if (network != null) {
            openStackContext.putParameter(OpenStackConstants.PUBLIC_NET_ID, network.getStringParameter(OpenStackConstants.PUBLIC_NET_ID));
        }

        return openStackContext;
    }

    @Override
    public Platform platform() {
        return OpenStackConstants.OPENSTACK_PLATFORM;
    }

    @Override
    public Variant variant() {
        return OpenStackConstants.OPENSTACK_VARIANT;
    }
}
