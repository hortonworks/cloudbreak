package com.sequenceiq.cloudbreak.domain;

import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class InstanceStatusConverterTest {

    private static final String NON_BACKWARD_COMPATIBLE_NAME = "non bakward compatible";

    private InstanceStatusConverter victim;

    @BeforeEach
    public void initTest() {
        victim = new InstanceStatusConverter();
    }

    @ParameterizedTest
    @EnumSource(InstanceStatus.class)
    public void shouldConvertToEnumByEnumName(InstanceStatus instanceStatus) {
        String name = instanceStatus.name();

        assertEquals(instanceStatus, victim.convertToEntityAttribute(name));
    }

    @Test
    public void shouldConvertUnexpectedValueToStarted() {
        assertEquals(InstanceStatus.STARTED, victim.convertToEntityAttribute(NON_BACKWARD_COMPATIBLE_NAME));
    }

    @Test
    public void shouldConvertNullToNull() {
        assertNull(victim.convertToDatabaseColumn(null));
        assertNull(victim.convertToEntityAttribute(null));
    }

    @ParameterizedTest
    @EnumSource(InstanceStatus.class)
    public void shouldConvertEnumToEnumName(InstanceStatus detailedDBStackStatus) {
        assertEquals(detailedDBStackStatus.name(), victim.convertToDatabaseColumn(detailedDBStackStatus));
    }
}