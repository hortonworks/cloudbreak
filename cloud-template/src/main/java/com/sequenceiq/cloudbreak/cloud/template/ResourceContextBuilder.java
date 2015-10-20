package com.sequenceiq.cloudbreak.cloud.template;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

public interface ResourceContextBuilder<C extends ResourceBuilderContext> extends CloudPlatformAware {

    C contextInit(CloudContext cloudContext, AuthenticatedContext auth, List<CloudResource> resources, boolean build);

}
