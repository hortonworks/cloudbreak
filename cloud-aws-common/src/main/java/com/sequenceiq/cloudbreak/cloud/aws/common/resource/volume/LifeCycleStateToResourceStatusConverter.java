package com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;

import software.amazon.awssdk.services.efs.model.LifeCycleState;

@Component
public class LifeCycleStateToResourceStatusConverter {

    public ResourceStatus convert(LifeCycleState lifecycleState) {
        switch (lifecycleState) {
        case AVAILABLE:
            return ResourceStatus.CREATED;
        case DELETED:
            return ResourceStatus.DELETED;
        case ERROR:
            return ResourceStatus.FAILED;
        case CREATING:
        case UPDATING:
        case DELETING:
        default:
            return ResourceStatus.IN_PROGRESS;
        }
    }
}
