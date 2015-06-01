package com.sequenceiq.cloudbreak.service.events;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.EventBusConfig;

import reactor.bus.EventBus;
import reactor.bus.selector.Selectors;

@Component
public class CloudbreakEventReactorInitializer implements InitializingBean {

    @Autowired
    private CloudbreakEventHandler cloudbreakEventHandler;

    @Autowired
    private EventBus reactor;

    @Override
    public void afterPropertiesSet() throws Exception {
        reactor.on(Selectors.$(EventBusConfig.CLOUDBREAK_EVENT), cloudbreakEventHandler);
    }

}
