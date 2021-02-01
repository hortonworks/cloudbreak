package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.StructuredFlowEvent;

class StructuredFlowEventToClusterShapeConverterTest {

    private StructuredFlowEventToClusterShapeConverter underTest;

    @BeforeEach
    public void setUp() {
        underTest = new StructuredFlowEventToClusterShapeConverter();
    }

    @Test
    public void testConvertWithNull() {
        Assert.assertNotNull("We should return empty object for not null", underTest.convert(null));
    }

    @Test
    public void testConversionWithEmptyStructuredEvent() {
        StructuredFlowEvent structuredFlowEvent = new StructuredFlowEvent();

        UsageProto.CDPClusterShape clusterShape = underTest.convert(structuredFlowEvent);

        Assert.assertEquals("", clusterShape.getClusterTemplateName());
        Assert.assertEquals(0, clusterShape.getNodes());
        Assert.assertEquals("", clusterShape.getDefinitionDetails());
    }

}