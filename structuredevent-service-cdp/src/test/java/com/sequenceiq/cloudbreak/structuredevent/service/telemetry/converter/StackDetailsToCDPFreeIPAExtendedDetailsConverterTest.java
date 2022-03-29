package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.powermock.reflect.Whitebox;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@ExtendWith(MockitoExtension.class)
public class StackDetailsToCDPFreeIPAExtendedDetailsConverterTest {

    private StackDetailsToCDPFreeIPAExtendedDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StackDetailsToCDPFreeIPAExtendedDetailsConverter();
        Whitebox.setInternalState(underTest, "freeIPAShapeConverter", new StackDetailsToCDPFreeIPAShapeConverter());
        Whitebox.setInternalState(underTest, "imageDetailsConverter", new StackDetailsToCDPImageDetailsConverter());
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPFreeIPAExtendedDetails freeIPAExtendedDetails = underTest.convert(null);

        Assertions.assertNotNull(freeIPAExtendedDetails.getFreeIPAShape());
        Assertions.assertNotNull(freeIPAExtendedDetails.getImageDetails());
    }

    @Test
    public void testConvertWithEmpty() {
        UsageProto.CDPFreeIPAExtendedDetails freeIPAExtendedDetails = underTest.convert(new StackDetails());

        Assertions.assertNotNull(freeIPAExtendedDetails.getFreeIPAShape());
        Assertions.assertNotNull(freeIPAExtendedDetails.getImageDetails());
    }
}
