package com.sequenceiq.freeipa.flow.stack.provision.handler;

import static com.sequenceiq.freeipa.flow.freeipa.common.FailureType.ERROR;

import java.util.Set;

import jakarta.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.entity.InstanceMetaData;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationFailed;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationRequest;
import com.sequenceiq.freeipa.flow.stack.provision.event.clusterproxy.ClusterProxyRegistrationSuccess;
import com.sequenceiq.freeipa.service.stack.ClusterProxyService;
import com.sequenceiq.freeipa.service.stack.instance.InstanceMetaDataService;

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
        try {
            Set<InstanceMetaData> ims = instanceMetaDataService.findNotTerminatedForStack(request.getResourceId());
            if (ims.isEmpty()) {
                LOGGER.error("Cluster Proxy registration has failed. No available instances  found for FreeIPA");
                ClusterProxyRegistrationFailed response = new ClusterProxyRegistrationFailed(
                        request.getResourceId(),
                        new NotFoundException("Cluster Proxy registration has failed. No available instances  found for FreeIPA"),
                        ERROR
                );
                sendEvent(response, event);
            } else {
                boolean allInstanceHasFqdn = ims.stream().allMatch(im -> StringUtils.isNotBlank(im.getDiscoveryFQDN()));
                if (allInstanceHasFqdn) {
                    LOGGER.info("All instances already have FQDN set, register all to cluster proxy");
                    clusterProxyService.registerFreeIpa(request.getResourceId());
                } else {
                    LOGGER.info("Instances missing FQDN, fallback to PGW registration");
                    clusterProxyService.registerFreeIpaForBootstrap(request.getResourceId());
                }
                sendEvent(new ClusterProxyRegistrationSuccess(request.getResourceId()), event);
            }
        } catch (Exception e) {
            LOGGER.error("Cluster Proxy bootstrap registration has failed", e);
            sendEvent(new ClusterProxyRegistrationFailed(request.getResourceId(), e, ERROR), event);
        }

    }

    private void sendEvent(Selectable response, Event<ClusterProxyRegistrationRequest> event) {
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
