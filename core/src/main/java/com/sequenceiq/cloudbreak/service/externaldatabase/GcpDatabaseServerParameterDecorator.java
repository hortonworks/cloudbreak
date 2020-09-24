package com.sequenceiq.cloudbreak.service.externaldatabase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.service.externaldatabase.model.DatabaseServerParameter;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.DatabaseServerV4StackRequest;
import com.sequenceiq.redbeams.api.endpoint.v4.stacks.gcp.GcpDatabaseServerV4Parameters;

@Component
public class GcpDatabaseServerParameterDecorator implements DatabaseServerParameterDecorator {

    @Value("${cb.gcp.externaldatabase.engineversion}")
    private String engineVersion;

    @Override
    public void setParameters(DatabaseServerV4StackRequest request, DatabaseServerParameter serverParameter) {
        GcpDatabaseServerV4Parameters parameters = new GcpDatabaseServerV4Parameters();
        parameters.setEngineVersion(engineVersion);
        request.setGcp(parameters);
    }

    @Override
    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.GCP;
    }
}
