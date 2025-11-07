package com.sequenceiq.notification.sender.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.notificationadmin.NotificationAdminProto.ChannelType;

public class ChannelTypeConverterTest {

    private final ChannelTypeConverter underTest = new ChannelTypeConverter();

    @Test
    @DisplayName("convert domain null -> proto UNKNOWN")
    void testDomainNullToProto() {
        assertEquals(ChannelType.Value.UNKNOWN, underTest.convert((com.sequenceiq.notification.domain.ChannelType) null));
    }

    @Test
    @DisplayName("convert domain enumerations -> proto values (CONSOLE maps to UNKNOWN)")
    void testDomainToProtoMappings() {
        assertEquals(ChannelType.Value.EMAIL, underTest.convert(com.sequenceiq.notification.domain.ChannelType.EMAIL));
        assertEquals(ChannelType.Value.IN_APP, underTest.convert(com.sequenceiq.notification.domain.ChannelType.IN_APP));
        assertEquals(ChannelType.Value.SLACK, underTest.convert(com.sequenceiq.notification.domain.ChannelType.SLACK));
    }

    @Test
    @DisplayName("convert proto null -> domain null")
    void testProtoNullToDomain() {
        assertNull(underTest.convert((ChannelType.Value) null));
    }

    @Test
    @DisplayName("convert proto enumerations -> domain values (UNKNOWN/UNRECOGNIZED -> null)")
    void testProtoToDomainMappings() {
        assertEquals(com.sequenceiq.notification.domain.ChannelType.EMAIL, underTest.convert(ChannelType.Value.EMAIL));
        assertEquals(com.sequenceiq.notification.domain.ChannelType.IN_APP, underTest.convert(ChannelType.Value.IN_APP));
        assertEquals(com.sequenceiq.notification.domain.ChannelType.SLACK, underTest.convert(ChannelType.Value.SLACK));
        assertNull(underTest.convert(ChannelType.Value.UNKNOWN));
        assertNull(underTest.convert(ChannelType.Value.UNRECOGNIZED));
    }

    @Test
    @DisplayName("all domain ChannelType values have proto mappings")
    void allDomainValuesHaveProtoMappings() {
        for (com.sequenceiq.notification.domain.ChannelType domainType : com.sequenceiq.notification.domain.ChannelType.values()) {
            ChannelType.Value protoValue = underTest.convert(domainType);
            assertNotNull(protoValue, "Domain ChannelType." + domainType + " should have a proto mapping");
            assertNotEquals(ChannelType.Value.UNKNOWN, protoValue,
                    "Domain ChannelType." + domainType + " should not map to UNKNOWN");
        }
    }

    @Test
    @DisplayName("all proto ChannelType values have domain mappings except UNKNOWN and UNRECOGNIZED")
    void allProtoValuesHaveDomainMappings() {
        for (ChannelType.Value protoValue : ChannelType.Value.values()) {
            if (protoValue == ChannelType.Value.UNKNOWN || protoValue == ChannelType.Value.UNRECOGNIZED) {
                continue;
            }
            com.sequenceiq.notification.domain.ChannelType domainType = underTest.convert(protoValue);
            assertNotNull(domainType, "Proto ChannelType.Value." + protoValue + " should have a domain mapping");
        }
    }
}
