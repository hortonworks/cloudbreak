package com.sequenceiq.cloudbreak.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;

public class DetailedStackStatusConverterTest {

    private static final String NON_BACKWARD_COMPATIBLE_NAME = "non bakward compatible";

    private DetailedStackStatusConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new DetailedStackStatusConverter();
    }

    @ParameterizedTest
    @EnumSource(DetailedStackStatus.class)
    public void shouldConvertToEnumByEnumName(DetailedStackStatus detailedStackStatus) {
        String name = detailedStackStatus.name();

        assertEquals(detailedStackStatus, victim.convertToEntityAttribute(name));
    }

    @Test
    public void shouldConvertUnexpectedValueToRunning() {
        assertEquals(DetailedStackStatus.AVAILABLE, victim.convertToEntityAttribute(NON_BACKWARD_COMPATIBLE_NAME));
    }

    @ParameterizedTest
    @EnumSource(DetailedStackStatus.class)
    public void shouldConvertEnumToEnumName(DetailedStackStatus detailedStackStatus) {
        assertEquals(detailedStackStatus.name(), victim.convertToDatabaseColumn(detailedStackStatus));
    }
}