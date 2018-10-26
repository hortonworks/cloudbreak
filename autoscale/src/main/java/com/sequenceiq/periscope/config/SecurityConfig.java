package com.sequenceiq.periscope.config;

import static com.sequenceiq.periscope.api.AutoscaleApi.API_ROOT_CONTEXT;

import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.expression.OAuth2MethodSecurityExpressionHandler;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import com.sequenceiq.cloudbreak.service.security.ScimAccountGroupReaderFilter;
import com.sequenceiq.periscope.service.security.TenantBasedPermissionEvaluator;

@Configuration
public class SecurityConfig {

    @Value("${periscope.client.secret}")
    private String clientSecret;

    @Bean("PBEStringCleanablePasswordEncryptor")
    @Scope("prototype")
    public PBEStringCleanablePasswordEncryptor encryptor() {
        PBEStringCleanablePasswordEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword(clientSecret);
        return encryptor;
    }

    @Bean("LegacyPBEStringCleanablePasswordEncryptor")
    @Scope("prototype")
    public PBEStringCleanablePasswordEncryptor legacyEncryptor() {
        PBEStringCleanablePasswordEncryptor encryptor = new StandardPBEStringEncryptor();
        encryptor.setPassword("cbsecret2015");
        return encryptor;
    }

    @EnableGlobalMethodSecurity(prePostEnabled = true)
    protected static class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

        @Inject
        @Lazy
        private TenantBasedPermissionEvaluator tenantBasedPermissionEvaluator;

        @Override
        protected MethodSecurityExpressionHandler createExpressionHandler() {
            OAuth2MethodSecurityExpressionHandler expressionHandler = new OAuth2MethodSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(tenantBasedPermissionEvaluator);
            return expressionHandler;
        }
    }

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        @Inject
        private ScimAccountGroupReaderFilter scimAccountGroupReaderFilter;

        @Inject
        private ResourceServerTokenServices resourceServerTokenServices;

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId("periscope");
            resources.tokenServices(resourceServerTokenServices);
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterAfter(scimAccountGroupReaderFilter, AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()
                    .antMatchers(API_ROOT_CONTEXT + "/v1/clusters/**").access("#oauth2.hasScope('cloudbreak.stacks') and #oauth2.hasScope('periscope.cluster')")
                    .antMatchers(API_ROOT_CONTEXT + "/v2/clusters/**").access("#oauth2.hasScope('cloudbreak.stacks') and #oauth2.hasScope('periscope.cluster')")
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
}
