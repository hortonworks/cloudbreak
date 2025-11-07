package com.sequenceiq.notification.sender.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.SeverityType;
import com.sequenceiq.notification.domain.NotificationSeverity;

public class NotificationSeverityConverterTest {

    private final NotificationSeverityConverter underTest = new NotificationSeverityConverter();

    @Test
    @DisplayName("convert(NotificationSeverity) should return DEFAULT when input is null")
    void testConvertFromDomainNull() {
        assertEquals(SeverityType.Value.DEFAULT, underTest.convert((NotificationSeverity) null));
    }

    @Test
    @DisplayName("convert(NotificationSeverity) should map all severities correctly")
    void testConvertFromDomainAllMappings() {
        assertEquals(SeverityType.Value.DEBUG, underTest.convert(NotificationSeverity.DEBUG));
        assertEquals(SeverityType.Value.INFO, underTest.convert(NotificationSeverity.INFO));
        assertEquals(SeverityType.Value.WARNING, underTest.convert(NotificationSeverity.WARNING));
        assertEquals(SeverityType.Value.ERROR, underTest.convert(NotificationSeverity.ERROR));
        assertEquals(SeverityType.Value.CRITICAL, underTest.convert(NotificationSeverity.CRITICAL));
    }

    @Test
    @DisplayName("convert(SeverityType.Value) should return INFO when input is null")
    void testConvertFromProtoNull() {
        assertEquals(NotificationSeverity.INFO, underTest.convert((SeverityType.Value) null));
    }

    @Test
    @DisplayName("convert(SeverityType.Value) should map DEBUG, WARNING, ERROR, CRITICAL explicitly and INFO via default branch")
    void testConvertFromProtoExplicitAndDefaultMappings() {
        assertEquals(NotificationSeverity.DEBUG, underTest.convert(SeverityType.Value.DEBUG));
        assertEquals(NotificationSeverity.WARNING, underTest.convert(SeverityType.Value.WARNING));
        assertEquals(NotificationSeverity.ERROR, underTest.convert(SeverityType.Value.ERROR));
        assertEquals(NotificationSeverity.CRITICAL, underTest.convert(SeverityType.Value.CRITICAL));
        assertEquals(NotificationSeverity.INFO, underTest.convert(SeverityType.Value.INFO));
    }

    @Test
    @DisplayName("convert(SeverityType.Value) should map DEFAULT value to INFO via default branch")
    void testConvertFromProtoDefaultValue() {
        assertEquals(NotificationSeverity.INFO, underTest.convert(SeverityType.Value.DEFAULT));
    }
}