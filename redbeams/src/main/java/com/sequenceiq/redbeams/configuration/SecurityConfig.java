package com.sequenceiq.redbeams.configuration;

import static com.sequenceiq.redbeams.api.RedbeamsApi.API_ROOT_CONTEXT;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

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
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationProvider;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.authentication.preauth.RequestHeaderAuthenticationFilter;

import com.sequenceiq.cloudbreak.auth.altus.GrpcUmsClient;
import com.sequenceiq.cloudbreak.auth.security.CrnUserDetailsService;
import com.sequenceiq.redbeams.service.security.TenantBasedPermissionEvaluator;

@Configuration
public class SecurityConfig {

    // @Value("${redbeams.client.secret}")
    // private String clientSecret;

    // @Bean("PBEStringCleanablePasswordEncryptor")
    // @Scope("prototype")
    // public PBEStringCleanablePasswordEncryptor encryptor() {
    //     PBEStringCleanablePasswordEncryptor encryptor = new StandardPBEStringEncryptor();
    //     encryptor.setPassword(clientSecret);
    //     return encryptor;
    // }

    // @Bean("LegacyPBEStringCleanablePasswordEncryptor")
    // @Scope("prototype")
    // public PBEStringCleanablePasswordEncryptor legacyEncryptor() {
    //     PBEStringCleanablePasswordEncryptor encryptor = new StandardPBEStringEncryptor();
    //     encryptor.setPassword("cbsecret2015");
    //     return encryptor;
    // }

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

        private static final String V4_API = API_ROOT_CONTEXT + "/v4/**";

        @Inject
        private GrpcUmsClient umsClient;

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
            // http.addFilterAfter(scimAccountGroupReaderFilter, AbstractPreAuthenticatedProcessingFilter.class)
            http.addFilterAfter(headerAuthenticationFilter(), AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()
                    .antMatchers(V4_API)
                    .authenticated()
                    .antMatchers(API_ROOT_CONTEXT + "/swagger.json").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/api-docs/**").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/**").denyAll();

            http.csrf().disable();

            http.headers().contentTypeOptions();
        }
    }

}
