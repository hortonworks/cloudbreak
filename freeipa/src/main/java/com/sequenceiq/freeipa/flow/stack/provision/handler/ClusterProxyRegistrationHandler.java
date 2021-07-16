package com.sequenceiq.freeipa.flow.stack.provision.handler;

import java.util.Set;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationSuccess;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class ClusterProxyRegistrationHandler implements EventHandler<ClusterProxyRegistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterProxyRegistrationHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterProxyService clusterProxyService;

    @Inject
    private InstanceMetaDataService instanceMetaDataService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(ClusterProxyRegistrationRequest.class);
    }

    @Override
    public void accept(Event<ClusterProxyRegistrationRequest> event) {
        ClusterProxyRegistrationRequest request = event.getData();
        Selectable response;
        try {
            Set<InstanceMetaData> ims = instanceMetaDataService.findNotTerminatedForStack(request.getResourceId());
            boolean allInstanceHasFqdn = ims.stream().allMatch(im -> StringUtils.isNotBlank(im.getDiscoveryFQDN()));
            if (allInstanceHasFqdn) {
                LOGGER.info("All instances already have FQDN set, register all to cluster proxy");
                clusterProxyService.registerFreeIpa(request.getResourceId());
            } else {
                LOGGER.info("Instances missing FQDN, fallback to PGW registration");
                clusterProxyService.registerBootstrapFreeIpa(request.getResourceId());
            }
            response = new ClusterProxyRegistrationSuccess(request.getResourceId());
        } catch (Exception e) {
            LOGGER.error("Cluster Proxy bootstrap registration has failed", e);
            response = new ClusterProxyRegistrationFailed(request.getResourceId(), e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
