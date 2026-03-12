package com.sequenceiq.freeipa.flow.freeipa.loadbalancer.event;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sequenceiq.freeipa.flow.stack.StackEvent;

public class LoadBalancerCreationTriggerEvent  extends StackEvent {

    private final LoadBalancerProvisioningMode loadBalancerProvisioningMode;

    private final String operationId;

    @JsonCreator
    public LoadBalancerCreationTriggerEvent(
            @JsonProperty("selector") String selector,
            @JsonProperty("resourceId")Long stackId,
            @JsonProperty("loadBalancerProvisioningMode") LoadBalancerProvisioningMode loadBalancerProvisioningMode,
            @JsonProperty("operationId") String operationId) {
        super(selector, stackId);
        this.loadBalancerProvisioningMode = Optional.ofNullable(loadBalancerProvisioningMode)
                .orElse(LoadBalancerProvisioningMode.BOOTSTRAP);
        this.operationId = operationId;
    }

    public LoadBalancerCreationTriggerEvent(String selector, Long stackId) {
        super(selector, stackId);
        this.loadBalancerProvisioningMode = LoadBalancerProvisioningMode.BOOTSTRAP;
        this.operationId = null;
    }

    public LoadBalancerProvisioningMode getLoadBalancerProvisioningMode() {
        return loadBalancerProvisioningMode;
    }

    public String getOperationId() {
        return operationId;
    }

    @Override
    public String toString() {
        return "LoadBalancerCreationTriggerEvent{" +
                "loadBalancerProvisioningMode=" + loadBalancerProvisioningMode +
                ", operationId='" + operationId + '\'' +
                "} " + super.toString();
    }
}