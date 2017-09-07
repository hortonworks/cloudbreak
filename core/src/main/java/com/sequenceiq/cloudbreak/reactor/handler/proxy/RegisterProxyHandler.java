package com.sequenceiq.cloudbreak.reactor.handler.proxy;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.event.Selectable;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.reactor.api.event.EventSelectorUtil;
import com.sequenceiq.cloudbreak.reactor.api.event.proxy.RegisterProxyFailed;
import com.sequenceiq.cloudbreak.reactor.api.event.proxy.RegisterProxyRequest;
import com.sequenceiq.cloudbreak.reactor.api.event.proxy.RegisterProxySuccess;
import com.sequenceiq.cloudbreak.reactor.handler.ReactorEventHandler;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.proxy.ProxyRegistrator;
import com.sequenceiq.cloudbreak.util.StackUtil;

import reactor.bus.Event;
import reactor.bus.EventBus;

@Component
public class RegisterProxyHandler implements ReactorEventHandler<RegisterProxyRequest> {

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ProxyRegistrator proxyRegistrator;

    @Inject
    private EventBus eventBus;

    @Inject
    private StackUtil stackUtil;

    @Override
    public String selector() {
        return EventSelectorUtil.selector(RegisterProxyRequest.class);
    }

    @Override
    public void accept(Event<RegisterProxyRequest> event) {
        Long stackId = event.getData().getStackId();
        Selectable response;
        try {
            Stack stack = stackRepository.findById(stackId);
            String proxyIp = stackUtil.extractAmbariIp(stack);
            String contextPath = stack.getCluster().getGateway().getPath();
            proxyRegistrator.register(stack.getName(), contextPath, proxyIp);
            response = new RegisterProxySuccess(stackId);
        } catch (RuntimeException e) {
            response = new RegisterProxyFailed(stackId, e);
        }
        eventBus.notify(response.selector(), new Event(event.getHeaders(), response));
    }
}
