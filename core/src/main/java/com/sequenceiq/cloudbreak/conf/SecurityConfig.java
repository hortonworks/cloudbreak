package com.sequenceiq.cloudbreak.conf;

import static com.sequenceiq.cloudbreak.api.CoreApi.API_ROOT_CONTEXT;

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

import com.sequenceiq.cloudbreak.auth.altus.CrnTokenExtractor;
import com.sequenceiq.cloudbreak.service.security.ScimAccountGroupReaderFilter;
import com.sequenceiq.cloudbreak.service.security.TenantBasedPermissionEvaluator;

@Configuration
public class SecurityConfig {

    @Value("${cb.client.secret}")
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

        private static final String V4_API = API_ROOT_CONTEXT + "/v4/**";

        private static final String AUTOSCALE_API = API_ROOT_CONTEXT + "/autoscale/**";

        @Inject
        private ResourceServerTokenServices resourceServerTokenServices;

        @Inject
        private ScimAccountGroupReaderFilter scimAccountGroupReaderFilter;

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId("cloudbreak");
            resources.tokenServices(resourceServerTokenServices);
            resources.tokenExtractor(new CrnTokenExtractor());
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterAfter(scimAccountGroupReaderFilter, AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()
                    .antMatchers(V4_API)
                    .access("#oauth2.isOAuth()")
                    .antMatchers(AUTOSCALE_API)
                    .access("#oauth2.hasScope('cloudbreak.autoscale')")

                    .antMatchers(API_ROOT_CONTEXT + "/swagger.json").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/api-docs/**").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/**").denyAll();

            http.csrf().disable();

            http.headers().contentTypeOptions();
        }
    }

}
