package com.sequenceiq.cloudbreak.cloud.template;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.context.ResourceBuilderContext;

public interface ResourceContextBuilder<C extends ResourceBuilderContext> extends CloudPlatformAware {

    C contextInit(CloudContext cloudContext, AuthenticatedContext auth, boolean build);

}
