package com.sequenceiq.cloudbreak.conf;

import java.io.IOException;

import javax.inject.Inject;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import com.sequenceiq.cloudbreak.domain.CbUser;
import com.sequenceiq.cloudbreak.service.user.UserDetailsService;
import com.sequenceiq.cloudbreak.service.user.UserFilterField;

@Configuration
public class SecurityConfig {

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true)
    protected static class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

        @Inject
        private UserDetailsService userDetailsService;

        @Inject
        private OwnerBasedPermissionEvaluator ownerBasedPermissionEvaluator;

        @Bean MethodSecurityExpressionHandler expressionHandler() {
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

        public static final Logger LOGGER = LoggerFactory.getLogger(ResourceServerConfiguration.class);

        @Value("${cb.client.id}")
        private String clientId;

        @Value("${cb.client.secret}")
        private String clientSecret;

        @Value("${cb.identity.server.url}")
        private String identityServerUrl;

        @Inject
        private UserDetailsService userDetailsService;

        @Bean RemoteTokenServices remoteTokenServices() {
            RemoteTokenServices rts = new RemoteTokenServices();
            rts.setClientId(clientId);
            rts.setClientSecret(clientSecret);
            rts.setCheckTokenEndpointUrl(identityServerUrl + "/check_token");
            return rts;
        }

        @Bean PBEStringCleanablePasswordEncryptor encryptor() {
            StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
            encryptor.setPassword(clientSecret);
            return encryptor;
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
                    .antMatchers("/stacks/ambari").access("#oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers("/stacks/*").access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers("/stacks/*/cluster/**").access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers("/events").access("#oauth2.hasScope('cloudbreak.events')")
                    .antMatchers("/usages/**").access("#oauth2.hasScope('cloudbreak.usages.global')")
                    .antMatchers("/account/usages/**").access("#oauth2.hasScope('cloudbreak.usages.account')")
                    .antMatchers("/user/usages/**").access("#oauth2.hasScope('cloudbreak.usages.user')")
                    .antMatchers("/subscription").access("#oauth2.hasScope('cloudbreak.subscribe')")
                    .antMatchers("/user/recipes").access("#oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers("/account/recipes").access("#oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers("/recipes").access("#oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers("/user/networks").access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers("/account/networks").access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers("/networks/**").access("#oauth2.hasScope('cloudbreak.templates')");
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
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null) {
                OAuth2Authentication oauth = (OAuth2Authentication) authentication;
                if (oauth.getUserAuthentication() != null) {
                    String username = (String) authentication.getPrincipal();
                    CbUser user = userDetailsService.getDetails(username, UserFilterField.USERNAME);
                    request.setAttribute("user", user);
                }
            }
            filterChain.doFilter(request, response);
        }
    }

}
