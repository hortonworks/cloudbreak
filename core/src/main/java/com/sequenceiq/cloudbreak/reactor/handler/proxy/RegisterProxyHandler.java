package com.sequenceiq.cloudbreak.reactor.handler.proxy;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.proxy.RegisterProxyFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.proxy.RegisterProxyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.proxy.RegisterProxySuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.service.proxy.ProxyRegistrator;
import com.sequenceiq.cloudbreak.service.stack.StackService;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RegisterProxyHandler implements ReactorEventHandler<RegisterProxyRequest> {

    @Inject
    private StackService stackService;

    @Inject
    private ProxyRegistrator proxyRegistrator;

    @Inject
    private EventBus eventBus;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RegisterProxyRequest.class);
    }

    @Override
    public void accept(Event<RegisterProxyRequest> event) {
        Long stackId = event.getData().getStackId();
        Selectable response;
        try {
            Stack stack = stackService.getByIdWithListsWithoutAuthorization(stackId);
            proxyRegistrator.registerIfNeed(stack);
            response = new RegisterProxySuccess(stackId);
        } catch (RuntimeException e) {
            response = new RegisterProxyFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event<>(event.getHeaders(), response));
    }
}
