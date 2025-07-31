package com.sequenceiq.cloudbreak.common.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

public class TargetMajorVersionTest {

    @ParameterizedTest
    @EnumSource(value = TargetMajorVersion.class)
    void testConvertToMajorVersion(TargetMajorVersion targetMajorVersion) {
        assertEquals(targetMajorVersion.getMajorVersion(), targetMajorVersion.convertToMajorVersion().getMajorVersion());
    }

    @ParameterizedTest
    @MethodSource("validVersionProvider")
    void testFromVersionWithValidValues(String version, TargetMajorVersion expectedEnum) {
        assertEquals(expectedEnum.getMajorVersion(), TargetMajorVersion.fromVersion(version).getMajorVersion());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "invalid", "12", "15", "18", "null"})
    void testFromVersionWithInvalidValues(String invalidVersion) {
        assertNull(TargetMajorVersion.fromVersion(invalidVersion));
    }

    @ParameterizedTest
    @ValueSource(strings = {""})
    void testFromVersionWithNullValue(String ignored) {
        assertNull(TargetMajorVersion.fromVersion(null));
    }

    static Stream<Arguments> validVersionProvider() {
        return Stream.of(
                Arguments.of("11", TargetMajorVersion.VERSION11),
                Arguments.of("14", TargetMajorVersion.VERSION14),
                Arguments.of("17", TargetMajorVersion.VERSION17),
                Arguments.of("11", TargetMajorVersion.VERSION_11),
                Arguments.of("14", TargetMajorVersion.VERSION_14)
        );
    }

}
