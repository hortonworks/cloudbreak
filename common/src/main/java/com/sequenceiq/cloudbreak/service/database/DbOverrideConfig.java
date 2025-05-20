package com.sequenceiq.cloudbreak.service.database;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import com.sequenceiq.cloudbreak.util.VersionComparator;

@Configuration
@ConfigurationProperties(prefix = "cb.db.override")
public class DbOverrideConfig {
    private List<DbOverrideVersion> versions;

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
}