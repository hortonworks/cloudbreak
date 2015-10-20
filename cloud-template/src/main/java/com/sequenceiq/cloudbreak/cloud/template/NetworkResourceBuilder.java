package com.sequenceiq.cloudbreak.cloud.template;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;
import com.sequenceiq.cloudbreak.cloud.event.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Security;
import com.sequenceiq.cloudbreak.common.type.ResourceType;

public interface NetworkResourceBuilder<C extends ResourceBuilderContext> extends CloudPlatformAware, OrderedBuilder, ResourceChecker<C> {

    CloudResource create(C context, AuthenticatedContext auth, Network network, Security security);

    CloudResource build(C context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) throws Exception;

    CloudResourceStatus update(C context, AuthenticatedContext auth, Network network, Security security, CloudResource resource) throws Exception;

    CloudResource delete(C context, AuthenticatedContext auth, CloudResource resource) throws Exception;

    ResourceType resourceType();

}
