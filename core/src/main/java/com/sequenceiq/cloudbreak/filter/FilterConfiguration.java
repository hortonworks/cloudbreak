package com.sequenceiq.cloudbreak.filter;

import javax.inject.Inject;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.service.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.RestRequestThreadLocalService;
import com.sequenceiq.cloudbreak.service.organization.OrganizationService;
import com.sequenceiq.cloudbreak.service.user.UserService;

@Configuration
public class FilterConfiguration {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private RestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private OrganizationService organizationService;

    @Inject
    private UserService userService;

    @Bean
    public FilterRegistrationBean<OrganizationConfiguratorFilter> organizationConfiguratorFilterRegistrationBean() {
        FilterRegistrationBean<OrganizationConfiguratorFilter> registrationBean = new FilterRegistrationBean<>();
        OrganizationConfiguratorFilter filter = new OrganizationConfiguratorFilter(restRequestThreadLocalService, organizationService, userService,
                authenticatedUserService);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(Integer.MAX_VALUE);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<IdentityUserConfiguratorFilter> identityUserConfiguratorFilterRegistrationBean() {
        FilterRegistrationBean<IdentityUserConfiguratorFilter> registrationBean = new FilterRegistrationBean<>();
        IdentityUserConfiguratorFilter filter = new IdentityUserConfiguratorFilter(restRequestThreadLocalService, authenticatedUserService);
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

//    @Bean
//    public FilterRegistrationBean securityFilterChain(@Qualifier(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME) Filter securityFilter) {
//        FilterRegistrationBean registration = new FilterRegistrationBean<>(securityFilter);
//        registration.setOrder(Integer.MAX_VALUE - 1);
//        registration.setName(AbstractSecurityWebApplicationInitializer.DEFAULT_FILTER_NAME);
//        return registration;
//    }
}
