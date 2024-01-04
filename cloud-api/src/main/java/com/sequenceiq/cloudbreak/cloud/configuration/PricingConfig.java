package com.sequenceiq.cloudbreak.cloud.configuration;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.cloud.PricingCache;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;

@Configuration
public class PricingConfig {

    @Inject
    private Optional<List<PricingCache>> pricingCaches;

    @Bean
    public Map<CloudPlatform, PricingCache> pricingCacheMap() {
        return pricingCaches.orElse(Collections.emptyList())
                .stream()
                .collect(Collectors.toMap(PricingCache::getCloudPlatform, Function.identity()));
    }
}
