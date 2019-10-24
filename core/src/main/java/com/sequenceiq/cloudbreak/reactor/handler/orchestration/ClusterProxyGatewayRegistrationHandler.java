package com.sequenceiq.cloudbreak.reactor.handler.orchestration;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.clusterproxy.ClusterProxyConfiguration;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.provision.clusterproxy.ClusterProxyService;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.orchestration.ClusterProxyGatewayRegistrationSuccess;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterProxyGatewayRegistrationHandler implements EventHandler<ClusterProxyGatewayRegistrationRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyGatewayRegistrationHandler.class);

    @Inject
    private ClusterProxyConfiguration clusterProxyConfiguration;

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyGatewayRegistrationRequest.class);
    }

    @Override
    public void accept(Event<ClusterProxyGatewayRegistrationRequest> event) {
        ClusterProxyGatewayRegistrationRequest request = event.getData();
        Selectable response;
        try {
            if (clusterProxyConfiguration.isClusterProxyIntegrationEnabled()) {
                clusterProxyService.registerGatewayConfiguration(request.getResourceId());
                response = new ClusterProxyGatewayRegistrationSuccess(request.getResourceId());
            } else {
                response = new ClusterProxyGatewayRegistrationSuccess(request.getResourceId());
            }
        } catch (Exception e) {
            LOGGER.error("Error occurred when registering gateway config with cluster proxy", e);
            response = new ClusterProxyGatewayRegistrationFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
