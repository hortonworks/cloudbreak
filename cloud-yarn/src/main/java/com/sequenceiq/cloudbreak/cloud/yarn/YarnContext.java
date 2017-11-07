package com.sequenceiq.cloudbreak.cloud.yarn;

import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class YarnContext extends ResourceBuilderContext {
    public YarnContext(String name, Location location, int parallelResourceRequest, boolean build) {
        super(name, location, parallelResourceRequest, build);
    }
}
