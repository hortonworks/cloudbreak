package com.sequenceiq.cloudbreak.cmtemplate.generator.support;

import static org.apache.commons.lang3.StringUtils.trimToNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.api.service.ExposedService;
import com.sequenceiq.cloudbreak.api.service.ExposedServiceCollector;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedService;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedServices;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedVersion;
import com.sequenceiq.cloudbreak.cmtemplate.generator.support.domain.SupportedVersions;

@Service
public class SupportedVersionService {

    @Inject
    private ExposedServiceCollector exposedServiceCollector;

    public SupportedVersions collectSupportedVersions(Set<String> versions) {
        Set<SupportedVersion> supportedVersionsSet = new HashSet<>();
        versions.forEach(version -> {
            Set<SupportedService> services = exposedServiceCollector.filterSupportedKnoxServices(Optional.of(version))
                    .stream()
                    .map(e -> supportedService(e))
                    .collect(Collectors.toSet());
            supportedVersionsSet.add(supportedVersion(version, services));
        });

        SupportedVersions supportedVersions = new SupportedVersions();
        supportedVersions.setSupportedVersions(supportedVersionsSet);
        return supportedVersions;
    }

    private SupportedService supportedService(ExposedService exposedService) {
        SupportedService supportedService = new SupportedService();
        supportedService.setName(exposedService.getName());
        supportedService.setDisplayName(
                Optional.ofNullable(trimToNull(exposedService.getDisplayName())).orElse(exposedService.getDisplayName()));
        supportedService.setVersion("N/A");
        supportedService.setIconKey(Optional.ofNullable(trimToNull(exposedService.getIconKey())).orElse(exposedService.getName()));
        return supportedService;
    }

    private SupportedVersion supportedVersion(String version, Set<SupportedService> services) {
        SupportedVersion supportedVersion = new SupportedVersion();
        supportedVersion.setType("CDH");
        supportedVersion.setVersion(version);
        SupportedServices supportedServices = new SupportedServices();
        supportedServices.setServices(services);
        supportedVersion.setSupportedServices(supportedServices);
        return supportedVersion;
    }

}
