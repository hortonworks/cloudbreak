package com.sequenceiq.cloudbreak.converter.scheduler;


import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.api.model.Status;
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
                return PollGroup.POLLABLE;
            case DELETE_COMPLETED:
                return PollGroup.CANCELLED;
            default:
                throw new UnsupportedOperationException(String.format("Status '%s' is not mapped to any PollGroup.", source));
        }
    }
}
