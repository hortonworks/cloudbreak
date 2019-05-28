package com.sequenceiq.environment.configuration.security;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticationModeConfiguration;
import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticationService;

@Configuration
public class CrnFilterConfiguration {

    private final ThreadLocalUserCrnProvider threadLocalUserCrnProvider;

    private final AuthenticationService authenticationService;

    public CrnFilterConfiguration(ThreadLocalUserCrnProvider threadLocalUserCrnProvider, AuthenticationService authenticationService) {
        this.threadLocalUserCrnProvider = threadLocalUserCrnProvider;
        this.authenticationService = authenticationService;
    }

    @Bean
    @ConditionalOnProperty(name = AuthenticationModeConfiguration.CB_AUTHENTICATION_DISABLED, havingValue = "true")
    public FilterRegistrationBean<DisabledSecurityCrnFilter> disabledSecurityCrnFilterFilterRegistrationBean() {
        FilterRegistrationBean<DisabledSecurityCrnFilter> registrationBean = new FilterRegistrationBean<>();
        DisabledSecurityCrnFilter filter = new DisabledSecurityCrnFilter(threadLocalUserCrnProvider, authenticationService);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
    }

    @Bean
    @ConditionalOnMissingBean(name = "disabledSecurityCrnFilterFilterRegistrationBean")
    public FilterRegistrationBean<HeaderCrnFilter> headerCrnFilterRegistrationBean() {
        FilterRegistrationBean<HeaderCrnFilter> registrationBean = new FilterRegistrationBean<>();
        HeaderCrnFilter filter = new HeaderCrnFilter(threadLocalUserCrnProvider);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
    }
}
