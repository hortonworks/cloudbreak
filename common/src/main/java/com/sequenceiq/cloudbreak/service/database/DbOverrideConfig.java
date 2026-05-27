package com.sequenceiq.cloudbreak.service.database;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;


import com.sequenceiq.cloudbreak.util.VersionComparator;

@Configuration
@ConfigurationProperties(prefix = "cb.db.override")
public class DbOverrideConfig {

    private List<DbOverrideVersion> versions;

    private List<DbOverrideVersion> deprecatedVersions = new ArrayList<>();

    private Map<String, LocalDate> eolDates = new HashMap<>();

    private final VersionComparator versionComparator = new VersionComparator();

    public List<DbOverrideVersion> getVersions() {
        return versions;
    }

    public void setVersions(List<DbOverrideVersion> versions) {
        this.versions = versions;
    }

    public String findEngineVersionForRuntime(String runtimeVersion) {
        return versions.stream()
                .filter(v -> versionComparator.compare(() -> runtimeVersion, v::getMinRuntimeVersion) >= 0)
                .max((v1, v2) -> versionComparator.compare(v1::getMinRuntimeVersion, v2::getMinRuntimeVersion))
                .map(DbOverrideVersion::getEngineVersion)
                .orElse(null);
    }

    public String findMinEngineVersion() {
        return versions.stream()
                .map(DbOverrideVersion::getEngineVersion)
                .min(Comparator.comparingInt(Integer::parseInt))
                .orElse(null);
    }

    public Optional<String> findMinRuntimeVersion(String targetMajorVersion) {
        return versions.stream().filter(dbOverrideVersion -> dbOverrideVersion.getEngineVersion().equals(targetMajorVersion))
                .findFirst()
                .map(DbOverrideVersion::getMinRuntimeVersion);
    }

    public List<DbOverrideVersion> getDeprecatedVersions() {
        return deprecatedVersions;
    }

    public void setDeprecatedVersions(List<DbOverrideVersion> deprecatedVersions) {
        this.deprecatedVersions = deprecatedVersions;
    }

    public Map<String, LocalDate> getEolDates() {
        return eolDates;
    }

    public void setEolDates(Map<String, LocalDate> eolDates) {
        this.eolDates = eolDates;
    }

    public boolean isVersionSupportedForRuntime(String engineVersion, String runtimeVersion) {
        if (runtimeVersion == null) {
            return true;
        }
        return deprecatedVersions.stream()
                .filter(v -> v.getEngineVersion().equals(engineVersion))
                .noneMatch(v -> versionComparator.compare(() -> runtimeVersion, v::getMinRuntimeVersion) >= 0);
    }

    public Optional<LocalDate> getEolDate(String engineVersion) {
        return Optional.ofNullable(eolDates.get(engineVersion));
    }

    public boolean isVersionEol(String engineVersion, LocalDate today) {
        return getEolDate(engineVersion)
                .map(eol -> !today.isBefore(eol))
                .orElse(false);
    }
}