package com.sequenceiq.cloudbreak.converter.scheduler


import org.springframework.stereotype.Component

import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup
import com.sequenceiq.cloudbreak.converter.AbstractConversionServiceAwareConverter
import com.sequenceiq.cloudbreak.api.model.Status

@Component
class StatusToPollGroupConverter : AbstractConversionServiceAwareConverter<Status, PollGroup>() {
    override fun convert(source: Status): PollGroup {
        when (source) {
            Status.REQUESTED, Status.CREATE_IN_PROGRESS, Status.AVAILABLE, Status.UPDATE_IN_PROGRESS, Status.UPDATE_REQUESTED, Status.UPDATE_FAILED, Status.CREATE_FAILED, Status.ENABLE_SECURITY_FAILED, Status.STOPPED, Status.STOP_REQUESTED, Status.START_REQUESTED, Status.STOP_IN_PROGRESS, Status.START_IN_PROGRESS, Status.START_FAILED, Status.STOP_FAILED, Status.DELETE_FAILED -> return PollGroup.POLLABLE
            Status.DELETE_IN_PROGRESS, Status.DELETE_COMPLETED -> return PollGroup.CANCELLED
            else -> throw UnsupportedOperationException(String.format("Status '%s' is not mapped to any PollGroup.", source))
        }
    }
}
