package com.sequenceiq.periscope.filter;

import javax.inject.Inject;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.filter.CloudbreakUserConfiguratorFilter;
import com.sequenceiq.cloudbreak.filter.MDCContextFilter;
import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;

@Configuration
public class FilterConfiguration {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Bean
    public FilterRegistrationBean<CloudbreakUserConfiguratorFilter> identityUserConfiguratorFilterRegistrationBean() {
        FilterRegistrationBean<CloudbreakUserConfiguratorFilter> registrationBean = new FilterRegistrationBean<>();
        CloudbreakUserConfiguratorFilter filter = new CloudbreakUserConfiguratorFilter(restRequestThreadLocalService, authenticatedUserService);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<MDCContextFilter> mdcContextFilterRegistrationBean() {
        FilterRegistrationBean<MDCContextFilter> registrationBean = new FilterRegistrationBean<>();
        MDCContextFilter userFilter = new MDCContextFilter(authenticatedUserService);
        registrationBean.setFilter(userFilter);
        registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
    }
}
