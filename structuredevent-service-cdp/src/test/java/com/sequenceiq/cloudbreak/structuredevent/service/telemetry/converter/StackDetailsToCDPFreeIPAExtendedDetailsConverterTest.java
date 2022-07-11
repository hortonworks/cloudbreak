package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

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
        UsageProto.CDPFreeIPAExtendedDetails freeIPAExtendedDetails = underTest.convert(new StackDetails());

        assertNotNull(freeIPAExtendedDetails.getFreeIPAShape());
        assertNotNull(freeIPAExtendedDetails.getImageDetails());
    }
}
