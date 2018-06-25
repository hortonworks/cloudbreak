package com.sequenceiq.cloudbreak.filter;

import javax.inject.Inject;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;

@Configuration
public class FilterConfiguration {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Bean
    public FilterRegistrationBean mdcContextFilterRegistrationBean() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        MDCContextFilter userFilter = new MDCContextFilter(authenticatedUserService);
        registrationBean.setFilter(userFilter);
        registrationBean.setOrder(Integer.MIN_VALUE);
        return registrationBean;
    }
}
