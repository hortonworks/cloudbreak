package com.sequenceiq.cloudbreak.cloud.template;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.common.api.type.ResourceType;

public interface AuthenticationResourceBuilder<C extends ResourceBuilderContext> extends CloudPlatformAware, OrderedBuilder, ResourceChecker<C> {

    CloudResource create(C context, AuthenticatedContext auth, CloudStack stack);

    CloudResource build(C context, AuthenticatedContext auth, CloudStack stack, CloudResource resource) throws Exception;

    CloudResourceStatus update(C context, AuthenticatedContext auth, CloudStack stack, CloudResource resource);

    CloudResource delete(C context, AuthenticatedContext auth, CloudResource resource, CloudStack stack) throws Exception;

    ResourceType resourceType();

}
