package com.sequenceiq.periscope.config;

import static com.sequenceiq.periscope.api.AutoscaleApi.API_ROOT_CONTEXT;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.periscope.domain.PeriscopeUser;
import com.sequenceiq.periscope.service.security.OwnerBasedPermissionEvaluator;
import com.sequenceiq.periscope.service.security.UserDetailsService;
import com.sequenceiq.periscope.service.security.UserFilterField;

@Configuration
public class SecurityConfig {

    @Inject
    private UserDetailsService userDetailsService;

    @Inject
    private OwnerBasedPermissionEvaluator ownerBasedPermissionEvaluator;

    @Bean
    MethodSecurityExpressionHandler expressionHandler() {
        DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
        ownerBasedPermissionEvaluator.setUserDetailsService(userDetailsService);
        expressionHandler.setPermissionEvaluator(ownerBasedPermissionEvaluator);
        return expressionHandler;
    }

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    protected static class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

        @Inject
        private MethodSecurityExpressionHandler expressionHandler;

        @Override
        protected MethodSecurityExpressionHandler createExpressionHandler() {
            return expressionHandler;
        }
    }

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Value("${periscope.client.id}")
        private String clientId;

        @Value("${periscope.client.secret}")
        private String clientSecret;

        @Autowired
        @Qualifier("identityServerUrl")
        private String identityServerUrl;

        @Autowired
        private UserDetailsService userDetailsService;

        @Bean
        RemoteTokenServices remoteTokenServices() {
            RemoteTokenServices rts = new RemoteTokenServices();
            rts.setClientId(clientId);
            rts.setClientSecret(clientSecret);
            rts.setCheckTokenEndpointUrl(identityServerUrl + "/check_token");
            return rts;
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
            resources.resourceId("periscope");
            resources.tokenServices(remoteTokenServices());
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterAfter(new ScimAccountGroupReaderFilter(userDetailsService), AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()
                    .antMatchers(API_ROOT_CONTEXT + "/clusters/**").access("#oauth2.hasScope('cloudbreak.stacks') and #oauth2.hasScope('periscope.cluster')")
                    .antMatchers(API_ROOT_CONTEXT + "/swagger.json").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/api-docs/**").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/**").denyAll()
                    .and()
                    .csrf()
                    .disable()
                    .headers()
                    .contentTypeOptions();
        }
    }

    private static class ScimAccountGroupReaderFilter extends OncePerRequestFilter {

        private UserDetailsService userDetailsService;

        ScimAccountGroupReaderFilter(UserDetailsService userDetailsService) {
            this.userDetailsService = userDetailsService;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException,
                IOException {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                PeriscopeUser user = userDetailsService.getDetails(username, UserFilterField.USERNAME);
                request.setAttribute("user", user);
            }
            filterChain.doFilter(request, response);
        }
    }

}
