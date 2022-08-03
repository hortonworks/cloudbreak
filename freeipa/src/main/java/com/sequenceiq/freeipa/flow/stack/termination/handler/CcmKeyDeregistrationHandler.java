package com.sequenceiq.freeipa.flow.stack.termination.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.flow.stack.termination.event.ccm.CcmKeyDeregistrationFinished;
import com.sequenceiq.freeipa.flow.stack.termination.event.ccm.CcmKeyDeregistrationRequest;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CcmKeyDeregistrationHandler implements EventHandler<CcmKeyDeregistrationRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmKeyDeregistrationHandler.class);

    @Inject
    private CcmResourceTerminationListener ccmResourceTerminationListener;

    @Inject
    private EventBus eventBus;

    @Inject
    private CcmV2AgentTerminationListener ccmV2AgentTerminationListener;

    @Override
    public void accept(Event<CcmKeyDeregistrationRequest> requestEvent) {
        CcmKeyDeregistrationRequest request = requestEvent.getData();
        try {
            if (request.getTunnel().useCcmV1()) {
                LOGGER.debug("De-registering CCM key '{}' for freeipa stack '{}'", request.getMinaSshdServiceId(), request.getResourceId());
                ccmResourceTerminationListener.deregisterCcmSshTunnelingKey(request.getActorCrn(), request.getAccountId(), request.getKeyId(),
                        request.getMinaSshdServiceId());
                LOGGER.debug("De-registered CCM key '{}' for freeipa stack '{}'", request.getMinaSshdServiceId(), request.getResourceId());
            } else if (request.getTunnel().useCcmV2OrJumpgate()) {
                LOGGER.debug("De-registering CCM V2 key '{}' for freeipa stack {}", request.getCcmV2AgentCrn(), request.getResourceId());
                ccmV2AgentTerminationListener.deregisterInvertingProxyAgent(request.getCcmV2AgentCrn());
                LOGGER.debug("De-registered CCM V2 key '{}' for freeipa stack {}", request.getCcmV2AgentCrn(), request.getResourceId());
            } else {
                LOGGER.debug("CCM is DISABLED, skipping de-registering of key from CCM. FreeIPA stack: {}", request.getResourceId());
            }
        } catch (Exception ex) {
            LOGGER.warn("CCM key de-registration failed", ex);
        }

        Selectable result = new CcmKeyDeregistrationFinished(request.getResourceId(), request.getForced());
        eventBus.notify(result.selector(), new Event<>(requestEvent.getHeaders(), result));
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CcmKeyDeregistrationRequest.class);
    }
}
