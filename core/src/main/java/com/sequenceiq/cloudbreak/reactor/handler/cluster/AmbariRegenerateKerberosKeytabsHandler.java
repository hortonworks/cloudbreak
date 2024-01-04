package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RegenerateKerberosKeytabsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.RegenerateKerberosKeytabsResult;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

@Component
public class AmbariRegenerateKerberosKeytabsHandler implements EventHandler<RegenerateKerberosKeytabsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRegenerateKerberosKeytabsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RegenerateKerberosKeytabsRequest.class);
    }

    @Override
    public void accept(Event<RegenerateKerberosKeytabsRequest> event) {
        RegenerateKerberosKeytabsRequest request = event.getData();
        Long stackId = request.getResourceId();
        RegenerateKerberosKeytabsResult result;
        try {

            clusterUpscaleService.regenerateKerberosKeytabs(stackId, request.getHostname());
            result = new RegenerateKerberosKeytabsResult(request);
        } catch (Exception e) {
            String message = "Failed to regenerate kerberos keytabs on new host";
            LOGGER.error(message, e);
            result = new RegenerateKerberosKeytabsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
