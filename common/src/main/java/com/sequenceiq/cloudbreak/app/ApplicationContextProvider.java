package com.sequenceiq.cloudbreak.app;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.sequenceiq.cloudbreak.common.tx.HibernateCircuitBreakerConfigProvider;

@Configuration
@Order(HIGHEST_PRECEDENCE)
public class ApplicationContextProvider implements ApplicationContextAware {

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        StaticApplicationContext.setApplicationContext(applicationContext);
        HibernateCircuitBreakerConfigProvider.init(applicationContext);
    }
}