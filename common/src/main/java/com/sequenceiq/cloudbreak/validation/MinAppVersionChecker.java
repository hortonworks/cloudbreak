package com.sequenceiq.cloudbreak.validation;

public class MinAppVersionChecker {

    private final Integer minMajorVersion;

    private final Integer minMinorVersion;

    public MinAppVersionChecker(Integer minMajorVersion, Integer minMinorVersion) {
        this.minMajorVersion = minMajorVersion;
        this.minMinorVersion = minMinorVersion;
    }

    public Integer getMinMajorVersion() {
        return minMajorVersion;
    }

    public Integer getMinMinorVersion() {
        return minMinorVersion;
    }

    public boolean isAppVersionValid(String appVersion) {
        boolean result = true;
        if (appVersion == null) {
            return result;
        }
        String withoutBuildNumber = appVersion.split("-")[0];
        String[] versionParts = withoutBuildNumber.split("\\.");
        if (versionParts.length > 1) {
            int majorVersion = Integer.parseInt(versionParts[0]);
            int minorVersion = Integer.parseInt(versionParts[1]);
            if (majorVersion < minMajorVersion) {
                result = false;
            } else if (majorVersion == minMajorVersion && minorVersion < minMinorVersion) {
                result = false;
            }
        }
        return result;
    }
}
