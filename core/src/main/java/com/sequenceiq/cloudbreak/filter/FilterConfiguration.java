package com.sequenceiq.cloudbreak.filter;

import jakarta.inject.Inject;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticatedUserService;
import com.sequenceiq.cloudbreak.service.user.UserService;
import com.sequenceiq.cloudbreak.service.workspace.CachedWorkspaceService;
import com.sequenceiq.cloudbreak.structuredevent.CloudbreakRestRequestThreadLocalService;

@Configuration
public class FilterConfiguration {

    @Inject
    private AuthenticatedUserService authenticatedUserService;

    @Inject
    private CloudbreakRestRequestThreadLocalService restRequestThreadLocalService;

    @Inject
    private CachedWorkspaceService workspaceService;

    @Inject
    private UserService userService;

    @Bean
    public FilterRegistrationBean<WorkspaceConfiguratorFilter> workspaceConfiguratorFilterRegistrationBean() {
        FilterRegistrationBean<WorkspaceConfiguratorFilter> registrationBean = new FilterRegistrationBean<>();
        WorkspaceConfiguratorFilter filter = new WorkspaceConfiguratorFilter(restRequestThreadLocalService, workspaceService, userService,
                authenticatedUserService);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(FilterOrderConstants.WORKSPACE_FILTER_ORDER);
        return registrationBean;
    }

    @Bean
    public FilterRegistrationBean<CloudbreakUserConfiguratorFilter> identityUserConfiguratorFilterRegistrationBean() {
        FilterRegistrationBean<CloudbreakUserConfiguratorFilter> registrationBean = new FilterRegistrationBean<>();
        CloudbreakUserConfiguratorFilter filter = new CloudbreakUserConfiguratorFilter(userService, restRequestThreadLocalService, authenticatedUserService);
        registrationBean.setFilter(filter);
        registrationBean.setOrder(FilterOrderConstants.CLOUDBREAK_USER_CONFIGURATOR_ORDER);
        return registrationBean;
    }
}
