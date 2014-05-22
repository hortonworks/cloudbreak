package com.sequenceiq.provisioning.domain;

import com.amazonaws.services.cloudformation.model.DescribeStackResourcesResult;

public class AwsCloudInstanceDescription extends CloudInstanceDescription {

    private DescribeStackResourcesResult describeStackResourcesResult;

    public DescribeStackResourcesResult getDescribeStackResourcesResult() {
        return describeStackResourcesResult;
    }

    public void setDescribeStackResourcesResult(DescribeStackResourcesResult describeStackResourcesResult) {
        this.describeStackResourcesResult = describeStackResourcesResult;
    }

}
