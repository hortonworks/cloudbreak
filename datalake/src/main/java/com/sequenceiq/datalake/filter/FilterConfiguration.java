package com.sequenceiq.datalake.filter;

import javax.inject.Inject;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.datalake.util.RestRequestThreadLocalService;

@Configuration
public class FilterConfiguration {

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Bean
    public FilterRegistrationBean<CrnFilter> crnFilterRegistrationBean() {
        FilterRegistrationBean<CrnFilter> registrationBean = new FilterRegistrationBean<>();
        CrnFilter filter = new CrnFilter(restRequestThreadLocalService);
        registrationBean.setFilter(filter);
            registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
    }

}
