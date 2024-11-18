package com.sequenceiq.cloudbreak.structuredevent.service.telemetry.converter;

import static com.cloudera.thunderhead.service.common.usage.UsageProto.CDPDatabaseDetails;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.cloudera.thunderhead.service.common.usage.UsageProto.CDPClusterDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.DatabaseDetails;
import com.sequenceiq.cloudbreak.structuredevent.event.cdp.datalake.DatalakeDetails;
import com.sequenceiq.common.model.SeLinux;

@Component
public class DatalakeDetailsToCDPClusterDetailsConverter {

    public CDPClusterDetails convert(DatalakeDetails datalakeDetails) {
        CDPClusterDetails.Builder cdpClusterDetails = CDPClusterDetails.newBuilder();
        if (datalakeDetails != null) {
            cdpClusterDetails.setMultiAz(datalakeDetails.isMultiAzEnabled());
            cdpClusterDetails.setSeLinux(datalakeDetails.getSeLinux() == null ?
                    SeLinux.PERMISSIVE.name() : datalakeDetails.getSeLinux().name());
            DatabaseDetails databaseDetails = datalakeDetails.getDatabaseDetails();
            if (databaseDetails != null) {
                cdpClusterDetails.setDatabaseDetails(convert(databaseDetails));
            }
        }
        return cdpClusterDetails.build();
    }

    private CDPDatabaseDetails convert(DatabaseDetails databaseDetails) {
        CDPDatabaseDetails.Builder builder = CDPDatabaseDetails.newBuilder();
        if (databaseDetails != null) {
            builder.setEngineVersion(Objects.requireNonNullElse(databaseDetails.getEngineVersion(), ""))
                    .setAvailabilityType(Objects.requireNonNullElse(databaseDetails.getAvailabilityType(), ""))
                    .setAttributes(Objects.requireNonNullElse(databaseDetails.getAttributes(), ""));
        }
        return builder.build();
    }

}
