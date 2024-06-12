package com.sequenceiq.cloudbreak.common.database;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import com.sequenceiq.cloudbreak.common.type.Versioned;

/**
    Represents the major version of a postgreSQL instance.
 */
public enum MajorVersion implements Version, Versioned {

    // VERSIONS_9 is not a concrete PostgreSQL version, but the collection of 9.0 to 9.6 versions.
    VERSION_FAMILY_9("9"),
    VERSION_9_6("9.6"),
    VERSION_10("10"),
    VERSION_11("11"),
    VERSION_12("12"),
    VERSION_13("13"),
    VERSION_14("14"),
    VERSION_15("15"),
    VERSION_16("16"),
    VERSION_17("17");

    private final String version;

    private final int majorVersionFamily;

    MajorVersion(String version) {
        this.version = version;
        this.majorVersionFamily = Integer.parseInt(version.split("\\.")[0]);
    }

    @Override
    public String getMajorVersion() {
        return version;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public int getMajorVersionFamily() {
        return majorVersionFamily;
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
