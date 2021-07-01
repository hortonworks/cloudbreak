package com.sequenceiq.cloudbreak.cloud.model.loadbalancer;

import java.util.ArrayList;
import java.util.List;

/**
 * AWS specific extension of LoadBalancerMetadataBase. For AWS, the resource information we need is the ARN
 * for the load balancer, and information about any target groups and listeners attached to the load
 * balancer. This information is represented in the AwsTargetGroupMetadata object. Using the provided information,
 * a user can use the AWS console and CLI to look up additional information about the load balancer as needed.
 */
public class AwsLoadBalancerMetadata extends LoadBalancerMetadataBase {

    private String arn;

    private List<AwsTargetGroupMetadata> targetGroupMetadata;

    public String getArn() {
        return arn;
    }

    public void setArn(String arn) {
        this.arn = arn;
    }

    public List<AwsTargetGroupMetadata> getTargetGroupMetadata() {
        return targetGroupMetadata;
    }

    public void setTargetGroupMetadata(List<AwsTargetGroupMetadata> targetGroupMetadata) {
        this.targetGroupMetadata = targetGroupMetadata;
    }

    public void addTargetGroupMetadata(AwsTargetGroupMetadata targetGroupMetadata) {
        if (this.targetGroupMetadata == null) {
            this.targetGroupMetadata = new ArrayList<>();
        }
        this.targetGroupMetadata.add(targetGroupMetadata);
    }
}
