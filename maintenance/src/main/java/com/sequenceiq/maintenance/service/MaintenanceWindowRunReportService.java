package com.sequenceiq.maintenance.service;

import jakarta.inject.Inject;

import org.springframework.stereotype.Service;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.maintenance.api.v1.task.model.request.ReportMaintenanceWindowRunOutcomeRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowRunResponse;
import com.sequenceiq.maintenance.dispatcher.MaintenanceWindowRunService;
import com.sequenceiq.maintenance.domain.MaintenanceRunStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;

@Service
public class MaintenanceWindowRunReportService {

    private static final String TERMINAL_STATUS_REQUIRED = "status must be COMPLETED or FAILED.";

    private final MaintenanceWindowRunService runService;

    private final MaintenanceWindowRunConverter runConverter;

    @Inject
    public MaintenanceWindowRunReportService(
            MaintenanceWindowRunService runService,
            MaintenanceWindowRunConverter runConverter) {
        this.runService = runService;
        this.runConverter = runConverter;
    }

    public MaintenanceWindowRunResponse reportOutcome(
            String accountId,
            Long taskId,
            Long runId,
            ReportMaintenanceWindowRunOutcomeRequest request) {
        MaintenanceRunStatus terminalStatus = parseTerminalStatus(request.getStatus());
        MaintenanceWindowRun run = switch (terminalStatus) {
            case COMPLETED -> runService.completeRun(accountId, runId, taskId);
            case FAILED -> runService.failRun(accountId, runId, taskId, requireErrorDetail(request));
            default -> throw new BadRequestException(TERMINAL_STATUS_REQUIRED);
        };
        return runConverter.toResponse(run, taskId);
    }

    private static MaintenanceRunStatus parseTerminalStatus(String rawStatus) {
        if (rawStatus == null) {
            throw new BadRequestException(TERMINAL_STATUS_REQUIRED);
        }
        MaintenanceRunStatus parsed;
        try {
            parsed = MaintenanceRunStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(TERMINAL_STATUS_REQUIRED);
        }
        if (parsed != MaintenanceRunStatus.COMPLETED && parsed != MaintenanceRunStatus.FAILED) {
            throw new BadRequestException(TERMINAL_STATUS_REQUIRED);
        }
        return parsed;
    }

    private static String requireErrorDetail(ReportMaintenanceWindowRunOutcomeRequest request) {
        String errorDetail = request.getErrorDetail();
        if (errorDetail == null || errorDetail.isBlank()) {
            throw new BadRequestException("errorDetail is required when status is FAILED.");
        }
        return errorDetail;
    }
}
