package com.sequenceiq.periscope.config;

import static com.sequenceiq.periscope.api.AutoscaleApi.API_ROOT_CONTEXT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.cloudbreak.exception.ErrorResponseHandler;
import com.sequenceiq.periscope.service.security.TenantBasedPermissionEvaluator;

@Configuration
public class SecurityConfig {

    @EnableGlobalMethodSecurity(prePostEnabled = true)
    protected static class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

        @Inject
        @Lazy
        private TenantBasedPermissionEvaluator tenantBasedPermissionEvaluator;

        @Override
        protected MethodSecurityExpressionHandler createExpressionHandler() {
            DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(tenantBasedPermissionEvaluator);
            return expressionHandler;
        }
    }

    @Configuration
    protected static class ResourceServerConfiguration extends WebSecurityConfigurerAdapter {

        @Inject
        private GrpcUmsClient umsClient;

        @Inject
        private ErrorResponseHandler errorResponseHandler;

        @Bean
        public RequestHeaderAuthenticationFilter headerAuthenticationFilter() throws Exception {
            RequestHeaderAuthenticationFilter requestHeaderAuthenticationFilter = new RequestHeaderAuthenticationFilter();
            requestHeaderAuthenticationFilter.setPrincipalRequestHeader("x-cdp-actor-crn");
            requestHeaderAuthenticationFilter.setAuthenticationManager(authenticationManager());
            requestHeaderAuthenticationFilter.setExceptionIfHeaderMissing(false);
            requestHeaderAuthenticationFilter.setContinueFilterChainOnUnsuccessfulAuthentication(true);
            return requestHeaderAuthenticationFilter;
        }

        @Bean
        public UserDetailsService userDetailsService() {
            return new CrnUserDetailsService(umsClient);
        }

        @Bean
        @Override
        protected AuthenticationManager authenticationManager() throws Exception {
            List<AuthenticationProvider> providers = new ArrayList<>(1);
            providers.add(preAuthAuthProvider());
            return new ProviderManager(providers);
        }

        @Bean
        public PreAuthenticatedAuthenticationProvider preAuthAuthProvider() throws Exception {
            PreAuthenticatedAuthenticationProvider provider = new PreAuthenticatedAuthenticationProvider();
            provider.setPreAuthenticatedUserDetailsService(userDetailsServiceWrapper());
            return provider;
        }

        @Bean
        public UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> userDetailsServiceWrapper() throws Exception {
            UserDetailsByNameServiceWrapper<PreAuthenticatedAuthenticationToken> wrapper = new UserDetailsByNameServiceWrapper<>();
            wrapper.setUserDetailsService(userDetailsService());
            return wrapper;
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterAfter(headerAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()
                    .antMatchers(API_ROOT_CONTEXT + "/v1/clusters/**").authenticated()
                    .antMatchers(API_ROOT_CONTEXT + "/v2/clusters/**").authenticated()
                    .antMatchers(API_ROOT_CONTEXT + "/v1/distrox/**").authenticated()
                    .antMatchers(API_ROOT_CONTEXT + "/v1/scaling_activities/**").authenticated()
                    .antMatchers(API_ROOT_CONTEXT + "/v1/yarn_recommendation/**").authenticated()
                    .antMatchers(API_ROOT_CONTEXT + "/authorization/**").authenticated()
                    .antMatchers(API_ROOT_CONTEXT + "/openapi.json").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/**").denyAll()
                    .and()
                    .csrf()
                    .disable()
                    .headers()
                    .contentTypeOptions();

            http.csrf().disable();
            http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
            http.headers().contentTypeOptions();
            http.exceptionHandling().authenticationEntryPoint(new CloudbreakAuthenticationEntryPoint(errorResponseHandler));
        }
    }

    private static class CloudbreakAuthenticationEntryPoint implements AuthenticationEntryPoint {

        private ErrorResponseHandler errorResponseHandler;

        CloudbreakAuthenticationEntryPoint(ErrorResponseHandler errorResponseHandler) {
            this.errorResponseHandler = errorResponseHandler;
        }

        @Override
        public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authenticationException) throws IOException {
            errorResponseHandler.handleErrorResponse(response, authenticationException);
        }
    }
}
