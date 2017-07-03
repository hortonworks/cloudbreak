package com.sequenceiq.cloudbreak.conf;

import static com.sequenceiq.cloudbreak.api.CoreApi.API_ROOT_CONTEXT;

import javax.inject.Inject;

import org.jasypt.encryption.pbe.PBEStringCleanablePasswordEncryptor;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
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

import com.sequenceiq.cloudbreak.service.security.OwnerBasedPermissionEvaluator;
import com.sequenceiq.cloudbreak.service.security.ScimAccountGroupReaderFilter;

@Configuration
public class SecurityConfig {

    @EnableGlobalMethodSecurity(prePostEnabled = true)
    protected static class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

        @Inject
        @Lazy
        private OwnerBasedPermissionEvaluator ownerBasedPermissionEvaluator;

        @Override
        protected MethodSecurityExpressionHandler createExpressionHandler() {
            OAuth2MethodSecurityExpressionHandler expressionHandler = new OAuth2MethodSecurityExpressionHandler();
            expressionHandler.setPermissionEvaluator(ownerBasedPermissionEvaluator);
            return expressionHandler;
        }
    }

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {
        private static final String[] BLUEPRINT_URL_PATTERNS = new String[]{API_ROOT_CONTEXT + "/blueprints/**"};

        private static final String[] TEMPLATE_URL_PATTERNS = new String[]{API_ROOT_CONTEXT + "/templates/**"};

        private static final String[] CREDENTIAL_URL_PATTERNS = new String[]{API_ROOT_CONTEXT + "/credentials/**"};

        private static final String[] RECIPE_URL_PATTERNS = new String[]{API_ROOT_CONTEXT + "/recipes/**"};

        private static final String[] NETWORK_URL_PATTERNS = new String[]{API_ROOT_CONTEXT + "/networks/**"};

        private static final String[] SECURITYGROUP_URL_PATTERNS = new String[]{API_ROOT_CONTEXT + "/securitygroups/**"};

        private static final String[] STACK_URL_PATTERNS = new String[]{API_ROOT_CONTEXT + "/stacks/**"};

        private static final String[] STACK_TEMPLATE_URL_PATTERNS = new String[]{API_ROOT_CONTEXT + "/clustertemplates/**"};

        private static final String ACCOUNT_PREFERENCES = API_ROOT_CONTEXT + "/accountpreferences/**";

        @Value("${cb.client.secret}")
        private String clientSecret;

        @Inject
        private ResourceServerTokenServices resourceServerTokenServices;

        @Inject
        private ScimAccountGroupReaderFilter scimAccountGroupReaderFilter;

        @Bean("PBEStringCleanablePasswordEncryptor")
        public PBEStringCleanablePasswordEncryptor encryptor() {
            StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
            encryptor.setPassword(clientSecret);
            return encryptor;
        }

        @Bean("LegacyPBEStringCleanablePasswordEncryptor")
        public PBEStringCleanablePasswordEncryptor legacyEncryptor() {
            StandardPBEStringEncryptor encryptor = new StandardPBEStringEncryptor();
            encryptor.setPassword("cbsecret2015");
            return encryptor;
        }

        @Override
        public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
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
                    .antMatchers(HttpMethod.GET, NETWORK_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.networks.read') or #oauth2.hasScope('cloudbreak.networks')")
                    .antMatchers(HttpMethod.GET, SECURITYGROUP_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.securitygroups.read') or #oauth2.hasScope('cloudbreak.securitygroups')")
                    .antMatchers(HttpMethod.GET, STACK_URL_PATTERNS)
                    .access("#oauth2.hasScope('cloudbreak.stacks.read') or #oauth2.hasScope('cloudbreak.stacks')"
                            + " or #oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers(HttpMethod.GET, ACCOUNT_PREFERENCES)
                    .permitAll()

                    .antMatchers(API_ROOT_CONTEXT + "/users/**").access("#oauth2.hasScope('openid')")
                    .antMatchers(BLUEPRINT_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.blueprints')")
                    .antMatchers(TEMPLATE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.templates')")
                    .antMatchers(CREDENTIAL_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.credentials')")
                    .antMatchers(RECIPE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers(NETWORK_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.networks')")
                    .antMatchers(SECURITYGROUP_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.securitygroups')")
                    .antMatchers(STACK_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers(STACK_TEMPLATE_URL_PATTERNS).access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(ACCOUNT_PREFERENCES)
                    .access("#oauth2.hasScope('cloudbreak.templates') and #oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/stacks/ambari", API_ROOT_CONTEXT + "/stacks/*/certificate", API_ROOT_CONTEXT + "/stacks/all")
                    .access("#oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers(API_ROOT_CONTEXT + "/events/**").access("#oauth2.hasScope('cloudbreak.events')")
                    .antMatchers(API_ROOT_CONTEXT + "/usages/account/**").access("#oauth2.hasScope('cloudbreak.usages.account')")
                    .antMatchers(API_ROOT_CONTEXT + "/usages/user/**").access("#oauth2.hasScope('cloudbreak.usages.user')")
                    .antMatchers(API_ROOT_CONTEXT + "/usages/flex/**").access("#oauth2.hasScope('cloudbreak.flex')")
                    .antMatchers(API_ROOT_CONTEXT + "/usages/**").access("#oauth2.hasScope('cloudbreak.usages.global')")
                    .antMatchers(API_ROOT_CONTEXT + "/subscriptions").access("#oauth2.hasScope('cloudbreak.subscribe')")
                    .antMatchers(API_ROOT_CONTEXT + "/constraints/**")
                    .access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers(API_ROOT_CONTEXT + "/topologies/**")
                    .access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.autoscale')")
                    .antMatchers(API_ROOT_CONTEXT + "/settings/**")
                    .access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers(API_ROOT_CONTEXT + "/sssd/**")
                    .access("#oauth2.hasScope('cloudbreak.stacks') or #oauth2.hasScope('cloudbreak.recipes')")
                    .antMatchers(API_ROOT_CONTEXT + "/ldap/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/util/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/rdsconfigs/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/smartsensesubscriptions/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/flexsubscriptions/**").access("#oauth2.hasScope('cloudbreak.stacks')")
                    .antMatchers(API_ROOT_CONTEXT + "/swagger.json").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/api-docs/**").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/connectors/**").permitAll()

                    .antMatchers(API_ROOT_CONTEXT + "/**").denyAll();

            http.csrf().disable();

            http.headers().contentTypeOptions();
        }
    }

}
