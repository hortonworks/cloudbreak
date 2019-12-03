package com.sequenceiq.cloudbreak.reactor.handler.recipe;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.ccm.termination.CcmResourceTerminationListener;
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

    @Override
    public void accept(Event<CcmKeyDeregisterRequest> requestEvent) {
        CcmKeyDeregisterRequest request = requestEvent.getData();
        Selectable result;
        try {
            Stack stack = stackService.getByIdWithListsInTransaction(request.getResourceId());
            if (Boolean.TRUE.equals(request.getUseCcm())) {
                LOGGER.debug("De-registering key from CCM. Cluster CRN: {}",
                        stack.getResourceCrn());
                try {
                    ccmResourceTerminationListener.deregisterCcmSshTunnelingKey(request.getActorCrn(), request.getAccountId(), request.getKeyId(),
                            stack.getMinaSshdServiceId());
                } catch (Exception ex) {
                    LOGGER.warn("CCM key deregistration failed", ex);
                }
            } else {
                LOGGER.info("CCM is DISABLED, skipping de-registering of key from CCM. Cluster CRN: {}",
                        stack.getResourceCrn());
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
