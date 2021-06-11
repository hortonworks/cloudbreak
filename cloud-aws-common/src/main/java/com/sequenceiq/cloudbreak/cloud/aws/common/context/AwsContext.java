package com.sequenceiq.cloudbreak.cloud.aws.common.context;

import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.template.context.ResourceBuilderContext;

public class AwsContext extends ResourceBuilderContext {

    private final AmazonEc2Client amazonEc2Client;

    public AwsContext(String name, AmazonEc2Client amazonEC2Client, Location location, int parallelResourceRequest, boolean build) {
        super(name, location, parallelResourceRequest, build);
        this.amazonEc2Client = amazonEC2Client;
    }

    public AmazonEc2Client getAmazonEc2Client() {
        return amazonEc2Client;
    }
}
