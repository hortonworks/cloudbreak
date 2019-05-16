package com.sequenceiq.environment.configuration;

import static com.sequenceiq.environment.api.EnvironmentApi.API_ROOT_CONTEXT;

import javax.inject.Inject;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

import com.sequenceiq.cloudbreak.security.ScimAccountGroupReaderFilter;
//import com.sequenceiq.cloudbreak.service.security.TenantBasedPermissionEvaluator;

@Configuration
public class SecurityConfig {

    @Configuration
    @EnableResourceServer
    protected static class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

        private static final String V1_API = "/v1/**";

        @Inject
        private ScimAccountGroupReaderFilter scimAccountGroupReaderFilter;

//        @Override
//        public void configure(ResourceServerSecurityConfigurer resources) {
//            resources.resourceId("cloudbreak");
//            resources.tokenServices(resourceServerTokenServices);
//            resources.tokenExtractor(new CrnTokenExtractor());
//        }

        @Override
        public void configure(HttpSecurity http) throws Exception {
            http.addFilterAfter(scimAccountGroupReaderFilter, AbstractPreAuthenticatedProcessingFilter.class)
                    .authorizeRequests()
                    .antMatchers(V1_API).permitAll()
//                    .access("#oauth2.isOAuth()")
//                    .antMatchers(AUTOSCALE_API)
//                    .access("#oauth2.hasScope('cloudbreak.autoscale')")

                    .antMatchers(API_ROOT_CONTEXT + "/swagger.json").permitAll()
                    .antMatchers(API_ROOT_CONTEXT + "/api-docs/**").permitAll();
//                    .antMatchers(API_ROOT_CONTEXT + "/**").denyAll();

            http.csrf().disable();

            http.headers().contentTypeOptions();
        }
    }

}
