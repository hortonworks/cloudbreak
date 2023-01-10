package com.sequenceiq.cloudbreak.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.CrnFilter;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.metrics.RequestHeaderMetricFilter;
import com.sequenceiq.cloudbreak.logger.MDCContextFilter;
import com.sequenceiq.cloudbreak.logger.MDCRequestIdOnlyFilter;
import com.sequenceiq.cloudbreak.logger.RestLoggerFilter;

@Configuration
public class CommonFilterConfiguration {

    @Value("${rest.logger.enabled:true}")
    private boolean restLoggerEnabled;

    @Bean
    public FilterRegistrationBean<CrnFilter> crnFilterRegistrationBean() {
        FilterRegistrationBean<CrnFilter> registrationBean = new FilterRegistrationBean<>();
        CrnFilter filter = new CrnFilter();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(FilterOrderConstants.CRN_FILTER_ORDER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<MDCContextFilter> mdcContextFilterRegistrationBean() {
        FilterRegistrationBean<MDCContextFilter> registrationBean = new FilterRegistrationBean<>();
        MDCContextFilter filter = new MDCContextFilter();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(FilterOrderConstants.MDC_FILTER_ORDER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<MDCRequestIdOnlyFilter> mdcContextRequestIdFilterRegistrationBean() {
        FilterRegistrationBean<MDCRequestIdOnlyFilter> registrationBean = new FilterRegistrationBean<>();
        MDCRequestIdOnlyFilter filter = new MDCRequestIdOnlyFilter();
        registrationBean.setFilter(filter);
        registrationBean.setOrder(FilterOrderConstants.MDC_REQUEST_ID_FILTER_ORDER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<RestLoggerFilter> restLoggerFilterFilterRegistrationBean() {
        FilterRegistrationBean<RestLoggerFilter> registrationBean = new FilterRegistrationBean<>();
        RestLoggerFilter filter = new RestLoggerFilter(restLoggerEnabled);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(FilterOrderConstants.REQUEST_RESPONSE_LOGGER_FILTER_ORDER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<RequestHeaderMetricFilter> requestHeaderMetricFilter(MetricService metricService) {
        FilterRegistrationBean<RequestHeaderMetricFilter> registrationBean = new FilterRegistrationBean<>();
        RequestHeaderMetricFilter filter = new RequestHeaderMetricFilter(metricService);
        registrationBean.setFilter(filter);
        return registrationBean;
    }
}
