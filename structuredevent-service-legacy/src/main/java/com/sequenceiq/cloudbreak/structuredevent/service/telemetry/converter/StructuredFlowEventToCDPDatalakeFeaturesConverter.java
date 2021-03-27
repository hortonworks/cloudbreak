package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto;
import com.sequenceiq.cloudbreak.structuredevent.event.ClusterDetails;

@Component
public class StructuredFlowEventToCDPDatalakeFeaturesConverter {

    private static final Logger LOGGER = LoggerFactory.getLogger(StructuredFlowEventToCDPDatalakeFeaturesConverter.class);

    public UsageProto.CDPDatalakeFeatures convert(ClusterDetails clusterDetails) {
        UsageProto.CDPDatalakeFeatures.Builder cdpDatalakeFeatures = UsageProto.CDPDatalakeFeatures.newBuilder();

        if (clusterDetails != null) {
            UsageProto.CDPRaz.Builder cdpRaz = UsageProto.CDPRaz.newBuilder();
            cdpRaz.setStatus(clusterDetails.isRazEnabled() ? "ENABLED" : "DISABLED");
            cdpDatalakeFeatures.setRaz(cdpRaz.build());
        }

        UsageProto.CDPDatalakeFeatures ret = cdpDatalakeFeatures.build();
        LOGGER.debug("Converted CDPDatalakeFeatures telemetry event: {}", ret);
        return ret;
    }

}
