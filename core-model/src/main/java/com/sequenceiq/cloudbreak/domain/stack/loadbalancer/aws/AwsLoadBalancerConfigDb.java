package com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws;

/**
 * The top level AWS specific load balancer metadata database object. For AWS, the only metadata needed at
 * the load balancer level is the load balancer ARN.
 */
public class AwsLoadBalancerConfigDb {

    private String arn;

    public void setArn(String arn) {
        this.arn = arn;
    }

    public String getArn() {
        return arn;
    }

    @Override
    public String toString() {
        return "AwsLoadBalancerConfig{" +
            "arn='" + arn + '\'' +
            '}';
    }
}
