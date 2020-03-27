package com.sequenceiq.cloudbreak.cloud;

import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class CustomVersionComparator {

    public static final String SPLIT_PATTERN = "[.\\-]";

    public int compare(String version1, String version2, CompareLevel compareLevel) {
        validateVersion(version1, version2, compareLevel);
        VersionComparator versionComparator = new VersionComparator();
        int result = 0;
        switch (compareLevel) {
            case FULL:
                result = versionComparator.compare(() -> version1, () -> version2);
                break;
            case MINOR:
                validateMajorVersion(version1, version2);
                String ver1 = getVersionWithoutMajorVersion(version1);
                String ver2 = getVersionWithoutMajorVersion(version2);
                result = versionComparator.compare(() -> ver1, () -> ver2);
                break;
            case MAINTENANCE:
                result = compareVersions(version1, version2);
                break;
            default:
                throw new IllegalArgumentException("Invalid compare level!");
        }
        return result;
    }

    private void validateMajorVersion(String version1, String version2) {
        if (notEquals(version1.split(SPLIT_PATTERN)[0], version2.split(SPLIT_PATTERN)[0])) {
            throw new IllegalArgumentException("The major versions cannot be different in case of minor level compare.");
        }
    }

    private String getVersionWithoutMajorVersion(String version) {
        return version.substring(version.indexOf(".") + 1);
    }

    private void validateVersion(String version1, String version2, CompareLevel compareLevel) {
        Assert.notNull(version1, "The version1 must not be null!");
        Assert.notNull(version2, "The version2 must not be null!");
        Assert.notNull(compareLevel, "The compareLevel must not be null!");
    }

    private int compareVersions(String version1, String version2) {
        String[] v1Parts = version1.split(SPLIT_PATTERN);
        String[] v2Parts = version2.split(SPLIT_PATTERN);
        validateParts(v1Parts, v2Parts);
        return isMaintenanceVersionGreater(v1Parts[2], v2Parts[2]);
    }

    private void validateParts(String[] v1Parts, String[] v2Parts) {
        if (notEquals(v1Parts[0], v2Parts[0]) || notEquals(v1Parts[1], v2Parts[1])) {
            throw new IllegalArgumentException("The major or the minor versions cannot be different in case of maintenance level compare.");
        }
    }

    private boolean notEquals(String v1, String v2) {
        return Integer.parseInt(v1) != Integer.parseInt(v2);
    }

    private int isMaintenanceVersionGreater(String v1, String v2) {
        return Integer.compare(Integer.parseInt(v1), Integer.parseInt(v2));
    }
}
