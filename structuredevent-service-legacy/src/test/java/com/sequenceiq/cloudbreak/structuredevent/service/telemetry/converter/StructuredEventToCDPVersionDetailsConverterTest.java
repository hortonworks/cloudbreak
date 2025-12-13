package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredSyncEvent;

class StructuredEventToCDPVersionDetailsConverterTest {

    private StructuredEventToCDPVersionDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredEventToCDPVersionDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        assertNotNull(underTest.convert((StructuredFlowEvent) null), "We should return empty object for not null");
        assertNotNull(underTest.convert((StructuredSyncEvent) null), "We should return empty object for not null");
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPVersionDetails flowVersionDetails = underTest.convert(structuredFlowEvent);

        assertEquals("", flowVersionDetails.getCmVersion());
        assertEquals("", flowVersionDetails.getCdpdVersion());
        assertEquals("", flowVersionDetails.getCrVersion());
        assertEquals("", flowVersionDetails.getOsPatchLevel());
        assertEquals("", flowVersionDetails.getSaltVersion());
        assertEquals("", flowVersionDetails.getJavaVersion());
        assertFalse(flowVersionDetails.getJavaVersionForced());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPVersionDetails syncVersionDetails = underTest.convert(structuredSyncEvent);

        assertEquals("", syncVersionDetails.getCmVersion());
        assertEquals("", syncVersionDetails.getCdpdVersion());
        assertEquals("", syncVersionDetails.getCrVersion());
        assertEquals("", syncVersionDetails.getOsPatchLevel());
        assertEquals("", syncVersionDetails.getSaltVersion());
        assertEquals("", flowVersionDetails.getJavaVersion());
        assertFalse(flowVersionDetails.getJavaVersionForced());
    }

    @Test
    public void testConversionFileldOutValues() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());

        UsageProto.CDPVersionDetails flowVersionDetails = underTest.convert(structuredFlowEvent);

        assertEquals("7.3.0-11", flowVersionDetails.getCmVersion());
        assertEquals("7.2.7-22", flowVersionDetails.getCdpdVersion());
        assertEquals("7.2.7", flowVersionDetails.getCrVersion());
        assertEquals("2021-02-04", flowVersionDetails.getOsPatchLevel());
        assertEquals("3000.5", flowVersionDetails.getSaltVersion());
        assertEquals("bootstrap=something, cdh-build-number=22, cm-build-number=11, cm=7.3.0, date=2021-02-04," +
                " java=8, salt=3000.5, stack=7.2.7", flowVersionDetails.getAll());
        assertEquals("8", flowVersionDetails.getJavaVersion());
        assertFalse(flowVersionDetails.getJavaVersionForced());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());

        UsageProto.CDPVersionDetails syncVersionDetails = underTest.convert(structuredSyncEvent);

        assertEquals("7.3.0-11", syncVersionDetails.getCmVersion());
        assertEquals("7.2.7-22", syncVersionDetails.getCdpdVersion());
        assertEquals("7.2.7", syncVersionDetails.getCrVersion());
        assertEquals("2021-02-04", syncVersionDetails.getOsPatchLevel());
        assertEquals("3000.5", syncVersionDetails.getSaltVersion());
        assertEquals("bootstrap=something, cdh-build-number=22, cm-build-number=11, cm=7.3.0, date=2021-02-04," +
                " java=8, salt=3000.5, stack=7.2.7", syncVersionDetails.getAll());
        assertEquals("8", flowVersionDetails.getJavaVersion());
        assertFalse(flowVersionDetails.getJavaVersionForced());
    }

    @Test
    public void testJavaVersionForced() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails stackDetails = createStackDetails();
        stackDetails.setJavaVersion(11);
        structuredFlowEvent.setStack(stackDetails);

        UsageProto.CDPVersionDetails flowVersionDetails = underTest.convert(structuredFlowEvent);
        assertEquals("11", flowVersionDetails.getJavaVersion());
        assertTrue(flowVersionDetails.getJavaVersionForced());
    }

    private StackDetails createStackDetails() {
        StackDetails stackDetails = new StackDetails();
        ImageDetails imageDetails = new ImageDetails();

        stackDetails.setImage(imageDetails);

        imageDetails.setPackageVersions(Map.of("cm", "7.3.0",
                "cm-build-number", "11",
                "stack", "7.2.7",
                "cdh-build-number", "22",
                "date", "2021-02-04",
                "salt", "3000.5",
                "bootstrap", "something",
                "java", "8"));

        return stackDetails;
    }
}