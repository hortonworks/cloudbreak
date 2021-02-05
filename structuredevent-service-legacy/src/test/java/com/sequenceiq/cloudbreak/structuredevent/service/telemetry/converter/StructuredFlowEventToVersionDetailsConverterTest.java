package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ImageDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StackDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

class StructuredFlowEventToVersionDetailsConverterTest {

    private StructuredFlowEventToVersionDetailsConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredFlowEventToVersionDetailsConverter();
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNotNull("We should return empty object for not null", underTest.convert(null));
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPVersionDetails versionDetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", versionDetails.getCmVersion());
        Assert.assertEquals("", versionDetails.getCdpdVersion());
        Assert.assertEquals("", versionDetails.getCrVersion());
        Assert.assertEquals("", versionDetails.getOsPatchLevel());
        Assert.assertEquals("", versionDetails.getSaltVersion());
    }

    @Test
    public void testConversionFileldOutValues() {
        StructuredFlowEvent structuredFlowEvent = createStructuredFlowEvent();

        UsageProto.CDPVersionDetails versionDetails = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("7.3.0-11", versionDetails.getCmVersion());
        Assert.assertEquals("7.2.7-22", versionDetails.getCdpdVersion());
        Assert.assertEquals("7.2.7", versionDetails.getCrVersion());
        Assert.assertEquals("2021-02-04", versionDetails.getOsPatchLevel());
        Assert.assertEquals("3000.5", versionDetails.getSaltVersion());
        Assert.assertEquals("bootstrap=something, cdh-build-number=22, cm-build-number=11, cm=7.3.0, date=2021-02-04," +
                " salt=3000.5, stack=7.2.7", versionDetails.getAll());
    }

    private StructuredFlowEvent createStructuredFlowEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();
        StackDetails stackDetails = new StackDetails();
        ImageDetails imageDetails = new ImageDetails();

        structuredFlowEvent.setStack(stackDetails);
        stackDetails.setImage(imageDetails);

        imageDetails.setPackageVersions(Map.of("cm", "7.3.0",
                "cm-build-number", "11",
                "stack", "7.2.7",
                "cdh-build-number", "22",
                "date", "2021-02-04",
                "salt", "3000.5",
                "bootstrap", "something"));

        return structuredFlowEvent;
    }
}