package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
import com.sequenceiq.cloudbreak.ccm.termination.CcmV2AgentTerminationListener;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.stack.termination.StackTerminationEvent;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.StackFailureEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.recipe.CcmKeyDeregisterSuccess;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class CcmKeyDeregisterHandler implements EventHandler<CcmKeyDeregisterRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(CcmKeyDeregisterHandler.class);

    @Inject
    private EventBus eventBus;

    @Inject
    private StackService stackService;

    @Inject
    private CcmResourceTerminationListener ccmResourceTerminationListener;

    @Inject
    private CcmV2AgentTerminationListener ccmV2AgentTerminationListener;

    @Override
    public void accept(Event<CcmKeyDeregisterRequest> requestEvent) {
        CcmKeyDeregisterRequest request = requestEvent.getData();
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            try {
                if (request.getTunnel().useCcmV1()) {
                    LOGGER.debug("De-registering MinaSshdServiceId '{}' from CCM. Cluster CRN: {}", stack.getMinaSshdServiceId(), stack.getResourceCrn());
                    ccmResourceTerminationListener.deregisterCcmSshTunnelingKey(request.getActorCrn(), request.getAccountId(), request.getKeyId(),
                            stack.getMinaSshdServiceId());
                    LOGGER.debug("De-registered MinaSshdServiceId '{}' from CCM. Cluster CRN: {}", stack.getMinaSshdServiceId(), stack.getResourceCrn());
                } else if (request.getTunnel().useCcmV2OrJumpgate() && StringUtils.isNotBlank(stack.getCcmV2AgentCrn())) {
                    LOGGER.debug("De-registering CcmV2AgentCrn '{}' from CCM. Cluster CRN: {}", stack.getCcmV2AgentCrn(), stack.getResourceCrn());
                    ccmV2AgentTerminationListener.deregisterInvertingProxyAgent(stack.getCcmV2AgentCrn());
                    LOGGER.debug("De-registered CcmV2AgentCrn '{}' from CCM. Cluster CRN: {}", stack.getCcmV2AgentCrn(), stack.getResourceCrn());
                } else {
                    LOGGER.debug("CCM is DISABLED, skipping de-registering of key from CCM. Cluster CRN: {}", stack.getResourceCrn());
                }
            } catch (Exception ex) {
                LOGGER.warn("CCM key deregistration failed", ex);
            }
            result = new CcmKeyDeregisterSuccess(stack.getId());
        } catch (Exception ex) {
            LOGGER.error("Stack retrieval for CCM key deregistration failed", ex);
            result = new StackFailureEvent(StackTerminationEvent.CCM_KEY_DEREGISTER_FAILED_EVENT.event(), request.getResourceId(), ex);
        }
        eventBus.notify(result.selector(), new Event<>(requestEvent.getHeaders(), result));
    }

    @Override
    public String selector() {
        return EventSelectorUtil.selector(CcmKeyDeregisterRequest.class);
    }
}
