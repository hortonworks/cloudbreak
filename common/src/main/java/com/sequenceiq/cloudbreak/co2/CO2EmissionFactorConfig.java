package com.sequenceiq.cloudbreak.co2;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Configuration
public class CO2EmissionFactorConfig {

    @Inject
    private Optional<List<CO2EmissionFactorService>> emissionFactorServices;

    @Bean
    public Map<CloudPlatform, CO2EmissionFactorService> co2EmissionFactorServiceMap() {
        return emissionFactorServices.orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(CO2EmissionFactorService::getCloudPlatform, Function.identity()));
    }
}
