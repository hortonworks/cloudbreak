package com.sequenceiq.cloudbreak.validation;

import java.util.Date;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.telemetry.support.SupportBundleConfiguration;

public abstract class AbstractCMDiagnosticsCollectionValidator<T>
        extends AbstractDiagnosticsCollectionValidator<T, Status> {

    public AbstractCMDiagnosticsCollectionValidator(SupportBundleConfiguration supportBundleConfiguration) {
        super(supportBundleConfiguration);
    }

    @Override
    protected void validateStackStatus(Status stackStatus, String stackName) {
        if (Status.STOPPED.equals(stackStatus)
                || Status.DELETED_ON_PROVIDER_SIDE.equals(stackStatus)
                || Status.REQUESTED.equals(stackStatus)) {
            throw new BadRequestException(String.format("Cannot run diagnostics on Data Hub (name: '%s') if the stack is in %s state",
                    stackName, stackStatus));
        }
    }

    @Override
    protected ImageDateChecker getImageDateChecker() {
        return null;
    }

    protected void validateTimeRange(Date startTime, Date endTime) {
        if (startTime != null) {
            if (endTime != null) {
                if (startTime.after(endTime)) {
                    throw new BadRequestException("Start time must not be after end time.");
                }
            } else if (startTime.after(new Date())) {
                throw new BadRequestException("Start time must not be in the future.");
            }
        }
    }
}
