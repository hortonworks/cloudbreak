package com.sequenceiq.cloudbreak.maintenancewindow;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.distrox.api.v1.distrox.model.DistroXMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;

@ExtendWith(MockitoExtension.class)
class DataHubMaintenanceWindowScheduleServiceTest {

    private static final String ACCOUNT_ID = "acc";

    private static final String CRN = "crn:cdp:datahub:us-west-1:" + ACCOUNT_ID + ":cluster:dh-1";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:1";

    @Mock
    private MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint;

    @InjectMocks
    private DataHubMaintenanceWindowScheduleService underTest;

    @Test
    void skipsCreateWhenScheduleAbsent() {
        underTest.createScheduleIfPresent(CRN, null);

        verify(scheduleInternalEndpoint, never()).create(any(), any(), any());
    }

    @Test
    void createsScheduleWhenPresentWithAccountFromCrnAndInitiatorUser() {
        DistroXMaintenanceWindowScheduleRequest schedule = new DistroXMaintenanceWindowScheduleRequest();
        schedule.setRecurrenceKind("WEEKLY");
        schedule.setDurationMinutes(60);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek("MONDAY");

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createScheduleIfPresent(CRN, schedule));

        verify(scheduleInternalEndpoint).create(eq(ACCOUNT_ID), eq(USER_CRN), any());
    }

    @Test
    void clearScheduleQuietlyDeletesSchedule() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.clearScheduleQuietly(CRN));

        verify(scheduleInternalEndpoint).delete(ACCOUNT_ID, USER_CRN, MaintenanceScopeType.DATAHUB.name(), CRN);
    }
}
