package com.sequenceiq.cloudbreak.filter;

import javax.servlet.DispatcherType;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.CrnFilter;
import com.sequenceiq.cloudbreak.logger.MDCContextFilter;

import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;

@Configuration
public class CommonFilterConfiguration {

    private static final int CRN_FILTER_ORDER = 0;

    @Bean
    public FilterRegistrationBean<CrnFilter> crnFilterRegistrationBean() {
        FilterRegistrationBean<CrnFilter> registrationBean = new FilterRegistrationBean<>();
        CrnFilter filter = new CrnFilter();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(CRN_FILTER_ORDER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<MDCContextFilter> mdcContextFilterRegistrationBean() {
        FilterRegistrationBean<MDCContextFilter> registrationBean = new FilterRegistrationBean<>();
        MDCContextFilter filter = new MDCContextFilter();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean spanFinishingFilter() {
        FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();
        filterRegistrationBean.setFilter(new SpanFinishingFilter());
        filterRegistrationBean.setAsyncSupported(true);
        filterRegistrationBean.setOrder(Integer.MIN_VALUE);
        filterRegistrationBean.setDispatcherTypes(DispatcherType.REQUEST);
        filterRegistrationBean.addUrlPatterns("*");
        return filterRegistrationBean;
    }
}
