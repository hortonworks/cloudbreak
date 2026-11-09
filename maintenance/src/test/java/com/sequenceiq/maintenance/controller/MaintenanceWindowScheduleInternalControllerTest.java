package com.sequenceiq.maintenance.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleService;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleInternalControllerTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String SCOPE_ID = "crn:cdp:datalake:us-west-1:" + ACCOUNT_ID + ":datalake:dl-1";

    private static final String INITIATOR_USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:1";

    @Mock
    private MaintenanceWindowScheduleService scheduleService;

    @InjectMocks
    private MaintenanceWindowScheduleInternalController underTest;

    @Test
    void createDelegatesToServiceWithExplicitAccountIdAndInitiatorUserAsAuditActor() {
        MaintenanceWindowScheduleRequest request = new MaintenanceWindowScheduleRequest();
        MaintenanceWindowScheduleResponse response = new MaintenanceWindowScheduleResponse();
        when(scheduleService.create(eq(request), eq(ACCOUNT_ID), eq(INITIATOR_USER_CRN))).thenReturn(response);

        MaintenanceWindowScheduleResponse result = underTest.create(ACCOUNT_ID, INITIATOR_USER_CRN, request);

        assertThat(result).isSameAs(response);
        verify(scheduleService).create(eq(request), eq(ACCOUNT_ID), eq(INITIATOR_USER_CRN));
    }

    @Test
    void deleteDelegatesToServiceWithExplicitAccountIdAndInitiatorUserAsAuditActor() {
        underTest.delete(ACCOUNT_ID, INITIATOR_USER_CRN, MaintenanceScopeType.DATALAKE.name(), SCOPE_ID);

        verify(scheduleService).delete(eq(ACCOUNT_ID), eq(MaintenanceScopeType.DATALAKE), eq(SCOPE_ID), eq(INITIATOR_USER_CRN));
    }

    @Test
    void getDelegatesToServiceWithExplicitAccountId() {
        MaintenanceWindowScheduleResponse response = new MaintenanceWindowScheduleResponse();
        when(scheduleService.get(ACCOUNT_ID, MaintenanceScopeType.DATALAKE, SCOPE_ID)).thenReturn(response);

        MaintenanceWindowScheduleResponse result = underTest.get(ACCOUNT_ID, MaintenanceScopeType.DATALAKE.name(), SCOPE_ID);

        assertThat(result).isSameAs(response);
    }
}
