package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleListResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleServiceTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:acc-1:user:1";

    private static final long NOW = 1_735_689_600_000L;

    @Mock
    private MaintenanceWindowScheduleRepository scheduleRepository;

    @Mock
    private MaintenanceWindowScheduleValidator scheduleValidator;

    @Mock
    private Clock clock;

    private MaintenanceOccurrenceCalculator occurrenceCalculator;

    private MaintenanceWindowScheduleConverter scheduleConverter;

    private MaintenanceWindowScheduleService scheduleService;

    @BeforeEach
    void setUp() {
        occurrenceCalculator = new MaintenanceOccurrenceCalculator();
        scheduleConverter = new MaintenanceWindowScheduleConverter(occurrenceCalculator, clock);
        scheduleService = new MaintenanceWindowScheduleService(scheduleRepository, scheduleValidator, scheduleConverter, clock);
    }

    @Test
    void createPersistsValidatedSchedule() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowScheduleRequest request = request("tenant-default");
        MaintenanceWindowSchedule saved = entityFromRequest(request);
        saved.setId(1L);
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(MaintenanceWindowSchedule.class))).thenReturn(saved);

        MaintenanceWindowScheduleResponse response = scheduleService.create(request, ACCOUNT_ID, USER_CRN);

        assertThat(response.getName()).isEqualTo("tenant-default");
        assertThat(response.getNextOccurrenceStart()).isNotNull();
        verify(scheduleValidator).validate(any(MaintenanceWindowSchedule.class), eq(null));
    }

    @Test
    void createRejectsDuplicateScheduleAtScope() {
        MaintenanceWindowScheduleRequest request = request("tenant-default");
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID))
                .thenReturn(Optional.of(entityFromRequest(request)));

        assertThrows(ConflictException.class, () -> scheduleService.create(request, ACCOUNT_ID, USER_CRN));
    }

    @Test
    void createMapsDuplicateKeyToConflict() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowScheduleRequest request = request("tenant-default");
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(MaintenanceWindowSchedule.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        ConflictException exception = assertThrows(ConflictException.class, () -> scheduleService.create(request, ACCOUNT_ID, USER_CRN));

        assertThat(exception.getMessage()).contains("already exists");
    }

    @Test
    void listReturnsAllSchedulesForAccountWhenScopeFilterOmitted() {
        MaintenanceWindowSchedule schedule = entityFromRequest(request("tenant-default"));
        schedule.setId(1L);
        when(scheduleRepository.findByAccountIdAndArchivedFalse(ACCOUNT_ID)).thenReturn(List.of(schedule));

        MaintenanceWindowScheduleListResponse response = scheduleService.list(ACCOUNT_ID, null, null);

        assertThat(response.getSchedules()).hasSize(1);
        assertThat(response.getSchedules().get(0).getName()).isEqualTo("tenant-default");
        verify(scheduleRepository).findByAccountIdAndArchivedFalse(ACCOUNT_ID);
    }

    @Test
    void listFiltersByScopeWhenBothScopeParamsProvided() {
        MaintenanceWindowSchedule schedule = entityFromRequest(request("tenant-default"));
        schedule.setId(1L);
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(Optional.of(schedule));

        MaintenanceWindowScheduleListResponse response = scheduleService.list(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID);

        assertThat(response.getSchedules()).hasSize(1);
        assertThat(response.getSchedules().get(0).getScopeId()).isEqualTo(ACCOUNT_ID);
        verify(scheduleRepository).findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID);
    }

    @Test
    void createRejectsMissingScopeTypeWithoutNpe() {
        MaintenanceWindowScheduleService serviceWithRealValidator = new MaintenanceWindowScheduleService(
                scheduleRepository,
                new MaintenanceWindowScheduleValidator(scheduleRepository, occurrenceCalculator, clock),
                scheduleConverter,
                clock);
        MaintenanceWindowScheduleRequest request = request("tenant-default");
        request.setScopeType(null);
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(any(), any(), any()))
                .thenReturn(Optional.empty());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> serviceWithRealValidator.create(request, ACCOUNT_ID, USER_CRN));

        assertThat(exception.getMessage()).contains("scopeType");
    }

    @Test
    void createAppliesScopeDerivedDefaultNameWhenNameOmitted() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowScheduleRequest request = request(null);
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(MaintenanceWindowSchedule.class))).thenAnswer(invocation -> {
            MaintenanceWindowSchedule schedule = invocation.getArgument(0);
            schedule.setId(1L);
            return schedule;
        });

        MaintenanceWindowScheduleResponse response = scheduleService.create(request, ACCOUNT_ID, USER_CRN);

        assertThat(response.getName()).isEqualTo("tenant_" + ACCOUNT_ID);
    }

    @Test
    void updateAppliesMutableFieldsFromRequest() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowSchedule schedule = entityFromRequest(request("tenant-default"));
        schedule.setId(1L);
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(MaintenanceWindowSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateMaintenanceWindowScheduleRequest updateRequest = updateRequest("updated-name", 180);

        MaintenanceWindowScheduleResponse response = scheduleService.update(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID, updateRequest, USER_CRN);

        assertThat(response.getName()).isEqualTo("updated-name");
        assertThat(response.getDurationMinutes()).isEqualTo(180);
        assertThat(response.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(response.getScopeType()).isEqualTo(MaintenanceScopeType.TENANT.name());
        assertThat(response.getScopeId()).isEqualTo(ACCOUNT_ID);
        assertThat(schedule.getUpdatedAt()).isEqualTo(NOW);
        assertThat(schedule.getUpdatedBy()).isEqualTo(USER_CRN);
        verify(scheduleValidator).validate(schedule, 1L);
    }

    @Test
    void createDefaultsTimezoneToUtcWhenOmitted() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowScheduleRequest request = request("tenant-default");
        request.setTimezone(null);
        MaintenanceWindowSchedule saved = entityFromRequest(request);
        saved.setId(1L);
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(MaintenanceWindowSchedule.class))).thenReturn(saved);

        MaintenanceWindowScheduleResponse response = scheduleService.create(request, ACCOUNT_ID, USER_CRN);

        assertThat(response.getTimezone()).isEqualTo("UTC");
    }

    @Test
    void deleteArchivesScheduleInsteadOfRemovingRow() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowSchedule schedule = entityFromRequest(request("tenant-default"));
        schedule.setId(1L);
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(MaintenanceWindowSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduleService.delete(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID, USER_CRN);

        assertThat(schedule.isArchived()).isTrue();
        assertThat(schedule.getName()).isEqualTo("tenant-default");
        assertThat(schedule.getUpdatedAt()).isEqualTo(NOW);
        assertThat(schedule.getUpdatedBy()).isEqualTo(USER_CRN);
        verify(scheduleRepository, never()).delete(any());
        verify(scheduleRepository).save(schedule);
    }

    private UpdateMaintenanceWindowScheduleRequest updateRequest(String name, int durationMinutes) {
        UpdateMaintenanceWindowScheduleRequest request = new UpdateMaintenanceWindowScheduleRequest();
        request.setName(name);
        request.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        request.setTimezone("UTC");
        request.setDurationMinutes(durationMinutes);
        request.setStartLocalTime("09:00");
        request.setDayOfWeek(DayOfWeek.MONDAY.name());
        return request;
    }

    private MaintenanceWindowScheduleRequest request(String name) {
        MaintenanceWindowScheduleRequest request = new MaintenanceWindowScheduleRequest();
        if (name != null) {
            request.setName(name);
        }
        request.setScopeType(MaintenanceScopeType.TENANT.name());
        request.setScopeId(ACCOUNT_ID);
        request.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        request.setTimezone("UTC");
        request.setDurationMinutes(120);
        request.setStartLocalTime("09:00");
        request.setDayOfWeek(DayOfWeek.MONDAY.name());
        return request;
    }

    private MaintenanceWindowSchedule entityFromRequest(MaintenanceWindowScheduleRequest request) {
        MaintenanceWindowSchedule schedule = new MaintenanceWindowSchedule();
        scheduleConverter.applyRequest(schedule, request);
        schedule.setAccountId(ACCOUNT_ID);
        schedule.setCreatedAt(NOW);
        schedule.setUpdatedAt(NOW);
        schedule.setCreatedBy(USER_CRN);
        schedule.setUpdatedBy(USER_CRN);
        return schedule;
    }
}
