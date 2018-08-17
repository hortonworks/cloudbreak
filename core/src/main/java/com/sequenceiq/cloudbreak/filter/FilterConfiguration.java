package com.sequenceiq.cloudbreak.filter;

import javax.inject.Inject;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;

@Configuration
public class FilterConfiguration {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Bean
    public FilterRegistrationBean restRequestThreadLocalFilterRegistrationBean() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        RestRequestThreadLocalFilter filter = new RestRequestThreadLocalFilter(restRequestThreadLocalService);
        registrationBean.setFilter(filter);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean mdcContextFilterRegistrationBean() {
        FilterRegistrationBean registrationBean = new FilterRegistrationBean();
        MDCContextFilter userFilter = new MDCContextFilter(authenticatedUserService);
        registrationBean.setFilter(userFilter);
        registrationBean.setOrder(Integer.MIN_VALUE);
        return registrationBean;
    }
}
