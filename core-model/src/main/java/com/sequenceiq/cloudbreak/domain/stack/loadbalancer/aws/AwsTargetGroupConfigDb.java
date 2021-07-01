package com.sequenceiq.cloudbreak.domain.stack.loadbalancer.aws;

import java.util.HashMap;
import java.util.Map;

/**
 * The top level AWS specific target group metadata database object. For AWS, there is a listener and target group
 * resources associated with each port the load balander is listening on. This class stores a map object that
 * maps ports to the listener listening on that port, and the target group that listener forwards traffic to.
 */
public class AwsTargetGroupConfigDb {

    private Map<Integer, AwsTargetGroupArnsDb> portArnMapping = new HashMap<>();

    public Map<Integer, AwsTargetGroupArnsDb> getPortArnMapping() {
        return portArnMapping;
    }

    public void setPortArnMapping(Map<Integer, AwsTargetGroupArnsDb> portArnMapping) {
        this.portArnMapping = portArnMapping;
    }

    public void addPortArnMapping(Integer port, AwsTargetGroupArnsDb arns) {
        portArnMapping.put(port, arns);
    }

    @Override
    public String toString() {
        return "AwsTargetGroupConfigDb{" +
            "portArnMapping=" + portArnMapping +
            '}';
    }
}
