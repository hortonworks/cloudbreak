package com.sequenceiq.cloudbreak.service.events;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.conf.ReactorConfig;

import reactor.core.Reactor;
import reactor.event.selector.Selectors;

@Component
public class CloudbreakEventReactorInitializer implements InitializingBean {

    @Autowired
    private CloudbreakEventHandler cloudbreakEventHandler;

    @Autowired
    private Reactor reactor;

    @Override
    public void afterPropertiesSet() throws Exception {
        reactor.on(Selectors.$(ReactorConfig.CLOUDBREAK_EVENT), cloudbreakEventHandler);
    }

}
