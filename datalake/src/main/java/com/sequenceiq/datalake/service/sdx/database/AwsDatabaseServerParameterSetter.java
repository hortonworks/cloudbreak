package com.sequenceiq.datalake.service.sdx.database;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.common.annotations.VisibleForTesting;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;

@Component
public class AwsDatabaseServerParameterSetter implements DatabaseServerParameterSetter {

    @VisibleForTesting
    @Value("${sdx.db.aws.ha.backupretentionperiod}")
    int backupRetentionPeriodHa;

    @VisibleForTesting
    @Value("${sdx.db.aws.nonha.backupretentionperiod}")
    int backupRetentionPeriodNonHa;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, SdxDatabaseAvailabilityType availabilityType, String databaseEngineVersion) {
        AwsDatabaseServerV4Parameters parameters = new AwsDatabaseServerV4Parameters();
        if (SdxDatabaseAvailabilityType.HA.equals(availabilityType)) {
            parameters.setBackupRetentionPeriod(backupRetentionPeriodHa);
            parameters.setMultiAZ("true");
        } else if (SdxDatabaseAvailabilityType.NON_HA.equals(availabilityType)) {
            parameters.setBackupRetentionPeriod(backupRetentionPeriodNonHa);
            parameters.setMultiAZ("false");
        } else {
            throw new IllegalArgumentException(availabilityType + " database availability type is not supported on AWS.");
        }
        if (StringUtils.isNotEmpty(databaseEngineVersion)) {
            parameters.setEngineVersion(databaseEngineVersion);
        }
        request.setAws(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
