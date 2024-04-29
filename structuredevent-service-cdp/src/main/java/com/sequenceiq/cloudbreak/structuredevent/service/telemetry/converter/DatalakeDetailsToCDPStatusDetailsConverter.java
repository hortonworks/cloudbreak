package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPStatusDetails;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.DatalakeDetails;

@Component
public class DatalakeDetailsToCDPStatusDetailsConverter {

    private static final int MAX_STRING_LENGTH = 1500;

    public CDPStatusDetails convert(DatalakeDetails datalakeDetails) {
        CDPStatusDetails.Builder builder = CDPStatusDetails.newBuilder();
        if (datalakeDetails != null) {
            String status = defaultIfEmpty(datalakeDetails.getStatus(), "");
            String statusReason = defaultIfEmpty(StringUtils.substring(datalakeDetails.getStatusReason(),
                    0, Math.min(StringUtils.length(datalakeDetails.getStatusReason()), MAX_STRING_LENGTH)), "");
            builder.setStackStatus(status)
                    .setStackDetailedStatus(status)
                    .setStackStatusReason(statusReason)
                    .setClusterStatus(status)
                    .setClusterStatusReason(statusReason);
        }
        return builder.build();
    }
}
