package com.sequenceiq.cloudbreak.service.upgrade.image;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.VersionComparator;

@Component
class PermittedServicesForUpgradeService {

    private static final String DEFAULT_MINIMUM_VERSION = "7.2.9";

    @Value("${cb.upgrade.permittedServicesForUpgrade}")
    private Set<String> permittedServicesForUpgrade;

    private Map<String, String> serviceNameToMinimumVersionMap;

    private final VersionComparator versionComparator = new VersionComparator();

    @PostConstruct
    void init() {
        serviceNameToMinimumVersionMap = permittedServicesForUpgrade.stream()
                .map(x -> x.split(":"))
                .collect(Collectors.toMap(extractServiceName(), extractServiceMinimumVersion()));
    }

    boolean isAllowedForUpgrade(String serviceName, String blueprintVersion) {
        return findMinimumVersionForService(serviceName)
                .map(serviceMinimumVersion -> versionComparator.compare(() -> serviceMinimumVersion, () -> blueprintVersion) <= 0)
                .orElse(false);
    }

    private Optional<String> findMinimumVersionForService(String serviceName) {
        return Optional.ofNullable(serviceNameToMinimumVersionMap.get(serviceName));
    }

    private Function<String[], String> extractServiceMinimumVersion() {
        return serviceNameVersionPair -> serviceNameVersionPair.length > 1 ? StringUtils.strip(serviceNameVersionPair[1]) : DEFAULT_MINIMUM_VERSION;
    }

    private Function<String[], String> extractServiceName() {
        return serviceNameVersionPair -> StringUtils.strip(serviceNameVersionPair[0]);
    }

    @Override
    public String toString() {
        return "PermittedServicesForUpgradeService{" +
                "serviceNameToMinimumVersionMap=" + serviceNameToMinimumVersionMap +
                '}';
    }
}
