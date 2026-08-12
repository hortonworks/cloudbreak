package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSkip;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;
import com.sequenceiq.maintenance.repository.MaintenanceWindowSkipRepository;
import com.sequenceiq.maintenance.service.model.MaintenanceWindowResourceIdentity;
import com.sequenceiq.maintenance.service.model.WindowOccurrence;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleEligibilityServiceTest {

    private static final String ACCOUNT_ID = "account-1";

    private static final String ENV_CRN = "crn:env:1";

    private static final String RESOURCE_CRN = "crn:dh:1";

    private static final long WINDOW_START = Instant.parse("2025-01-06T09:00:00Z").toEpochMilli();

    private static final long WINDOW_END = Instant.parse("2025-01-06T10:00:00Z").toEpochMilli();

    private static final long INSIDE_WINDOW_NOW = Instant.parse("2025-01-06T09:30:00Z").toEpochMilli();

    private static final long OUTSIDE_WINDOW_NOW = Instant.parse("2025-01-06T11:00:00Z").toEpochMilli();

    private static final long ANY_INSTANT = INSIDE_WINDOW_NOW;

    @Mock
    private MaintenanceWindowScheduleRepository scheduleRepository;

    @Mock
    private MaintenanceWindowSkipRepository skipRepository;

    @Mock
    private MaintenanceOccurrenceCalculator occurrenceCalculator;

    @InjectMocks
    private MaintenanceWindowScheduleEligibilityService underTest;

    private MaintenanceWindowResourceIdentity identity;

    @BeforeEach
    void setUp() {
        identity = new MaintenanceWindowResourceIdentity(ACCOUNT_ID, ENV_CRN, RESOURCE_CRN, MaintenanceScopeType.DATAHUB);
        lenient().when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(any(), any(), any()))
                .thenReturn(Optional.empty());
    }

    @Test
    void resolveEffectiveSchedulePrefersResourceScopeOverEnvironmentAndTenant() {
        MaintenanceWindowSchedule tenant = schedule(1L, MaintenanceScopeType.TENANT, ACCOUNT_ID);
        MaintenanceWindowSchedule environment = schedule(2L, MaintenanceScopeType.ENVIRONMENT, ENV_CRN);
        MaintenanceWindowSchedule resource = schedule(3L, MaintenanceScopeType.DATAHUB, RESOURCE_CRN);
        stubSchedule(resource);
        stubSchedule(environment);
        stubSchedule(tenant);

        assertThat(underTest.resolveEffectiveSchedule(identity)).contains(resource);
    }

    @Test
    void resolveEffectiveScheduleFallsBackToEnvironmentWhenResourceMissing() {
        MaintenanceWindowSchedule tenant = schedule(1L, MaintenanceScopeType.TENANT, ACCOUNT_ID);
        MaintenanceWindowSchedule environment = schedule(2L, MaintenanceScopeType.ENVIRONMENT, ENV_CRN);
        stubSchedule(environment);
        stubSchedule(tenant);

        assertThat(underTest.resolveEffectiveSchedule(identity)).contains(environment);
        verify(scheduleRepository).findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, RESOURCE_CRN);
    }

    @Test
    void resolveEffectiveScheduleFallsBackToTenantWhenOnlyTenantDefined() {
        MaintenanceWindowSchedule tenant = schedule(1L, MaintenanceScopeType.TENANT, ACCOUNT_ID);
        stubSchedule(tenant);

        assertThat(underTest.resolveEffectiveSchedule(identity)).contains(tenant);
        verify(scheduleRepository).findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, RESOURCE_CRN);
        verify(scheduleRepository).findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT, ENV_CRN);
    }

    @Test
    void resolveEffectiveScheduleEmptyWhenNoScheduleAtAnyScope() {
        assertThat(underTest.resolveEffectiveSchedule(identity)).isEmpty();
    }

    @Test
    void resolveEffectiveScheduleEmptyWhenAccountIdBlank() {
        MaintenanceWindowResourceIdentity blankAccountIdentity =
                new MaintenanceWindowResourceIdentity("  ", ENV_CRN, RESOURCE_CRN, MaintenanceScopeType.DATAHUB);

        assertThat(underTest.resolveEffectiveSchedule(blankAccountIdentity)).isEmpty();
        verify(scheduleRepository, never()).findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(any(), any(), any());
    }

    @Test
    void checkEligibilityNotDispatchableWhenNoSchedule() {
        MaintenanceWindowScheduleEligibility evaluation = underTest.checkEligibility(identity, ANY_INSTANT);

        assertThat(evaluation.dispatchable()).isFalse();
        assertThat(evaluation.schedule()).isEmpty();
        assertThat(evaluation.currentOccurrence()).isEmpty();
    }

    @Test
    void checkEligibilityDispatchableWhenInsideCurrentOccurrence() {
        MaintenanceWindowSchedule resource = schedule(3L, MaintenanceScopeType.DATAHUB, RESOURCE_CRN);
        stubSchedule(resource);
        WindowOccurrence occurrence = new WindowOccurrence(WINDOW_START, WINDOW_END);
        when(occurrenceCalculator.findOccurrenceContaining(resource, INSIDE_WINDOW_NOW)).thenReturn(Optional.of(occurrence));
        when(skipRepository.findByMaintenanceWindowScheduleIdAndWindowStart(resource.getId(), WINDOW_START))
                .thenReturn(Optional.empty());

        MaintenanceWindowScheduleEligibility evaluation = underTest.checkEligibility(identity, INSIDE_WINDOW_NOW);

        assertThat(evaluation.dispatchable()).isTrue();
        assertThat(evaluation.schedule()).contains(resource);
        assertThat(evaluation.currentOccurrence()).contains(occurrence);
        verify(skipRepository).findByMaintenanceWindowScheduleIdAndWindowStart(resource.getId(), WINDOW_START);
    }

    @Test
    void checkEligibilityNotDispatchableWhenOutsideWindow() {
        MaintenanceWindowSchedule tenant = schedule(1L, MaintenanceScopeType.TENANT, ACCOUNT_ID);
        stubSchedule(tenant);
        when(occurrenceCalculator.findOccurrenceContaining(tenant, OUTSIDE_WINDOW_NOW)).thenReturn(Optional.empty());

        MaintenanceWindowScheduleEligibility evaluation = underTest.checkEligibility(identity, OUTSIDE_WINDOW_NOW);

        assertThat(evaluation.dispatchable()).isFalse();
        assertThat(evaluation.schedule()).contains(tenant);
        assertThat(evaluation.currentOccurrence()).isEmpty();
        verifyNoInteractions(skipRepository);
    }

    @Test
    void checkEligibilityNotDispatchableWhenOccurrenceSkipped() {
        MaintenanceWindowSchedule resource = schedule(3L, MaintenanceScopeType.DATAHUB, RESOURCE_CRN);
        stubSchedule(resource);
        WindowOccurrence occurrence = new WindowOccurrence(WINDOW_START, WINDOW_END);
        when(occurrenceCalculator.findOccurrenceContaining(resource, INSIDE_WINDOW_NOW)).thenReturn(Optional.of(occurrence));
        MaintenanceWindowSkip skip = new MaintenanceWindowSkip();
        skip.setMaintenanceWindowSchedule(resource);
        skip.setWindowStart(WINDOW_START);
        when(skipRepository.findByMaintenanceWindowScheduleIdAndWindowStart(resource.getId(), WINDOW_START))
                .thenReturn(Optional.of(skip));

        MaintenanceWindowScheduleEligibility evaluation = underTest.checkEligibility(identity, INSIDE_WINDOW_NOW);

        assertThat(evaluation.dispatchable()).isFalse();
        assertThat(evaluation.schedule()).contains(resource);
        assertThat(evaluation.currentOccurrence()).isEmpty();
    }

    private MaintenanceWindowSchedule schedule(long id, MaintenanceScopeType scopeType, String scopeId) {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        schedule.setId(id);
        schedule.setAccountId(ACCOUNT_ID);
        schedule.setScopeType(scopeType);
        schedule.setScopeId(scopeId);
        schedule.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY);
        schedule.setTimezone("UTC");
        schedule.setDurationMinutes(60);
        schedule.setStartLocalTime("09:00");
        schedule.setDayOfWeek(DayOfWeek.MONDAY);
        return schedule;
    }

    private void stubSchedule(MaintenanceWindowSchedule schedule) {
        lenient().when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, schedule.getScopeType(), schedule.getScopeId())).thenReturn(Optional.of(schedule));
    }
}
