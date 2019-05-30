package com.sequenceiq.cloudbreak.auth.filter;

import javax.inject.Inject;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;

@Configuration
public class AuthFilterConfiguration {

    private static final int ORDER = 0;

    @Inject
    private ThreadBasedUserCrnProvider threadBasedUserCrnProvider;

    @Bean
    public FilterRegistrationBean<CrnFilter> crnFilterRegistrationBean() {
        FilterRegistrationBean<CrnFilter> registrationBean = new FilterRegistrationBean<>();
        CrnFilter filter = new CrnFilter(threadBasedUserCrnProvider);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(ORDER);
        return registrationBean;
    }
}
