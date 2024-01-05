package com.sequenceiq.periscope.config;

import static com.sequenceiq.periscope.api.AutoscaleApi.API_ROOT_CONTEXT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

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
    protected static class ResourceServerConfiguration {

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

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http.addFilterAfter(headerAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class);
            http.authorizeHttpRequests(req -> {
                req.requestMatchers(AntPathRequestMatcher.antMatcher(API_ROOT_CONTEXT + "/v1/clusters/**")).authenticated();
                req.requestMatchers(AntPathRequestMatcher.antMatcher(API_ROOT_CONTEXT + "/v2/clusters/**")).authenticated();
                req.requestMatchers(AntPathRequestMatcher.antMatcher(API_ROOT_CONTEXT + "/v1/distrox/**")).authenticated();
                req.requestMatchers(AntPathRequestMatcher.antMatcher(API_ROOT_CONTEXT + "/v1/scaling_activities/**")).authenticated();
                req.requestMatchers(AntPathRequestMatcher.antMatcher(API_ROOT_CONTEXT + "/v1/yarn_recommendation/**")).authenticated();
                req.requestMatchers(AntPathRequestMatcher.antMatcher(API_ROOT_CONTEXT + "/authorization/**")).authenticated();
                req.requestMatchers(AntPathRequestMatcher.antMatcher(API_ROOT_CONTEXT + "/openapi.json")).permitAll();
                req.requestMatchers(AntPathRequestMatcher.antMatcher("/info")).permitAll();
                req.requestMatchers(AntPathRequestMatcher.antMatcher("/health")).permitAll();
                req.requestMatchers(AntPathRequestMatcher.antMatcher("/metrics")).permitAll();
                req.requestMatchers(AntPathRequestMatcher.antMatcher(API_ROOT_CONTEXT + "/**")).denyAll();
            });
            http.csrf(AbstractHttpConfigurer::disable);
            http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            http.headers(h -> h.contentTypeOptions(t -> { }));
            http.exceptionHandling(c -> c.authenticationEntryPoint(new CloudbreakAuthenticationEntryPoint(errorResponseHandler)));
            return http.build();
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
