package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.freeipa.FreeIpaStackDetails;

@ExtendWith(MockitoExtension.class)
class StackDetailsToCDPFreeIPAExtendedDetailsConverterTest {

    private StackDetailsToCDPFreeIPAExtendedDetailsConverter underTest;

    @BeforeEach
    void setUp() {
        underTest = new StackDetailsToCDPFreeIPAExtendedDetailsConverter();
        ReflectionTestUtils.setField(underTest, "freeIPAShapeConverter", new StackDetailsToCDPFreeIPAShapeConverter());
        ReflectionTestUtils.setField(underTest, "imageDetailsConverter", new StackDetailsToCDPImageDetailsConverter());
    }

    @Test
    void testConvertWithNull() {
        UsageProto.CDPFreeIPAExtendedDetails freeIPAExtendedDetails = underTest.convert(null);

        assertNotNull(freeIPAExtendedDetails.getFreeIPAShape());
        assertNotNull(freeIPAExtendedDetails.getImageDetails());
    }

    @Test
    void testConvertWithEmpty() {
        UsageProto.CDPFreeIPAExtendedDetails freeIPAExtendedDetails = underTest.convert(new FreeIpaStackDetails());

        assertNotNull(freeIPAExtendedDetails.getFreeIPAShape());
        assertNotNull(freeIPAExtendedDetails.getImageDetails());
    }

    @Test
    void testConvertTrustRelationshipTypeWithNullStackDetails() {
        UsageProto.CDPFreeIPAExtendedDetails freeIPAExtendedDetails = underTest.convert(null);

        assertEquals(UsageProto.CDPFreeIPATrustRelationshipType.Value.UNSET, freeIPAExtendedDetails.getTrustRelationshipType());
    }

    @Test
    void testConvertTrustRelationshipTypeWithoutTrust() {
        UsageProto.CDPFreeIPAExtendedDetails freeIPAExtendedDetails = underTest.convert(new FreeIpaStackDetails());

        assertEquals(UsageProto.CDPFreeIPATrustRelationshipType.Value.UNSET, freeIPAExtendedDetails.getTrustRelationshipType());
    }

    @ParameterizedTest
    @CsvSource({
            "UNKNOWN, UNKNOWN",
            "ONE_WAY, ONE_WAY",
            "TWO_WAY, TWO_WAY",
            "SOMETHING_ELSE, UNKNOWN"
    })
    void testConvertTrustRelationshipType(String trustRelationshipType, UsageProto.CDPFreeIPATrustRelationshipType.Value expected) {
        FreeIpaStackDetails stackDetails = new FreeIpaStackDetails();
        stackDetails.setTrustRelationshipType(trustRelationshipType);

        UsageProto.CDPFreeIPAExtendedDetails freeIPAExtendedDetails = underTest.convert(stackDetails);

        assertEquals(expected, freeIPAExtendedDetails.getTrustRelationshipType());
    }
}
