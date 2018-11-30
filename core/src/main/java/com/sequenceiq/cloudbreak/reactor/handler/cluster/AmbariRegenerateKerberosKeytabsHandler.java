package com.sequenceiq.cloudbreak.reactor.handler.cluster;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.core.cluster.ClusterUpscaleService;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRegenerateKerberosKeytabsRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.cluster.AmbariRegenerateKerberosKeytabsResult;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class AmbariRegenerateKerberosKeytabsHandler implements ReactorEventHandler<AmbariRegenerateKerberosKeytabsRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AmbariRegenerateKerberosKeytabsHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private ClusterUpscaleService clusterUpscaleService;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(AmbariRegenerateKerberosKeytabsRequest.class);
    }

    @Override
    public void accept(Event<AmbariRegenerateKerberosKeytabsRequest> event) {
        AmbariRegenerateKerberosKeytabsRequest request = event.getData();
        Long stackId = request.getStackId();
        AmbariRegenerateKerberosKeytabsResult result;
        try {

            clusterUpscaleService.regenerateKerberosKeytabs(stackId, request.getHostname());
            result = new AmbariRegenerateKerberosKeytabsResult(request);
        } catch (Exception e) {
            String message = "Failed to regenerate kerberos keytabs on new host";
            LOGGER.error(message, e);
            result = new AmbariRegenerateKerberosKeytabsResult(message, e, request);
        }
        eventBus.notify(result.selector(), new Event<>(event.getHeaders(), result));
    }
}
