package com.sequenceiq.cloudbreak.cloud.aws.context;

import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class AwsContext extends ResourceBuilderContext {

    public AwsContext(String name, Location location, int parallelResourceRequest, boolean build) {
        super(name, location, parallelResourceRequest, build);
    }
}
