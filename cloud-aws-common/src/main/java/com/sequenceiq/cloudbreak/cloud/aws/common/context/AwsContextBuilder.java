package com.sequenceiq.cloudbreak.cloud.aws.common.context;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.aws.common.AwsConstants;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.aws.common.view.AuthenticatedContextView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.template.ResourceContextBuilder;

@Service
public class AwsContextBuilder implements ResourceContextBuilder<AwsContext> {

    @Value("${aws.resource.builder.pool.size:20}")
    private int resourceBuilderPoolSize;

    @Override
    public AwsContext contextInit(CloudContext context, AuthenticatedContext auth, Network network, boolean build) {
        Location location = context.getLocation();
        AuthenticatedContextView authenticatedContextView = new AuthenticatedContextView(auth);
        AmazonEc2Client amazonEC2Client = authenticatedContextView.getAmazonEC2Client();
        AmazonElasticLoadBalancingClient elasticLoadBalancingClient = authenticatedContextView.getElasticLoadBalancingClient();
        return new AwsContext(context.getName(), amazonEC2Client, location, resourceBuilderPoolSize, build, elasticLoadBalancingClient);
    }

    @Override
    public Platform platform() {
        return AwsConstants.AWS_PLATFORM;
    }

    @Override
    public Variant variant() {
        return AwsConstants.AWS_DEFAULT_VARIANT;
    }
}
