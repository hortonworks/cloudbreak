package com.sequenceiq.cloudbreak.common.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationStateConverterTest {

    private NotificationStateConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new NotificationStateConverter();
    }

    @Test
    void testConvertToEntityAttribute() {
        assertEquals(NotificationState.ENABLED, underTest.convertToEntityAttribute("ENABLED"));
        assertEquals(NotificationState.DISABLED, underTest.convertToEntityAttribute("DISABLED"));
        assertEquals(NotificationState.DISABLED, underTest.convertToEntityAttribute("invalid"));
        assertEquals(NotificationState.DISABLED, underTest.convertToEntityAttribute(null));
    }

    @Test
    void testConvertToDatabaseColumn() {
        assertEquals("ENABLED", underTest.convertToDatabaseColumn(NotificationState.ENABLED));
        assertEquals("DISABLED", underTest.convertToDatabaseColumn(NotificationState.DISABLED));
    }

    @Test
    void testGetDefault() {
        assertEquals(NotificationState.DISABLED, underTest.getDefault());
    }
}
