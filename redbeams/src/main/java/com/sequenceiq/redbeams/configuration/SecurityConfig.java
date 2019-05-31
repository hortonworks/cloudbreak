package com.sequenceiq.redbeams.configuration;

import static com.sequenceiq.redbeams.api.RedbeamsApi.API_ROOT_CONTEXT;

import javax.inject.Inject;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import com.sequenceiq.cloudbreak.auth.security.ScimAccountGroupReaderFilter;
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
        private ScimAccountGroupReaderFilter scimAccountGroupReaderFilter;

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterAfter(scimAccountGroupReaderFilter, AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()
                    .antMatchers(V4_API)
                    .access("#oauth2.isOAuth()")
                    // .antMatchers(REDBEAMS_API)
                    // .access("#oauth2.hasScope('cloudbreak.autoscale')")

                    .antMatchers(API_ROOT_CONTEXT + "/swagger.json").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/api-docs/**").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/**").denyAll();

            http.csrf().disable();

            http.headers().contentTypeOptions();
        }
    }

}
