package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.Map;

import org.junit.Assert;
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
        Assert.assertNotNull("We should return empty object for not null", underTest.convert((StructuredFlowEvent) null));
        Assert.assertNotNull("We should return empty object for not null", underTest.convert((StructuredSyncEvent) null));
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPVersionDetails flowVersionDetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", flowVersionDetails.getCmVersion());
        Assert.assertEquals("", flowVersionDetails.getCdpdVersion());
        Assert.assertEquals("", flowVersionDetails.getCrVersion());
        Assert.assertEquals("", flowVersionDetails.getOsPatchLevel());
        Assert.assertEquals("", flowVersionDetails.getSaltVersion());
        Assert.assertEquals("", flowVersionDetails.getJavaVersion());
        Assert.assertFalse(flowVersionDetails.getJavaVersionForced());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();

        UsageProto.CDPVersionDetails syncVersionDetails = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("", syncVersionDetails.getCmVersion());
        Assert.assertEquals("", syncVersionDetails.getCdpdVersion());
        Assert.assertEquals("", syncVersionDetails.getCrVersion());
        Assert.assertEquals("", syncVersionDetails.getOsPatchLevel());
        Assert.assertEquals("", syncVersionDetails.getSaltVersion());
        Assert.assertEquals("", flowVersionDetails.getJavaVersion());
        Assert.assertFalse(flowVersionDetails.getJavaVersionForced());
    }

    @Test
    public void testConversionFileldOutValues() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        structuredFlowEvent.setStack(createStackDetails());

        UsageProto.CDPVersionDetails flowVersionDetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("7.3.0-11", flowVersionDetails.getCmVersion());
        Assert.assertEquals("7.2.7-22", flowVersionDetails.getCdpdVersion());
        Assert.assertEquals("7.2.7", flowVersionDetails.getCrVersion());
        Assert.assertEquals("2021-02-04", flowVersionDetails.getOsPatchLevel());
        Assert.assertEquals("3000.5", flowVersionDetails.getSaltVersion());
        Assert.assertEquals("bootstrap=something, cdh-build-number=22, cm-build-number=11, cm=7.3.0, date=2021-02-04," +
                " java=8, salt=3000.5, stack=7.2.7", flowVersionDetails.getAll());
        Assert.assertEquals("8", flowVersionDetails.getJavaVersion());
        Assert.assertFalse(flowVersionDetails.getJavaVersionForced());

        StructuredSyncEvent structuredSyncEvent = new StructuredSyncEvent();
        structuredSyncEvent.setStack(createStackDetails());

        UsageProto.CDPVersionDetails syncVersionDetails = underTest.convert(structuredSyncEvent);

        Assert.assertEquals("7.3.0-11", syncVersionDetails.getCmVersion());
        Assert.assertEquals("7.2.7-22", syncVersionDetails.getCdpdVersion());
        Assert.assertEquals("7.2.7", syncVersionDetails.getCrVersion());
        Assert.assertEquals("2021-02-04", syncVersionDetails.getOsPatchLevel());
        Assert.assertEquals("3000.5", syncVersionDetails.getSaltVersion());
        Assert.assertEquals("bootstrap=something, cdh-build-number=22, cm-build-number=11, cm=7.3.0, date=2021-02-04," +
                " java=8, salt=3000.5, stack=7.2.7", syncVersionDetails.getAll());
        Assert.assertEquals("8", flowVersionDetails.getJavaVersion());
        Assert.assertFalse(flowVersionDetails.getJavaVersionForced());
    }

    @Test
    public void testJavaVersionForced() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails stackDetails = createStackDetails();
        stackDetails.setJavaVersion(11);
        structuredFlowEvent.setStack(stackDetails);

        UsageProto.CDPVersionDetails flowVersionDetails = underTest.convert(structuredFlowEvent);
        Assert.assertEquals("11", flowVersionDetails.getJavaVersion());
        Assert.assertTrue(flowVersionDetails.getJavaVersionForced());
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