package com.sequenceiq.cloudbreak.cloud.aws.authentication;

import java.util.List;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.aws.resource.AbstractAwsNativeResourceBuilder;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.template.AuthenticationResourceBuilder;

public abstract class AbstractAwsAuthenticationBuilder extends AbstractAwsNativeResourceBuilder implements AuthenticationResourceBuilder<AwsContext> {

    @Override
    public List<CloudResourceStatus> checkResources(AwsContext context, AuthenticatedContext auth, List<CloudResource> resources) {
        return checkResources(resourceType(), context, auth, resources);
    }
}
