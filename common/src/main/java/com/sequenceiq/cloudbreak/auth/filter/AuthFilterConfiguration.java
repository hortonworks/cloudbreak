package com.sequenceiq.cloudbreak.auth.filter;

import javax.inject.Inject;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.ThreadBaseUserCrnProvider;

@Configuration
public class AuthFilterConfiguration {
    @Inject
    private ThreadBaseUserCrnProvider threadBaseUserCrnProvider;

    @Bean
    public FilterRegistrationBean<CrnFilter> crnFilterRegistrationBean() {
        FilterRegistrationBean<CrnFilter> registrationBean = new FilterRegistrationBean<>();
        CrnFilter filter = new CrnFilter(threadBaseUserCrnProvider);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
    }
}
