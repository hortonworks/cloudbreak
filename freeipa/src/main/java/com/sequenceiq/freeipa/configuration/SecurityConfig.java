package com.sequenceiq.freeipa.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;

import com.sequenceiq.cloudbreak.client.CaasClient;
import com.sequenceiq.cloudbreak.client.ConfigKey;

@Configuration
@EnableResourceServer
public class SecurityConfig  extends ResourceServerConfigurerAdapter {
    @Value("${caas.url:}")
    private String caasUrl;

    @Value("${caas.protocol:http}")
    private String caasProtocol;

    @Value("${caas.cert.validation:false}")
    private boolean caasCertificateValidation;

    @Value("${caas.cert.ignorePreValidation:false}")
    private boolean caasIgnorePreValidation;

    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests().antMatchers("/**").permitAll();
        http.csrf().disable();
        http.headers().contentTypeOptions();
    }

    @Bean
    public CaasClient caasClient() {
        return new CaasClient(caasProtocol, caasUrl, new ConfigKey(caasCertificateValidation, false, caasIgnorePreValidation));
    }
}
