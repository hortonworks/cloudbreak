package com.sequenceiq.cloudbreak.core.flow2.stack.migration.handler.service;

import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.event.resource.migration.aws.CreateResourcesRequest;

public interface ResourceRecreator {

    void recreate(CreateResourcesRequest request, AwsContext awsContext, AuthenticatedContext ac) throws Exception;
}
