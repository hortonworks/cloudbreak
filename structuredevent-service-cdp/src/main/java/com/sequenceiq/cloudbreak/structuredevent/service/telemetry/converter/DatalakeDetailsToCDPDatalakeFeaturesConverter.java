package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPDatalakeFeatures;
import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPRaz;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.DatalakeDetails;

@Component
public class DatalakeDetailsToCDPDatalakeFeaturesConverter {

    public CDPDatalakeFeatures convert(DatalakeDetails datalakeDetails) {
        CDPDatalakeFeatures.Builder cdpDatalakeFeatures = CDPDatalakeFeatures.newBuilder();
        if (datalakeDetails != null) {
            cdpDatalakeFeatures.setRaz(CDPRaz.newBuilder()
                    .setStatus(datalakeDetails.isRazEnabled() ? "ENABLED" : "DISABLED")
                    .build());
        }
        return cdpDatalakeFeatures.build();
    }
}
