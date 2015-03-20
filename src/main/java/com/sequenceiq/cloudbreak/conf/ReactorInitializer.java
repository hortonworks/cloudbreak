package com.sequenceiq.cloudbreak.conf;

import static reactor.event.selector.Selectors.$;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.service.events.CloudbreakEventHandler;

import reactor.core.Reactor;

@Component
public class ReactorInitializer implements InitializingBean {

    @Autowired
    private CloudbreakEventHandler cloudbreakEventHandler;

    @Autowired
    private Reactor reactor;

    @Override
    public void afterPropertiesSet() throws Exception {
        reactor.on($(ReactorConfig.CLOUDBREAK_EVENT), cloudbreakEventHandler);
    }

}
