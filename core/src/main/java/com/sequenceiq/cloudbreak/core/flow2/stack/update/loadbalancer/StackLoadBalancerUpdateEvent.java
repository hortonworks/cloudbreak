package com.sequenceiq.cloudbreak.core.flow2.stack.update.loadbalancer;

import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformResult;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.LoadBalancerMetadataFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.LoadBalancerMetadataSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateCloudLoadBalancersFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateCloudLoadBalancersSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateLoadBalancerEntityFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.CreateLoadBalancerEntitySuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterFreeIpaDnsFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterFreeIpaDnsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterPublicDnsFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterPublicDnsSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RestartCmForLbFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RestartCmForLbSuccess;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigSuccess;
import com.sequenceiq.flow.core.FlowEvent;

public enum StackLoadBalancerUpdateEvent implements FlowEvent {
    STACK_LOAD_BALANCER_UPDATE_EVENT("STACK_LOAD_BALANCER_UPDATE_TRIGGER_EVENT"),
    CREATE_LOAD_BALANCER_ENTITY_FINISHED_EVENT(CloudPlatformResult.selector(CreateLoadBalancerEntitySuccess.class)),
    CREATE_LOAD_BALANCER_ENTITY_FAILED_EVENT(CloudPlatformResult.selector(CreateLoadBalancerEntityFailure.class)),
    CREATE_CLOUD_LOAD_BALANCERS_FINISHED_EVENT(CloudPlatformResult.selector(CreateCloudLoadBalancersSuccess.class)),
    CREATE_CLOUD_LOAD_BALANCERS_FAILED_EVENT(CloudPlatformResult.selector(CreateCloudLoadBalancersFailure.class)),
    COLLECT_LOAD_BALANCER_METADATA_FINISHED_EVENT(CloudPlatformResult.selector(LoadBalancerMetadataSuccess.class)),
    COLLECT_LOAD_BALANCER_METADATA_FAILED_EVENT(CloudPlatformResult.selector(LoadBalancerMetadataFailure.class)),
    REGISTER_PUBLIC_DNS_FINISHED_EVENT(CloudPlatformResult.selector(RegisterPublicDnsSuccess.class)),
    REGISTER_PUBLIC_DNS_FAILED_EVENT(CloudPlatformResult.selector(RegisterPublicDnsFailure.class)),
    REGISTER_FREEIPA_DNS_FINISHED_EVENT(CloudPlatformResult.selector(RegisterFreeIpaDnsSuccess.class)),
    REGISTER_FREEIPA_DNS_FAILED_EVENT(CloudPlatformResult.selector(RegisterFreeIpaDnsFailure.class)),
    UPDATE_SERVICE_CONFIG_FINISHED_EVENT(CloudPlatformResult.selector(UpdateServiceConfigSuccess.class)),
    UPDATE_SERVICE_CONFIG_FAILED_EVENT(CloudPlatformResult.selector(UpdateServiceConfigFailure.class)),
    RESTART_CM_FAILED_EVENT(CloudPlatformResult.selector(RestartCmForLbFailure.class)),
    RESTART_CM_FINISHED_EVENT(CloudPlatformResult.selector(RestartCmForLbSuccess.class)),
    LOAD_BALANCER_UPDATE_FAILED("STACK_LOAD_BALANCER_UPDATE_FAILED"),
    LOAD_BALANCER_UPDATE_FINISHED_EVENT("STACK_LOAD_BALANCER_UPDATE_FINISHED"),
    LOAD_BALANCER_UPDATE_FAIL_HANDLED_EVENT("STACK_LOAD_BALANCER_UPDATE_FAIL_HANDLED");

    private final String event;

    StackLoadBalancerUpdateEvent(String event) {
        this.event = event;
    }

    @Override
    public String event() {
        return event;
    }
}
