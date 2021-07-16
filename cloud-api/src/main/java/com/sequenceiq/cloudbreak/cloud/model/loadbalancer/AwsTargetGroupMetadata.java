package com.sequenceiq.cloudbreak.cloud.model.loadbalancer;

import java.util.Objects;

/**
 * Contains information about listeners and target groups attached to a load balancer. In AWS each listener
 * listens on a single port, and forwards traffic received on that port to a target group, which distributes
 * the traffic among the instances in the target group on a defined traffic port. Listeners and target groups
 * each have their own ARN. In the cloudbreak implementation, the listener port and the target group traffic
 * port are the same, so they are represented by a single 'port' member.
 */
public class AwsTargetGroupMetadata {

    private String listenerArn;

    private String targetGroupArn;

    private int port;

    public String getListenerArn() {
        return listenerArn;
    }

    public void setListenerArn(String listenerArn) {
        this.listenerArn = listenerArn;
    }

    public String getTargetGroupArn() {
        return targetGroupArn;
    }

    public void setTargetGroupArn(String targetGroupArn) {
        this.targetGroupArn = targetGroupArn;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AwsTargetGroupMetadata that = (AwsTargetGroupMetadata) o;
        return port == that.port &&
            Objects.equals(listenerArn, that.listenerArn) &&
            Objects.equals(targetGroupArn, that.targetGroupArn);
    }

    @Override
    public int hashCode() {
        return Objects.hash(listenerArn, targetGroupArn, port);
    }
}
