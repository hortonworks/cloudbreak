package com.sequenceiq.cloudbreak.service.events;

import javax.inject.Inject;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.EventBusConfig;

import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

@Component
public class CloudbreakEventReactorInitializer implements InitializingBean {

    @Inject
    private CloudbreakEventHandler cloudbreakEventHandler;

    @Inject
    private EventBus reactor;

    @Override
    public void afterPropertiesSet() throws Exception {
        reactor.on(Selectors.$(EventBusConfig.CLOUDBREAK_EVENT), cloudbreakEventHandler);
    }

}
