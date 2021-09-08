package com.sequenceiq.cloudbreak.domain.stack.loadbalancer.gcp;

public class GcpLoadBalancerNamesDb {

    private String instanceGroupName;

    private String backendServiceName;

    public String getInstanceGroupName() {
        return instanceGroupName;
    }

    public void setInstanceGroupName(String instanceGroupName) {
        this.instanceGroupName = instanceGroupName;
    }

    public String getBackendServiceName() {
        return backendServiceName;
    }

    public void setBackendServiceName(String backendServiceName) {
        this.backendServiceName = backendServiceName;
    }

    @Override
    public String toString() {
        return "GcpLoadBalancerNamesDb{" +
                "instanceGroupName='" + instanceGroupName + '\'' +
                ", BackendServiceName='" + backendServiceName + '\'' +
                '}';
    }
}
