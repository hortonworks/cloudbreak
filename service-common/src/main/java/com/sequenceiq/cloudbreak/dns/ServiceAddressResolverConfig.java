package com.sequenceiq.cloudbreak.dns;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.registry.DNSServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.RetryingServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;

@Configuration
public class ServiceAddressResolverConfig {

    @Value("${service.address.resolving.timeout:60000}")
    private int resolvingTimeout;

    @Bean
    public ServiceAddressResolver serviceAddressResolver() {
        return new RetryingServiceAddressResolver(new DNSServiceAddressResolver(), resolvingTimeout);
    }
}
