package com.sequenceiq.cloudbreak.cloud.yarn;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;

public class YarnContextBuilder implements ResourceContextBuilder<YarnContext> {
    public static final int PARALLEL_RESOURCE_REQUEST = 30;

    @Override
    public YarnContext contextInit(CloudContext context,
                                   AuthenticatedContext auth, Network network,
                                   List<CloudResource> resources, boolean build) {
        return new YarnContext(context.getName(), context.getLocation(),
                PARALLEL_RESOURCE_REQUEST, build);
    }

    @Override
    public Platform platform() {
        return YarnConstants.YARN_PLATFORM;
    }

    @Override
    public Variant variant() {
        return YarnConstants.YARN_VARIANT;
    }
}
