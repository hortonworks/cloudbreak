package com.sequenceiq.cloudbreak.cloud.aws.common;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.cloud.NotificationConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.event.ResourceEvent;

@Service
public class AwsNotificationConnector implements NotificationConnector {

    @Override
    public Optional<ResourceEvent> preStartNotificiationEvent(AuthenticatedContext ac, List<CloudResource> resources, List<CloudInstance> vms) {
        if (vms.size() >= AwsInstanceConnector.RESILIENT_START_THRESHOLD) {
            return Optional.of(ResourceEvent.AWS_LARGE_CLUSTER_START);
        }
        return Optional.empty();
    }
}
