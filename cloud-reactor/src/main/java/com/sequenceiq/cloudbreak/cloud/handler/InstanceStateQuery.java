package com.sequenceiq.cloudbreak.cloud.handler;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;

@Component
public class InstanceStateQuery {

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public List<CloudVmInstanceStatus> getCloudVmInstanceStatuses(
            CloudCredential cloudCredential, CloudContext cloudContext, List<CloudInstance> instances) {
        CloudConnector connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, cloudCredential);
        return connector.instances().check(auth, instances);
    }

}
