package com.sequenceiq.cloudbreak.cloud.template;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;

public interface ResourceChecker<C extends ResourceBuilderContext> {
    List<CloudResourceStatus> checkResources(C context, AuthenticatedContext auth, List<CloudResource> resources);
}
