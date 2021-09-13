package com.sequenceiq.cloudbreak.converter.scheduler;


import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter;

@Component
public class StatusToPollGroupConverter extends AbstractConversionServiceAwareConverter<Status, PollGroup> {
    @Override
    public PollGroup convert(Status source) {
        switch (source) {
            case REQUESTED:
            case CREATE_IN_PROGRESS:
            case AVAILABLE:
            case UPDATE_IN_PROGRESS:
            case UPDATE_REQUESTED:
            case UPDATE_FAILED:
            case CREATE_FAILED:
            case ENABLE_SECURITY_FAILED:
            case STOPPED:
            case STOP_REQUESTED:
            case START_REQUESTED:
            case STOP_IN_PROGRESS:
            case START_IN_PROGRESS:
            case START_FAILED:
            case STOP_FAILED:
            case WAIT_FOR_SYNC:
            case MAINTENANCE_MODE_ENABLED:
            case PRE_DELETE_IN_PROGRESS:
            case DELETE_FAILED:
            case DELETE_IN_PROGRESS:
            case DELETED_ON_PROVIDER_SIDE:
            case AMBIGUOUS:
            case UNREACHABLE:
            case NODE_FAILURE:
            case EXTERNAL_DATABASE_CREATION_FAILED:
            case EXTERNAL_DATABASE_CREATION_IN_PROGRESS:
            case EXTERNAL_DATABASE_DELETION_FAILED:
            case EXTERNAL_DATABASE_DELETION_FINISHED:
            case EXTERNAL_DATABASE_DELETION_IN_PROGRESS:
            case EXTERNAL_DATABASE_START_FAILED:
            case EXTERNAL_DATABASE_START_FINISHED:
            case EXTERNAL_DATABASE_START_IN_PROGRESS:
            case EXTERNAL_DATABASE_STOP_FAILED:
            case EXTERNAL_DATABASE_STOP_FINISHED:
            case EXTERNAL_DATABASE_STOP_IN_PROGRESS:
            case BACKUP_IN_PROGRESS:
            case BACKUP_FAILED:
            case RESTORE_IN_PROGRESS:
            case RESTORE_FAILED:
            case LOAD_BALANCER_UPDATE_IN_PROGRESS:
            case LOAD_BALANCER_UPDATE_FAILED:
                return PollGroup.POLLABLE;
            case DELETE_COMPLETED:
                return PollGroup.CANCELLED;
            default:
                throw new UnsupportedOperationException(String.format("Status '%s' is not mapped to any PollGroup.", source));
        }
    }
}
