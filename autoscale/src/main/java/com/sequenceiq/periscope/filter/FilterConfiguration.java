package com.sequenceiq.periscope.filter;

import jakarta.inject.Inject;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.filter.FilterOrderConstants;
import com.sequenceiq.periscope.service.AuditService;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;

@Configuration
public class FilterConfiguration {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private AuditService auditService;

    @Value("${altus.audit.enabled:false}")
    private boolean auditEnabled;

    @Value("${cb.audit.allEndpoints.enabled:false}")
    private boolean auditAllEndpointsEnabled;

    @Bean(name = "CloudbreakUserConfiguratorFilter")
    public FilterRegistrationBean<CloudbreakUserConfiguratorFilter> identityUserConfiguratorFilterRegistrationBean() {
        FilterRegistrationBean<CloudbreakUserConfiguratorFilter> registrationBean = new FilterRegistrationBean<>();
        CloudbreakUserConfiguratorFilter filter =
                new CloudbreakUserConfiguratorFilter(restRequestThreadLocalService, authenticatedUserService);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(FilterOrderConstants.CLOUDBREAK_USER_CONFIGURATOR_ORDER);
        return registrationBean;
    }

    @Bean(name = "AuditFilter")
    public FilterRegistrationBean<AuditFilter> auditFilterRegistrationBean() {
        FilterRegistrationBean<AuditFilter> registrationBean = new FilterRegistrationBean<>();
        AuditFilter filter = new AuditFilter(auditEnabled, auditService, authenticatedUserService, auditAllEndpointsEnabled);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(FilterOrderConstants.AUDIT_FILTER_ORDER);
        return registrationBean;
    }
}
