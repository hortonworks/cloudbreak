package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPStatusDetails;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.DatalakeDetails;

class DatalakeDetailsToCDPStatusDetailsConverterTest {

    private static final String STATUS = "status";

    private static final String STATUS_REASON = "statusReason";

    private DatalakeDetailsToCDPStatusDetailsConverter underTest = new DatalakeDetailsToCDPStatusDetailsConverter();

    @Test
    void testConvert() {
        DatalakeDetails datalakeDetails = new DatalakeDetails();
        datalakeDetails.setStatus(STATUS);
        datalakeDetails.setStatusReason(STATUS_REASON);

        CDPStatusDetails result = underTest.convert(datalakeDetails);

        assertEquals(STATUS, result.getClusterStatus());
        assertEquals(STATUS, result.getStackStatus());
        assertEquals(STATUS, result.getStackDetailedStatus());
        assertEquals(STATUS_REASON, result.getStackStatusReason());
        assertEquals(STATUS_REASON, result.getClusterStatusReason());
    }

}