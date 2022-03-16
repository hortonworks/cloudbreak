package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
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

        Assertions.assertEquals("", freeIPAStatusDetails.getStackStatus());
        Assertions.assertEquals("", freeIPAStatusDetails.getStackDetailedStatus());
        Assertions.assertEquals("", freeIPAStatusDetails.getStackStatusReason());
    }

    @Test
    public void testConvertWithEmpty() {
        UsageProto.CDPFreeIPAStatusDetails freeIPAStatusDetails = underTest.convert(new StackDetails());

        Assertions.assertEquals("", freeIPAStatusDetails.getStackStatus());
        Assertions.assertEquals("", freeIPAStatusDetails.getStackDetailedStatus());
        Assertions.assertEquals("", freeIPAStatusDetails.getStackStatusReason());
    }

    @Test
    public void testConversionWithValues() {
        StackDetails stackDetails = new StackDetails();
        stackDetails.setStatus("AVAILABLE");
        stackDetails.setDetailedStatus("AVAILABLE");
        stackDetails.setStatusReason("statusreason");

        UsageProto.CDPFreeIPAStatusDetails freeIPAStatusDetails = underTest.convert(stackDetails);

        Assertions.assertEquals("AVAILABLE", freeIPAStatusDetails.getStackStatus());
        Assertions.assertEquals("AVAILABLE", freeIPAStatusDetails.getStackDetailedStatus());
        Assertions.assertEquals("statusreason", freeIPAStatusDetails.getStackStatusReason());
    }

    @Test
    public void testStatusReasonStringTrimming() {
        StackDetails stackDetails = new StackDetails();

        UsageProto.CDPFreeIPAStatusDetails freeIPAStatusDetails = underTest.convert(stackDetails);

        Assertions.assertEquals("", freeIPAStatusDetails.getStackStatusReason());

        stackDetails.setStatusReason("");
        freeIPAStatusDetails = underTest.convert(stackDetails);

        Assertions.assertEquals("", freeIPAStatusDetails.getStackStatusReason());

        stackDetails.setStatusReason(StringUtils.repeat("*", 10));
        freeIPAStatusDetails = underTest.convert(stackDetails);

        Assertions.assertEquals(StringUtils.repeat("*", 10), freeIPAStatusDetails.getStackStatusReason());

        stackDetails.setStatusReason(StringUtils.repeat("*", 1500));
        freeIPAStatusDetails = underTest.convert(stackDetails);

        Assertions.assertEquals(StringUtils.repeat("*", 1500), freeIPAStatusDetails.getStackStatusReason());

        stackDetails.setStatusReason(StringUtils.repeat("*", 3000));
        freeIPAStatusDetails = underTest.convert(stackDetails);

        Assertions.assertEquals(StringUtils.repeat("*", 1500), freeIPAStatusDetails.getStackStatusReason());

    }
}
