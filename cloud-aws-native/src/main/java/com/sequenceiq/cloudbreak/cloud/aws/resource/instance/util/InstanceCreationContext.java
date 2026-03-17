package com.sequenceiq.cloudbreak.cloud.aws.resource.instance.util;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.context.AwsContext;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

public record InstanceCreationContext(AwsContext context, AuthenticatedContext auth, AmazonEc2Client amazonEc2Client) {
}
