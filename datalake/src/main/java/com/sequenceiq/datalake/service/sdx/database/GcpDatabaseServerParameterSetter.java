package com.sequenceiq.datalake.service.sdx.database;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.gcp.GcpDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Component
public class GcpDatabaseServerParameterSetter implements DatabaseServerParameterSetter {

    @VisibleForTesting
    @Value("${sdx.db.gcp.nonha.backupretentionperiod}")
    int backupRetentionPeriodNonHa;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, SdxDatabaseAvailabilityType availabilityType) {
        GcpDatabaseServerV4Parameters parameters = new GcpDatabaseServerV4Parameters();
        if (SdxDatabaseAvailabilityType.HA.equals(availabilityType)) {
            parameters.setBackupRetentionDays(backupRetentionPeriodNonHa);
        } else if (SdxDatabaseAvailabilityType.NON_HA.equals(availabilityType)) {
            parameters.setBackupRetentionDays(backupRetentionPeriodNonHa);
        } else {
            throw new IllegalArgumentException(availabilityType + " database availability type is not supported on Azure.");
        }
        request.setGcp(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }
}
