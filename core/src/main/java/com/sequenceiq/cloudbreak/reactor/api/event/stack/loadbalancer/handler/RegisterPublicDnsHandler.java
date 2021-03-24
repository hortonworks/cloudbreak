package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterPublicDnsFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterPublicDnsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.RegisterPublicDnsSuccess;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.publicendpoint.GatewayPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class RegisterPublicDnsHandler extends ExceptionCatcherEventHandler<RegisterPublicDnsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterPublicDnsHandler.class);

    @Inject
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Inject
    private InstanceGroupService instanceGroupService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Inject
    private GatewayPublicEndpointManagementService gatewayPublicEndpointManagementService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RegisterPublicDnsRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<RegisterPublicDnsRequest> event) {
        return new RegisterPublicDnsFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        RegisterPublicDnsRequest request = event.getData();
        Stack stack = request.getStack();
        if (gatewayPublicEndpointManagementService.isCertRenewalTriggerable(stack)) {
            try {
                LOGGER.debug("Fetching instance group and instance metadata for stack.");
                InstanceGroup instanceGroup = instanceGroupService.getPrimaryGatewayInstanceGroupByStackId(stack.getId());
                Optional<InstanceMetaData> instanceMetaDataOptional = instanceMetaDataService.getPrimaryGatewayInstanceMetadata(stack.getId());
                if (instanceMetaDataOptional.isPresent()) {
                    stack.getInstanceGroups().stream()
                        .filter(ig -> ig.getId().equals(instanceGroup.getId()))
                        .forEach(ig -> ig.setInstanceMetaData(Set.of(instanceMetaDataOptional.get())));
                    LOGGER.debug("Registering load balancer public DNS entry");
                    boolean success = clusterPublicEndpointManagementService.provisionLoadBalancer(stack);
                    if (!success) {
                        throw new CloudbreakException("Public DNS registration resulted in failed state. Please consult DNS registration logs.");
                    }
                    LOGGER.debug("Load balancer public DNS registration was successful");
                    return new RegisterPublicDnsSuccess(stack);
                } else {
                    throw new CloudbreakException("Unable to find instance metadata for primary instance group. Certificates cannot " +
                        "be updated.");
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to register load balancer public DNS entries.", e);
                return new RegisterPublicDnsFailure(request.getResourceId(), e);
            }
        } else {
            LOGGER.info("Certificates and DNS are not managed by PEM for stack {}. Skipping public DNS registration.", stack.getName());
            return new RegisterPublicDnsSuccess(stack);
        }
    }
}
