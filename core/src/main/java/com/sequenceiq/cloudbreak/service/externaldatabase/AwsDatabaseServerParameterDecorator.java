package com.sequenceiq.cloudbreak.service.externaldatabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.database.DatabaseAvailabilityType;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.environment.api.v1.environment.model.response.DetailedEnvironmentResponse;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.aws.AwsDatabaseServerV4Parameters;

@Component
public class AwsDatabaseServerParameterDecorator implements DatabaseServerParameterDecorator {

    @Value("${cb.aws.externaldatabase.ha.retentionperiod}")
    private int retentionPeriodHa;

    @Value("${cb.aws.externaldatabase.nonha.retentionperiod}")
    private int retentionPeriodNonHa;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, DatabaseServerParameter serverParameter, DetailedEnvironmentResponse env, boolean multiAz) {
        AwsDatabaseServerV4Parameters parameters = new AwsDatabaseServerV4Parameters();
        parameters.setBackupRetentionPeriod(serverParameter.getAvailabilityType() == DatabaseAvailabilityType.HA ? retentionPeriodHa : retentionPeriodNonHa);
        parameters.setMultiAZ(Boolean.toString(serverParameter.getAvailabilityType() == DatabaseAvailabilityType.HA));
        parameters.setEngineVersion(serverParameter.getEngineVersion());
        request.setAws(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.AWS;
    }
}
