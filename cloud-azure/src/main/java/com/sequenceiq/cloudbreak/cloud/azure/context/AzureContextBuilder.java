package com.sequenceiq.cloudbreak.cloud.azure.context;

import org.springframework.beans.factory.annotation.Value;
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

    @Value("${azure.resource.builder.pool.size:20}")
    private int resourceBuilderPoolSize;

    @Override
    public AzureContext contextInit(CloudContext context, AuthenticatedContext auth, Network network, boolean build) {
        Location location = context.getLocation();
        return new AzureContext(context.getName(), location, resourceBuilderPoolSize, build);
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
