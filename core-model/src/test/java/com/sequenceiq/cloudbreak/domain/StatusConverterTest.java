package com.sequenceiq.cloudbreak.domain;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StatusConverterTest {

    private static final String NON_BACKWARD_COMPATIBLE_NAME = "non bakward compatible";

    private StatusConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new StatusConverter();
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    public void shouldConvertToEnumByEnumName(Status status) {
        String name = status.name();

        assertEquals(status, victim.convertToEntityAttribute(name));
    }

    @Test
    public void shouldConvertUnexpectedValueToRunning() {
        assertEquals(Status.AVAILABLE, victim.convertToEntityAttribute(NON_BACKWARD_COMPATIBLE_NAME));
    }

    @ParameterizedTest
    @EnumSource(Status.class)
    public void shouldConvertEnumToEnumName(Status status) {
        assertEquals(status.name(), victim.convertToDatabaseColumn(status));
    }
}