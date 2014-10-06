package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
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

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;

@Configuration
public class SecurityConfig {

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    protected static class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

        @Autowired
        private UserDetailsService userDetailsService;

        @Autowired
        private OwnerBasedPermissionEvaluator ownerBasedPermissionEvaluator;

        @Bean
        MethodSecurityExpressionHandler expressionHandler() {
            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
            ownerBasedPermissionEvaluator.setUserDetailsService(userDetailsService);
            expressionHandler.setPermissionEvaluator(ownerBasedPermissionEvaluator);
            return expressionHandler;
        }

        @Override
        protected MethodSecurityExpressionHandler createExpressionHandler() {
            return expressionHandler();
        }
    }

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Value("${cb.client.id}")
        private String clientId;

        @Value("${cb.client.secret}")
        private String clientSecret;

        @Value("${cb.identity.server.url}")
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
            resources.resourceId("cloudbreak");
            resources.tokenServices(remoteTokenServices());
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.csrf()
                    .disable()
                    .headers()
                    .contentTypeOptions()
                    .and()
                    .addFilterAfter(new ScimAccountGroupReaderFilter(userDetailsService), AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()
                    .antMatchers("/user/blueprints").access("#oauth2.hasScope('cloudbreak.blueprints')")
                    .antMatchers("/account/blueprints").access("#oauth2.hasScope('cloudbreak.blueprints')")
                    .antMatchers("/blueprints/**").access("#oauth2.hasScope('cloudbreak.blueprints')")
                    .antMatchers("/user/templates").access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers("/account/templates").access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers("/templates/**").access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers("/user/credentials/**").access("#oauth2.hasScope('cloudbreak.credentials')")
                    .antMatchers("/account/credentials/**").access("#oauth2.hasScope('cloudbreak.credentials')")
                    .antMatchers("/credentials/**").access("#oauth2.hasScope('cloudbreak.credentials')")
                    .antMatchers("/user/stacks/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers("/account/stacks/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers("/stacks/*").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers("/stacks/*/cluster/**").access("#oauth2.hasScope('cloudbreak.stacks')");
        }
    }

    private static class ScimAccountGroupReaderFilter extends OncePerRequestFilter {

        private UserDetailsService userDetailsService;

        public ScimAccountGroupReaderFilter(UserDetailsService userDetailsService) {
            this.userDetailsService = userDetailsService;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException,
                IOException {
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                CbUser user = userDetailsService.getDetails(username);
                request.setAttribute("user", user);
            }
            filterChain.doFilter(request, response);
        }
    }

}