package com.sequenceiq.cloudbreak.database;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import com.sequenceiq.cloudbreak.registry.ServiceAddressResolver;
import com.sequenceiq.cloudbreak.registry.ServiceAddressResolvingException;

@Configuration
public class DatabaseAddressConfig {

    @Inject
    private DatabaseProperties databaseProperties;

    @Inject
    private ServiceAddressResolver serviceAddressResolver;

    @Bean
    @DependsOn("serviceAddressResolver")
    public String databaseAddress() throws ServiceAddressResolvingException {
        return serviceAddressResolver.resolveHostPort(databaseProperties.getDatabaseHost(), databaseProperties.getDatabasePort(),
                databaseProperties.getDatabaseId());
    }
}
