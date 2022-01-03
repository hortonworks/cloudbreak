package com.sequenceiq.cloudbreak.conf;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.service.upgrade.image.filter.UpgradeImageFilter;

@Configuration
public class UpgradeImageFilterConfig {

    @Inject
    private Set<UpgradeImageFilter> upgradeImageFilters;

    @Bean
    public List<UpgradeImageFilter> orderedUpgradeImageFilters() {
        return upgradeImageFilters.stream()
                .sorted(Comparator.comparingInt(UpgradeImageFilter::getFilterOrderNumber))
                .collect(Collectors.toList());
    }
}
