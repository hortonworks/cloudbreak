package com.sequenceiq.maintenance.dispatcher.model;

import java.util.Optional;

import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

public record TaskDispatchEvaluation(
        boolean shouldDispatch,
        TaskDispatchSkipReason reason,
        WindowOccurrence occurrence,
        MaintenanceWindowRun priorRun) {

    public static TaskDispatchEvaluation dispatch(WindowOccurrence occurrence, MaintenanceWindowRun priorRun) {
        return new TaskDispatchEvaluation(true, null, occurrence, priorRun);
    }

    public static TaskDispatchEvaluation skip(
            TaskDispatchSkipReason reason,
            WindowOccurrence occurrence,
            MaintenanceWindowRun priorRun) {
        return new TaskDispatchEvaluation(false, reason, occurrence, priorRun);
    }

    public Optional<TaskDispatchSkipReason> skipReason() {
        return Optional.ofNullable(reason);
    }

    public Optional<MaintenanceWindowRun> existingRun() {
        return Optional.ofNullable(priorRun);
    }
}
