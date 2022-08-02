package com.sequenceiq.cloudbreak.common.database;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public enum MajorVersion {

    VERSION_10("10"),
    VERSION_11("11"),
    VERSION_12("12"),
    VERSION_13("13"),
    VERSION_14("14");

    private final String version;

    MajorVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public static Optional<MajorVersion> get(String version) {
        return Arrays.stream(values())
                .filter(ver -> exactMatch(version, ver) || nonExactMatch(version, ver))
                .findFirst();
    }

    private static boolean exactMatch(String version, MajorVersion ver) {
        return ver.version.equals(version);
    }

    private static boolean nonExactMatch(String version, MajorVersion ver) {
        return Objects.nonNull(version) && version.startsWith(ver.version + ".");
    }

}
