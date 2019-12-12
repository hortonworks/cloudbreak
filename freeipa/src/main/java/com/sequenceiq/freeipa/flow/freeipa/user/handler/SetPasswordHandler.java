package com.sequenceiq.freeipa.flow.freeipa.user.handler;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.logger.MDCBuilder;
import com.sequenceiq.flow.event.EventSelectorUtil;
import com.sequenceiq.flow.reactor.api.handler.EventHandler;
import com.sequenceiq.freeipa.client.FreeIpaClient;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordRequest;
import com.sequenceiq.freeipa.flow.freeipa.user.event.SetPasswordResult;
import com.sequenceiq.freeipa.service.freeipa.FreeIpaClientFactory;
import com.sequenceiq.freeipa.service.stack.StackService;

import reactor.bus.Event;

@Component
public class SetPasswordHandler implements EventHandler<SetPasswordRequest> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SetPasswordHandler.class);

    @Inject
    private StackService stackService;

    @Inject
    private FreeIpaClientFactory freeIpaClientFactory;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(SetPasswordRequest.class);
    }

    @Override
    public void accept(Event<SetPasswordRequest> setPasswordRequestEvent) {
        SetPasswordRequest request = setPasswordRequestEvent.getData();
        LOGGER.info("SetPasswordHandler accepting request {}", request);
        try {
            Stack stack = stackService.getStackById(request.getResourceId());
            MDCBuilder.buildMdcContext(stack);

            FreeIpaClient freeIpaClient = freeIpaClientFactory.getFreeIpaClientForStack(stack);
            freeIpaClient.userSetPasswordWithExpiration(request.getUsername(), request.getPassword(), request.getExpirationInstant());
            SetPasswordResult result = new SetPasswordResult(request);
            request.getResult().onNext(result);
        } catch (Exception e) {
            request.getResult().onError(e);
        }
    }
}