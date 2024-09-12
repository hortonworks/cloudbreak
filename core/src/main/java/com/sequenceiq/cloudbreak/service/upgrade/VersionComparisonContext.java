package com.sequenceiq.cloudbreak.service.upgrade;

import java.util.Optional;

public class VersionComparisonContext {

    private final String majorVersion;

    private final Integer patchVersion;

    private final Integer buildNumber;

    private VersionComparisonContext(String majorVersion, Integer patchVersion, Integer buildNumber) {
        this.majorVersion = majorVersion;
        this.patchVersion = patchVersion;
        this.buildNumber = buildNumber;
    }

    public String getMajorVersion() {
        return majorVersion;
    }

    public Optional<Integer> getPatchVersion() {
        return Optional.ofNullable(patchVersion);
    }

    public Integer getBuildNumber() {
        return buildNumber;
    }

    public static class Builder {

        private String majorVersion;

        private Integer patchVersion;

        private Integer buildNumber;

        public Builder withMajorVersion(String majorVersion) {
            this.majorVersion = majorVersion;
            return this;
        }

        public Builder withPatchVersion(Integer patchVersion) {
            this.patchVersion = patchVersion;
            return this;
        }

        public Builder withBuildNumber(Integer buildNumber) {
            this.buildNumber = buildNumber;
            return this;
        }

        public VersionComparisonContext build() {
            return new VersionComparisonContext(majorVersion, patchVersion, buildNumber);
        }
    }
}
