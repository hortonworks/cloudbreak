package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPDatalakeFeatures;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.DatalakeDetails;

class DatalakeDetailsToCDPDatalakeFeaturesConverterTest {

    private DatalakeDetailsToCDPDatalakeFeaturesConverter underTest = new DatalakeDetailsToCDPDatalakeFeaturesConverter();

    @Test
    void testConvertWhenRazEnabled() {
        DatalakeDetails datalakeDetails = new DatalakeDetails();
        datalakeDetails.setRazEnabled(true);

        CDPDatalakeFeatures result = underTest.convert(datalakeDetails);

        assertEquals("ENABLED", result.getRaz().getStatus());
    }

    @Test
    void testConvertWhenRazDisabled() {
        DatalakeDetails datalakeDetails = new DatalakeDetails();
        datalakeDetails.setRazEnabled(false);

        CDPDatalakeFeatures result = underTest.convert(datalakeDetails);

        assertEquals("DISABLED", result.getRaz().getStatus());
    }

}