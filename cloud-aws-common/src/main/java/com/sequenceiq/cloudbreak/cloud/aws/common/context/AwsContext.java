package com.sequenceiq.cloudbreak.cloud.aws.common.context;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonElasticLoadBalancingClient;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class AwsContext extends ResourceBuilderContext {

    private final AmazonEc2Client amazonEc2Client;

    private final AmazonElasticLoadBalancingClient loadBalancingClient;

    public AwsContext(String name, AmazonEc2Client amazonEC2Client, Location location, int resourceBuilderPoolSize, boolean build,
            AmazonElasticLoadBalancingClient elasticLoadBalancingClient) {
        super(name, location, resourceBuilderPoolSize, build);
        this.amazonEc2Client = amazonEC2Client;
        this.loadBalancingClient = elasticLoadBalancingClient;
    }

    public AmazonEc2Client getAmazonEc2Client() {
        return amazonEc2Client;
    }

    public AmazonElasticLoadBalancingClient getLoadBalancingClient() {
        return loadBalancingClient;
    }
}
