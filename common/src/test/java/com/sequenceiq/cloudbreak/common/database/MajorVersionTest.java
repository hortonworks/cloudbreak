package com.sequenceiq.cloudbreak.common.database;

import static com.sequenceiq.cloudbreak.common.database.MajorVersion.VERSION_10;
import static com.sequenceiq.cloudbreak.common.database.MajorVersion.VERSION_11;
import static com.sequenceiq.cloudbreak.common.database.MajorVersion.VERSION_13;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class MajorVersionTest {

    @ParameterizedTest
    @MethodSource("majorVersionDataProvider")
    void testGetMajorVersion(String fullVersion, MajorVersion expectedMajorVersion) {
        Optional<MajorVersion> majorVersion = MajorVersion.get(fullVersion);
        Optional<MajorVersion> expected = Optional.ofNullable(expectedMajorVersion);
        assertEquals(expected, majorVersion);
    }

    static Object[][] majorVersionDataProvider() {
        return new Object[][]{
                // fullVersion expectedMajorVersion
                {"10", VERSION_10},
                {"10.6", VERSION_10},
                {"10.14", VERSION_10},
                {"11", VERSION_11},
                {"13", VERSION_13},
                {"13.2", VERSION_13},
                {"13.2.4.2.1", VERSION_13},
                {"13.234567", VERSION_13},
                {"13.asdf", VERSION_13},
                {"13.0", VERSION_13},
                {"130", null},
                {"asd", null},
        };
    }
}