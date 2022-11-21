package com.sequenceiq.cloudbreak.cloud.azure.context;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.azure.AzureConstants;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;

@Service
public class AzureContextBuilder implements ResourceContextBuilder<AzureContext> {

    private static final int PARALLEL_RESOURCE_REQUEST = 30;

    @Override
    public AzureContext contextInit(CloudContext context, AuthenticatedContext auth, Network network, boolean build) {
        Location location = context.getLocation();
        return new AzureContext(context.getName(), location, PARALLEL_RESOURCE_REQUEST, build);
    }

    @Override
    public Platform platform() {
        return AzureConstants.PLATFORM;
    }

    @Override
    public Variant variant() {
        return AzureConstants.VARIANT;
    }
}
