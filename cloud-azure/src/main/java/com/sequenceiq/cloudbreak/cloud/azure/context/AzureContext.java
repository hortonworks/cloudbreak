package com.sequenceiq.cloudbreak.cloud.azure.context;

import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class AzureContext extends ResourceBuilderContext {

    public AzureContext(String name, Location location, int parallelResourceRequest, boolean build) {
        super(name, location, parallelResourceRequest, build);
    }
}
