package com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws;

/**
 * Contains the ARNs for both a listener and the target group that listener forwards traffic to.
 */
public class AwsTargetGroupArnsDb {

    private String listenerArn;

    private String targetGroupArn;

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

    @Override
    public String toString() {
        return "AwsTargetGroupArnsDb{" +
            "listenerArn='" + listenerArn + '\'' +
            ", targetGroupArn='" + targetGroupArn + '\'' +
            '}';
    }
}
