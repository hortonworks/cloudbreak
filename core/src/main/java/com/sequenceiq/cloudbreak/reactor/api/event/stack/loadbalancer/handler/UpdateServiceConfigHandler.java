package com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.handler;

import static java.util.Objects.requireNonNull;

import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.bootstrap.service.host.ClusterHostServiceRunner;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigFailure;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.stack.loadbalancer.UpdateServiceConfigSuccess;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerFqdnUtil;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.view.StackView;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.ExceptionCatcherEventHandler;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;

@Component
public class UpdateServiceConfigHandler extends ExceptionCatcherEventHandler<UpdateServiceConfigRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateServiceConfigHandler.class);

    private static final String HUE_KNOX_PROXYHOSTS = "knox_proxyhosts";

    private static final String HUE_SERVICE = "HUE";

    @Inject
    private StackDtoService stackDtoService;

    @Inject
    private ClusterHostServiceRunner clusterHostServiceRunner;

    @Inject
    private ClusterApiConnectors clusterApiConnectors;

    @Inject
    private LoadBalancerFqdnUtil loadBalancerFqdnUtil;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(UpdateServiceConfigRequest.class);
    }

    @Override
    protected Selectable defaultFailureEvent(Long resourceId, Exception e, Event<UpdateServiceConfigRequest> event) {
        return new UpdateServiceConfigFailure(resourceId, e);
    }

    @Override
    protected Selectable doAccept(HandlerEvent<UpdateServiceConfigRequest> event) {
        UpdateServiceConfigRequest request = event.getData();
        StackDto stackDto = stackDtoService.getById(event.getData().getResourceId());
        StackView stack = stackDto.getStack();
        requireNonNull(stack);
        requireNonNull(stackDto.getCluster());
        try {
            LOGGER.debug("Gathering entries for Hue knox_proxyhosts property.");
            Set<String> proxyhosts = new HashSet<>();
            if (StringUtils.isNotEmpty(stackDto.getPrimaryGatewayInstance().getDiscoveryFQDN())) {
                proxyhosts.add(stackDto.getPrimaryGatewayInstance().getDiscoveryFQDN());
                proxyhosts.add(stackDto.getCluster().getFqdn());
            }
            String loadBalancerFqdn = loadBalancerFqdnUtil.getLoadBalancerUserFacingFQDN(stack.getId());
            if (StringUtils.isNotEmpty(loadBalancerFqdn)) {
                proxyhosts.add(loadBalancerFqdn);
            }
            LOGGER.debug("Hue knox_proxyhosts setting will be updated to {}", proxyhosts);
            ClusterApi clusterApi = clusterApiConnectors.getConnector(stackDto);
            clusterApi.clusterModificationService().updateServiceConfigAndRestartService(HUE_SERVICE, HUE_KNOX_PROXYHOSTS, String.join(",", proxyhosts));
            LOGGER.debug("Updating CM frontend URL with load balancer DNS");
            clusterHostServiceRunner.updateClusterConfigs(stackDto);
            LOGGER.debug("Service config update was successful");
            return new UpdateServiceConfigSuccess(stack.getId());
        } catch (Exception e) {
            LOGGER.warn("Error updating HUE and CM service configuration.", e);
            return new UpdateServiceConfigFailure(request.getResourceId(), e);
        }
    }
}
