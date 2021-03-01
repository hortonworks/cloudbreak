package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.List;

import java.util.Set;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import reactor.bus.Event;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigSuccess;
import com.sequenceiq.cloudbreak.service.LoadBalancerConfigService;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;

@Component
public class UpdateServiceConfigHandler extends ExceptionCatcherEventHandler<UpdateServiceConfigRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateServiceConfigHandler.class);

    private static final String HUE_KNOX_PROXYHOSTS = "knox_proxyhosts";

    private static final String HUE_SERVICE = "HUE";

    @Inject
    private StackService stackService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private LoadBalancerConfigService loadBalancerConfigService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateServiceConfigRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateServiceConfigRequest> event) {
        return new UpdateServiceConfigFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent event) {
        UpdateServiceConfigRequest request = event.getData();
        Stack stack = request.getStack();
        requireNonNull(stack);
        requireNonNull(stack.getCluster());
        try {
            LOGGER.debug("Gathering entries for Hue knox_proxyhosts property.");
            Set<String> proxyhosts = new HashSet<>();
            if (StringUtils.isNotEmpty(stack.getPrimaryGatewayInstance().getDiscoveryFQDN())) {
                proxyhosts.add(stack.getPrimaryGatewayInstance().getDiscoveryFQDN());
            }
            if (StringUtils.isNotEmpty(stack.getPrimaryGatewayInstance().getDiscoveryFQDN())) {
                proxyhosts.add(stack.getCluster().getFqdn());
            }
            String loadBalancerFqdn = loadBalancerConfigService.getLoadBalancerUserFacingFQDN(stack.getId());
            if (StringUtils.isNotEmpty(loadBalancerFqdn)) {
                proxyhosts.add(loadBalancerFqdn);
            }
            LOGGER.debug("Hue knox_proxyhosts setting will be updated to {}", proxyhosts);
            ClusterApi clusterApi = clusterApiConnectors.getConnector(stackService.getByIdWithListsInTransaction(event.getData().getResourceId()));
            clusterApi.clusterModificationService().updateServiceConfigAndRestartService(HUE_SERVICE, HUE_KNOX_PROXYHOSTS, String.join(",", proxyhosts));
            LOGGER.debug("Updating CM frontend URL with load balancer DNS");
            clusterHostServiceRunner.updateClusterConfigs(stack, stack.getCluster(), List.of());
            LOGGER.debug("Service config update was successful");
            return new UpdateServiceConfigSuccess(stack);
        } catch (Exception e) {
            LOGGER.warn("Error updating HUE and CM service configuration.", e);
            return new UpdateServiceConfigFailure(request.getResourceId(), e);
        }
    }
}
