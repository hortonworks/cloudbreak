package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.maintenance.api.v1.task.model.request.ReportMaintenanceWindowRunOutcomeRequest;
import com.sequenceiq.maintenance.api.v1.task.model.response.MaintenanceWindowRunResponse;
import com.sequenceiq.maintenance.dispatcher.MaintenanceWindowRunService;
import com.sequenceiq.maintenance.domain.MaintenanceRunStatus;
import com.sequenceiq.maintenance.domain.MaintenanceWindowRun;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowRunReportServiceTest {

    private static final String ACCOUNT_ID = "account-1";

    @Mock
    private MaintenanceWindowRunService runService;

    @Mock
    private MaintenanceWindowRunConverter runConverter;

    private MaintenanceWindowRunReportService underTest;

    @BeforeEach
    void setUp() {
        underTest = new MaintenanceWindowRunReportService(runService, runConverter);
    }

    @Test
    void reportOutcomeCompletedDelegatesToCompleteRun() {
        MaintenanceWindowRun run = new MaintenanceWindowRun();
        run.setStatus(MaintenanceRunStatus.COMPLETED);
        MaintenanceWindowRunResponse response = new MaintenanceWindowRunResponse();
        response.setStatus("COMPLETED");
        when(runService.completeRun(ACCOUNT_ID, 10L, 5L)).thenReturn(run);
        when(runConverter.toResponse(run, 5L)).thenReturn(response);

        ReportMaintenanceWindowRunOutcomeRequest request = new ReportMaintenanceWindowRunOutcomeRequest();
        request.setStatus("COMPLETED");

        assertThat(underTest.reportOutcome(ACCOUNT_ID, 5L, 10L, request).getStatus()).isEqualTo("COMPLETED");
        verify(runService).completeRun(ACCOUNT_ID, 10L, 5L);
    }

    @Test
    void reportOutcomeFailedRequiresErrorDetail() {
        ReportMaintenanceWindowRunOutcomeRequest request = new ReportMaintenanceWindowRunOutcomeRequest();
        request.setStatus("FAILED");

        assertThatThrownBy(() -> underTest.reportOutcome(ACCOUNT_ID, 5L, 10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("errorDetail");
    }

    @Test
    void reportOutcomeFailedRejectsBlankErrorDetail() {
        ReportMaintenanceWindowRunOutcomeRequest request = new ReportMaintenanceWindowRunOutcomeRequest();
        request.setStatus("FAILED");
        request.setErrorDetail("   ");

        assertThatThrownBy(() -> underTest.reportOutcome(ACCOUNT_ID, 5L, 10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("errorDetail");
    }

    @Test
    void reportOutcomeFailedDelegatesToFailRun() {
        MaintenanceWindowRun run = new MaintenanceWindowRun();
        run.setStatus(MaintenanceRunStatus.FAILED);
        when(runService.failRun(ACCOUNT_ID, 10L, 5L, "timeout")).thenReturn(run);
        when(runConverter.toResponse(run, 5L)).thenReturn(new MaintenanceWindowRunResponse());

        ReportMaintenanceWindowRunOutcomeRequest request = new ReportMaintenanceWindowRunOutcomeRequest();
        request.setStatus("FAILED");
        request.setErrorDetail("timeout");

        underTest.reportOutcome(ACCOUNT_ID, 5L, 10L, request);
        verify(runService).failRun(ACCOUNT_ID, 10L, 5L, "timeout");
    }

    @Test
    void reportOutcomeRejectsNonTerminalStatus() {
        ReportMaintenanceWindowRunOutcomeRequest request = new ReportMaintenanceWindowRunOutcomeRequest();
        request.setStatus("RUNNING");

        assertThatThrownBy(() -> underTest.reportOutcome(ACCOUNT_ID, 5L, 10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COMPLETED or FAILED");
    }

    @Test
    void reportOutcomeRejectsUnknownStatus() {
        ReportMaintenanceWindowRunOutcomeRequest request = new ReportMaintenanceWindowRunOutcomeRequest();
        request.setStatus("PARTIAL");

        assertThatThrownBy(() -> underTest.reportOutcome(ACCOUNT_ID, 5L, 10L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("COMPLETED or FAILED");
    }
}
