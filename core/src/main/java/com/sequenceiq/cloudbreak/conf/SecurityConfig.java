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
import org.springframework.http.HttpMethod;
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
        private static final String[] BLUEPRINT_URL_PATTERNS = {API_ROOT_CONTEXT + "/v1/blueprints/**"};

        private static final String[] TEMPLATE_URL_PATTERNS = {API_ROOT_CONTEXT + "/v1/templates/**"};

        private static final String[] CREDENTIAL_URL_PATTERNS = {API_ROOT_CONTEXT + "/v1/credentials/**"};

        private static final String[] RECIPE_URL_PATTERNS = {API_ROOT_CONTEXT + "/v1/recipes/**"};

        private static final String[] MPACK_URL_PATTERNS = {API_ROOT_CONTEXT + "/v1/mpacks/**"};

        private static final String[] NETWORK_URL_PATTERNS = {API_ROOT_CONTEXT + "/v1/networks/**"};

        private static final String[] SECURITYGROUP_URL_PATTERNS = {API_ROOT_CONTEXT + "/v1/securitygroups/**"};

        private static final String[] STACK_URL_PATTERNS = {API_ROOT_CONTEXT + "/v1/stacks/**", API_ROOT_CONTEXT + "/v2/stacks/**"};

        private static final String[] STACK_TEMPLATE_URL_PATTERNS = {API_ROOT_CONTEXT + "/v1/clustertemplates/**"};

        private static final String ACCOUNT_PREFERENCES = API_ROOT_CONTEXT + "/v1/accountpreferences/**";

        private static final String IMAGE_CATALOG_PATTERN = API_ROOT_CONTEXT + "/v1/imagecatalogs/**";

        private static final String SECURITY_RULE_URL_PATTERNS = API_ROOT_CONTEXT + "/v1/securityrules/**";

        private static final String V3_API = API_ROOT_CONTEXT + "/v3/**";

        private static final String AUTOSCALE_API = API_ROOT_CONTEXT + "/autoscale/**";

        @Inject
        private ResourceServerTokenServices resourceServerTokenServices;

        @Inject
        private ScimAccountGroupReaderFilter scimAccountGroupReaderFilter;

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) {
            resources.resourceId("cloudbreak");
            resources.tokenServices(resourceServerTokenServices);
        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterAfter(scimAccountGroupReaderFilter, AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()

                    .antMatchers(HttpMethod.GET, BLUEPRINT_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.blueprints.read') or #oauth2.hasScope('cloudbreak.blueprints')")
                    .antMatchers(HttpMethod.GET, STACK_TEMPLATE_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.stacks.read') or #oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(HttpMethod.GET, TEMPLATE_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.templates.read') or #oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers(HttpMethod.GET, CREDENTIAL_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.credentials.read') or #oauth2.hasScope('cloudbreak.credentials')")
                    .antMatchers(HttpMethod.GET, RECIPE_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.recipes.read') or #oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers(HttpMethod.GET, MPACK_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.recipes.read') or #oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers(HttpMethod.GET, NETWORK_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.networks.read') or #oauth2.hasScope('cloudbreak.networks')")
                    .antMatchers(HttpMethod.GET, SECURITYGROUP_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.securitygroups.read') or #oauth2.hasScope('cloudbreak.securitygroups')")
                    .antMatchers(HttpMethod.GET, SECURITY_RULE_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.securitygroups.read') or #oauth2.hasScope('cloudbreak.securitygroups')")
                    .antMatchers(HttpMethod.GET, STACK_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.stacks.read') or #oauth2.hasScope('cloudbreak.stacks')"
                            + " or #oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers(HttpMethod.GET, IMAGE_CATALOG_PATTERN)
                    .access("#oauth2.hasScope('cloudbreak.templates.read') or #oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers(HttpMethod.GET, ACCOUNT_PREFERENCES)
                    .permitAll()
                    .antMatchers(V3_API)
                    .permitAll()
                    .antMatchers(AUTOSCALE_API)
                    .access("#oauth2.hasScope('cloudbreak.autoscale')")

                    .antMatchers(API_ROOT_CONTEXT + "/v1/users/**").access("#oauth2.hasScope('openid')")
                    .antMatchers(BLUEPRINT_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.blueprints')")
                    .antMatchers(TEMPLATE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers(CREDENTIAL_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.credentials')")
                    .antMatchers(RECIPE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers(MPACK_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers(NETWORK_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.networks')")
                    .antMatchers(SECURITYGROUP_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.securitygroups')")
                    .antMatchers(STACK_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers(STACK_TEMPLATE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(SECURITY_RULE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.securitygroups')")
                    .antMatchers(ACCOUNT_PREFERENCES)
                    .access("#oauth2.hasScope('cloudbreak.templates') and #oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(IMAGE_CATALOG_PATTERN).access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/events/**").access("#oauth2.hasScope('cloudbreak.events')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/audits/**").access("#oauth2.hasScope('cloudbreak.events')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/usages/account/**").access("#oauth2.hasScope('cloudbreak.usages.account')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/usages/user/**").access("#oauth2.hasScope('cloudbreak.usages.user')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/usages/flex/**").access("#oauth2.hasScope('cloudbreak.flex')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/usages/**").access("#oauth2.hasScope('cloudbreak.usages.global')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/subscriptions").access("#oauth2.hasScope('cloudbreak.subscribe')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/constraints/**")
                    .access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/topologies/**")
                    .access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/settings/**")
                    .access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/ldap/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/util/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/rdsconfigs/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/proxyconfigs/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/smartsensesubscriptions/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/flexsubscriptions/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/connectors/**").access("#oauth2.hasScope('cloudbreak.credentials')")
                    .antMatchers(API_ROOT_CONTEXT + "/v2/connectors/**").access("#oauth2.hasScope('cloudbreak.credentials')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/workspaces/**").access("#oauth2.hasScope('cloudbreak.credentials')")
                    .antMatchers(API_ROOT_CONTEXT + "/v1/repositoryconfigs/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/swagger.json").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/api-docs/**").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/**").denyAll();

            http.csrf().disable();

            http.headers().contentTypeOptions();
        }
    }

}
