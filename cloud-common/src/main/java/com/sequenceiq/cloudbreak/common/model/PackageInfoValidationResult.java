package com.sequenceiq.cloudbreak.common.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class PackageInfoValidationResult {

    private final Map<String, List<PackageInfo>> cmPackageVersionsFromAllHostsFiltered;

    private final Set<String> hostsWithInvalidResponse;

    public PackageInfoValidationResult(Map<String, List<PackageInfo>> cmPackageVersionsFromAllHostsFiltered, Set<String> hostsWithInvalidResponse) {
        this.cmPackageVersionsFromAllHostsFiltered = cmPackageVersionsFromAllHostsFiltered;
        this.hostsWithInvalidResponse = hostsWithInvalidResponse;
    }

    public Map<String, List<PackageInfo>> getCmPackageVersionsFromAllHostsFiltered() {
        return cmPackageVersionsFromAllHostsFiltered;
    }

    public Set<String> getHostsWithInvalidResponse() {
        return hostsWithInvalidResponse;
    }

    @Override
    public String toString() {
        return "PackageInfoValidationResult{" +
                "cmPackageVersionsFromAllHostsFiltered=" + cmPackageVersionsFromAllHostsFiltered +
                ", hostsWithInvalidResponse=" + hostsWithInvalidResponse +
                '}';
    }
}
