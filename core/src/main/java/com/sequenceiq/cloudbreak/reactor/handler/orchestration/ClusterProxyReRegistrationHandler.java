package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy.ClusterProxyService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.host.HostGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.exception.NotFoundException;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyReRegistrationResult;
import com.sequenceiq.cloudbreak.service.gateway.GatewayService;
import com.sequenceiq.cloudbreak.service.hostgroup.HostGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterProxyReRegistrationHandler implements EventHandler<ClusterProxyReRegistrationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyReRegistrationHandler.class);

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private HostGroupService hostGroupService;

    @Inject
    private StackService stackService;

    @Inject
    private GatewayService gatewayService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyReRegistrationRequest.class);
    }

    @Override
    public void accept(Event<ClusterProxyReRegistrationRequest> event) {
        ClusterProxyReRegistrationRequest request = event.getData();
        Selectable response = registerCluster(request);
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }

    private Selectable registerCluster(ClusterProxyReRegistrationRequest request) {
        if (!clusterProxyConfiguration.isClusterProxyIntegrationEnabled()) {
            return new ClusterProxyReRegistrationResult(request);
        }

        Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
        try {
            String hostGroupName = request.getHostGroupName();
            HostGroup hostGroup = hostGroupService.findHostGroupInClusterByName(stack.getCluster().getId(), hostGroupName)
                    .orElseThrow(NotFoundException.notFound("hostgroup", hostGroupName));
            InstanceGroup instanceGroup = hostGroup.getConstraint().getInstanceGroup();
            boolean gatewayInstanceGroup = InstanceGroupType.GATEWAY.equals(instanceGroup.getInstanceGroupType());

            if (!gatewayInstanceGroup) {
                LOGGER.info("Not re-registering with Cluster Proxy as this is not a gateway host group. stack crn: {}", stack.getResourceCrn());
                return new ClusterProxyReRegistrationResult(request);
            }
            clusterProxyService.reRegisterCluster(stack, request.getAccountId());
            return new ClusterProxyReRegistrationResult(request);
        } catch (Exception e) {
            LOGGER.error("Error occurred re-registering cluster {} in environment {} to cluster proxy",
                    stack.getCluster().getId(), stack.getEnvironmentCrn(), e);
            return new ClusterProxyReRegistrationResult(e.getMessage(), e, request);
        }
    }
}
