package com.sequenceiq.cloudbreak.filter;

import javax.servlet.DispatcherType;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.CrnFilter;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.RequestHeaderMetricFilter;
import com.sequenceiq.cloudbreak.logger.MDCContextFilter;
import com.sequenceiq.cloudbreak.logger.RestLoggerFilter;

import io.opentracing.contrib.jaxrs2.server.SpanFinishingFilter;

@Configuration
public class CommonFilterConfiguration {

    private static final int CRN_FILTER_ORDER = 0;

    private static final int MDC_FILTER_ORDER = 1;

    private static final int REQUEST_RESPONSE_LOGGER_FILTER_ORDER = 2;

    @Value("${rest.logger.enabled:true}")
    private boolean restLoggerEnabled;

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
        registrationBean.setOrder(MDC_FILTER_ORDER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<RestLoggerFilter> restLoggerFilterFilterRegistrationBean() {
        FilterRegistrationBean<RestLoggerFilter> registrationBean = new FilterRegistrationBean<>();
        RestLoggerFilter filter = new RestLoggerFilter(restLoggerEnabled);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(REQUEST_RESPONSE_LOGGER_FILTER_ORDER);
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

    @Bean
    public FilterRegistrationBean<RequestHeaderMetricFilter> requestHeaderMetricFilter(MetricService metricService) {
        FilterRegistrationBean<RequestHeaderMetricFilter> registrationBean = new FilterRegistrationBean<>();
        RequestHeaderMetricFilter filter = new RequestHeaderMetricFilter(metricService);
        registrationBean.setFilter(filter);
        return registrationBean;
    }
}
