package com.sequenceiq.environment.configuration;

import static com.sequenceiq.cloudbreak.auth.security.authentication.AuthenticationModeConfiguration.CB_AUTHENTICATION_DISABLED;
import static com.sequenceiq.environment.api.EnvironmentApi.API_ROOT_CONTEXT;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
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
import com.sequenceiq.cloudbreak.auth.security.AccountIdBasedPermissionEvaluator;
import com.sequenceiq.cloudbreak.auth.security.ScimAccountGroupReaderFilter;

@Configuration
public class SecurityConfig {

    @EnableGlobalMethodSecurity(prePostEnabled = true)
    protected static class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

        private final AccountIdBasedPermissionEvaluator accountIdBasedPermissionEvaluator;

        public MethodSecurityConfig(AccountIdBasedPermissionEvaluator accountIdBasedPermissionEvaluator) {
            this.accountIdBasedPermissionEvaluator = accountIdBasedPermissionEvaluator;
        }

        @Override
        protected MethodSecurityExpressionHandler createExpressionHandler() {
            OAuth2MethodSecurityExpressionHandler expressionHandler = new OAuth2MethodSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(accountIdBasedPermissionEvaluator);
            return expressionHandler;
        }
    }

    @Configuration
    @EnableResourceServer
    @ConditionalOnProperty(name = CB_AUTHENTICATION_DISABLED, havingValue = "true")
    protected static class UnsecuredResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        private final ScimAccountGroupReaderFilter scimAccountGroupReaderFilter;

        public UnsecuredResourceServerConfiguration(ResourceServerTokenServices resourceServerTokenServices,
                ScimAccountGroupReaderFilter scimAccountGroupReaderFilter) {
            this.scimAccountGroupReaderFilter = scimAccountGroupReaderFilter;
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId("environment");
            resources.tokenExtractor(new CrnTokenExtractor());
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.csrf().disable()
                    .authorizeRequests()
                    .antMatchers("/**")
                    .permitAll();
        }
    }

    @Configuration
    @EnableResourceServer
    @ConditionalOnMissingBean(UnsecuredResourceServerConfiguration.class)
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        private static final String V1_API = API_ROOT_CONTEXT + "/v1/**";

        private final ResourceServerTokenServices resourceServerTokenServices;

        private final ScimAccountGroupReaderFilter scimAccountGroupReaderFilter;

        public ResourceServerConfiguration(ResourceServerTokenServices resourceServerTokenServices,
                ScimAccountGroupReaderFilter scimAccountGroupReaderFilter) {
            this.resourceServerTokenServices = resourceServerTokenServices;
            this.scimAccountGroupReaderFilter = scimAccountGroupReaderFilter;
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId("environment");
            resources.tokenServices(resourceServerTokenServices);
            resources.tokenExtractor(new CrnTokenExtractor());
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterAfter(scimAccountGroupReaderFilter, AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()
                    .antMatchers(V1_API)
                    .access("#oauth2.isOAuth()")

                    .antMatchers(API_ROOT_CONTEXT + "/swagger.json").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/api-docs/**").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/**").denyAll();

            http.csrf().disable();
            http.headers().contentTypeOptions();
        }
    }

}
