package com.sequenceiq.cloudbreak.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class TargetMajorVersionTest {

    @ParameterizedTest
    @EnumSource(value = TargetMajorVersion.class)
    void testConvertToMajorVersion(TargetMajorVersion targetMajorVersion) {
        assertEquals(targetMajorVersion.getMajorVersion(), targetMajorVersion.convertToMajorVersion().getMajorVersion());
    }

}
