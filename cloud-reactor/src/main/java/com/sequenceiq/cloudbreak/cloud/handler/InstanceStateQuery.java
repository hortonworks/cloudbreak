package com.sequenceiq.cloudbreak.cloud.handler;

import java.util.List;

import javax.inject.Inject;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudVmInstanceStatus;

@Component
public class InstanceStateQuery {

    @Inject
    private CloudPlatformConnectors cloudPlatformConnectors;

    public List<CloudVmInstanceStatus> getCloudVmInstanceStatuses(
            CloudCredential cloudCredential, CloudContext cloudContext, List<CloudInstance> instances) {
        CloudConnector<Object> connector = cloudPlatformConnectors.get(cloudContext.getPlatformVariant());
        AuthenticatedContext auth = connector.authentication().authenticate(cloudContext, cloudCredential);
        return connector.instances().check(auth, instances);
    }

}
