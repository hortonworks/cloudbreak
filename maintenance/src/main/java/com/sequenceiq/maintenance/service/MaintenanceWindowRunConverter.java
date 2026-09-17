package com.sequenceiq.maintenance.service;

import org.springframework.stereotype.Component;

import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowRunResponse;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;

@Component
public class MaintenanceWindowRunConverter {

    /**
     * @param taskId owning task id (caller-supplied) — avoids lazy-loading {@link MaintenanceWindowRun#getMaintenanceWindowTask()}
     *             after the persistence transaction that updated {@code run} has ended
     */
    public MaintenanceWindowRunResponse toResponse(MaintenanceWindowRun run, Long taskId) {
        MaintenanceWindowRunResponse response = new MaintenanceWindowRunResponse();
        response.setId(run.getId());
        response.setTaskId(taskId);
        response.setStatus(run.getStatus().name());
        response.setErrorDetail(run.getErrorDetail());
        response.setWindowStart(run.getWindowStart());
        response.setWindowEnd(run.getWindowEnd());
        response.setWindowExecutionEnd(run.getWindowExecutionEnd());
        response.setAttemptCount(run.getAttemptCount());
        response.setVersion(run.getVersion());
        return response;
    }
}
