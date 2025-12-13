package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;

@ExtendWith(MockitoExtension.class)
public class StackDetailsToCDPFreeIPAStatusDetailsConverterTest {

    private StackDetailsToCDPFreeIPAStatusDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StackDetailsToCDPFreeIPAStatusDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        UsageProto.CDPFreeIPAStatusDetails freeIPAStatusDetails = underTest.convert(null);

        assertEquals("", freeIPAStatusDetails.getStackStatus());
        assertEquals("", freeIPAStatusDetails.getStackDetailedStatus());
        assertEquals("", freeIPAStatusDetails.getStackStatusReason());
    }

    @Test
    public void testConvertWithEmpty() {
        UsageProto.CDPFreeIPAStatusDetails freeIPAStatusDetails = underTest.convert(new StackDetails());

        assertEquals("", freeIPAStatusDetails.getStackStatus());
        assertEquals("", freeIPAStatusDetails.getStackDetailedStatus());
        assertEquals("", freeIPAStatusDetails.getStackStatusReason());
    }

    @Test
    public void testConversionWithValues() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setStatus("AVAILABLE");
        stackDetails.setDetailedStatus("AVAILABLE");
        stackDetails.setStatusReason("statusreason");

        UsageProto.CDPFreeIPAStatusDetails freeIPAStatusDetails = underTest.convert(stackDetails);

        assertEquals("AVAILABLE", freeIPAStatusDetails.getStackStatus());
        assertEquals("AVAILABLE", freeIPAStatusDetails.getStackDetailedStatus());
        assertEquals("statusreason", freeIPAStatusDetails.getStackStatusReason());
    }

    @Test
    public void testStatusReasonStringTrimming() {
        StackDetails stackDetails = new StackDetails();

        UsageProto.CDPFreeIPAStatusDetails freeIPAStatusDetails = underTest.convert(stackDetails);

        assertEquals("", freeIPAStatusDetails.getStackStatusReason());

        stackDetails.setStatusReason("");
        freeIPAStatusDetails = underTest.convert(stackDetails);

        assertEquals("", freeIPAStatusDetails.getStackStatusReason());

        stackDetails.setStatusReason(StringUtils.repeat("*", 10));
        freeIPAStatusDetails = underTest.convert(stackDetails);

        assertEquals(StringUtils.repeat("*", 10), freeIPAStatusDetails.getStackStatusReason());

        stackDetails.setStatusReason(StringUtils.repeat("*", 1500));
        freeIPAStatusDetails = underTest.convert(stackDetails);

        assertEquals(StringUtils.repeat("*", 1500), freeIPAStatusDetails.getStackStatusReason());

        stackDetails.setStatusReason(StringUtils.repeat("*", 3000));
        freeIPAStatusDetails = underTest.convert(stackDetails);

        assertEquals(StringUtils.repeat("*", 1500), freeIPAStatusDetails.getStackStatusReason());

    }
}
