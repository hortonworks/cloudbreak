package com.sequenceiq.cloudbreak.conf;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

@Configuration
@EnableResourceServer
public class SecurityConfig extends ResourceServerConfigurerAdapter {

    @Autowired
    private UserDetailsService userDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Bean
    RemoteTokenServices remoteTokenServices() {
        RemoteTokenServices rts = new RemoteTokenServices();
        rts.setClientId("cloudbreak");
        rts.setClientSecret("cloudbreaksecret");
        rts.setCheckTokenEndpointUrl("http://172.20.0.21:8080/check_token");
        return rts;
    }

    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.resourceId("cloudbreak");
        resources.tokenServices(remoteTokenServices());
    }

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .headers()
                .contentTypeOptions()
                .and()
                .authorizeRequests()
                .antMatchers("/templates").access("#oauth2.hasScope('cloudbreak.read')")
                .antMatchers("/notification/**").permitAll()
                .antMatchers("/sns/**").permitAll()
                .antMatchers(HttpMethod.POST, "/users/**").permitAll()
                .antMatchers(HttpMethod.GET, "/users/confirm/**").permitAll()
                .antMatchers(HttpMethod.POST, "/password/reset/**").permitAll()
                .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .antMatchers(HttpMethod.GET, "/stacks/metadata/**").permitAll();
    }
}