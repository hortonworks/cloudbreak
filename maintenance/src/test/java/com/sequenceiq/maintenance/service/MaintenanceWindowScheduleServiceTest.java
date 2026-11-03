package com.sequenceiq.maintenance.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowEffectiveScheduleParams;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRestoreRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowEffectiveScheduleResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleListResponse;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSkip;
import com.sequenceiq.maintenance.exception.ConflictException;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;
import com.sequenceiq.maintenance.repository.MaintenanceWindowSkipRepository;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleLookupContext;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleResolution;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleResourceContext;
import com.sequenceiq.maintenance.service.model.MaintenanceWindowResourceIdentity;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleServiceTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:acc-1:user:1";

    private static final long NOW = 1_735_689_600_000L;

    @Mock
    private MaintenanceWindowScheduleRepository scheduleRepository;

    @Mock
    private MaintenanceWindowScheduleValidator scheduleValidator;

    private final MaintenanceWindowScheduleScopeValidator scopeValidator = new MaintenanceWindowScheduleScopeValidator();

    @Mock
    private MaintenanceWindowSkipRepository skipRepository;

    @Mock
    private Clock clock;

    @Mock
    private MaintenanceWindowScheduleEligibilityService scheduleEligibilityService;

    @Mock
    private MaintenanceResourceEnvironmentCrnResolver resourceEnvironmentCrnResolver;

    private MaintenanceOccurrenceCalculator occurrenceCalculator;

    private MaintenanceWindowScheduleConverter scheduleConverter;

    private MaintenanceWindowScheduleService scheduleService;

    private MaintenanceWindowScheduleEligibilityService eligibilityService;

    @BeforeEach
    void setUp() {
        occurrenceCalculator = new MaintenanceOccurrenceCalculator();
        scheduleConverter = new MaintenanceWindowScheduleConverter(occurrenceCalculator, clock);
        scheduleService = new MaintenanceWindowScheduleService(
                scheduleRepository, scheduleValidator, scopeValidator, scheduleConverter,
                scheduleEligibilityService, resourceEnvironmentCrnResolver, clock);
        eligibilityService = new MaintenanceWindowScheduleEligibilityService(
                scheduleRepository, skipRepository, occurrenceCalculator, clock);
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
        verify(scheduleValidator).validate(any(MaintenanceWindowSchedule.class));
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
                new MaintenanceWindowScheduleValidator(occurrenceCalculator, clock),
                new MaintenanceWindowScheduleScopeValidator(),
                scheduleConverter,
                scheduleEligibilityService,
                resourceEnvironmentCrnResolver,
                clock);
        MaintenanceWindowScheduleRequest request = request("tenant-default");
        request.setScopeType(null);
        lenient().when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(any(), any(), any()))
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
        verify(scheduleValidator).validate(schedule);
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

    @Test
    void resolveEffectiveScheduleReturnsResolvedScheduleWithSource() {
        String resourceCrn = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";
        String environmentCrn = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";
        EffectiveScheduleLookupContext lookup = new EffectiveScheduleLookupContext(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, resourceCrn, environmentCrn);

        MaintenanceWindowSchedule schedule = entityFromRequest(request("resource-default"));
        schedule.setScopeType(MaintenanceScopeType.DATAHUB);
        schedule.setScopeId(resourceCrn);
        EffectiveScheduleResolution resolution = new EffectiveScheduleResolution(
                schedule, MaintenanceScopeType.DATAHUB, resourceCrn);

        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        when(scheduleEligibilityService.resolveEffectiveScheduleWithSource(any())).thenReturn(Optional.of(resolution));

        MaintenanceWindowEffectiveScheduleResponse response =
                scheduleService.toEffectiveResponse(scheduleService.resolveEffectiveSchedule(lookup));

        assertThat(response.getSourceScopeType()).isEqualTo(MaintenanceScopeType.DATAHUB.name());
        assertThat(response.getSourceScopeId()).isEqualTo(resourceCrn);
        assertThat(response.getSchedule().getScopeId()).isEqualTo(resourceCrn);
        verify(scheduleEligibilityService).resolveEffectiveScheduleWithSource(
                new MaintenanceWindowResourceIdentity(ACCOUNT_ID, environmentCrn, resourceCrn, MaintenanceScopeType.DATAHUB));
    }

    @Test
    void validateEffectiveScheduleResourceRejectsForeignAccountResourceCrn() {
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn("crn:cdp:datahub:us-west-1:other-acc:cluster:dh-1");

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> scheduleService.validateEffectiveScheduleResource(ACCOUNT_ID, params));

        assertThat(error.getMessage()).contains("scopeId must belong to the same account");
    }

    @Test
    void validateEffectiveScheduleResourceRejectsNonEnvironmentEnvironmentCrn() {
        String datahubCrn = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(datahubCrn);
        params.setEnvironmentCrn(datahubCrn);

        BadRequestException error = assertThrows(BadRequestException.class,
                () -> scheduleService.validateEffectiveScheduleResource(ACCOUNT_ID, params));

        assertThat(error.getMessage()).isEqualTo("environmentCrn must reference an environment resource.");
    }

    @Test
    void resolveEffectiveScheduleThrowsNotFoundWhenNoScheduleApplies() {
        String resourceCrn = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";
        EffectiveScheduleLookupContext lookup = new EffectiveScheduleLookupContext(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, resourceCrn, null);
        when(scheduleEligibilityService.resolveEffectiveScheduleWithSource(any())).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> scheduleService.resolveEffectiveSchedule(lookup));
    }

    @Test
    void completeEffectiveScheduleLookupUsesDerivedEnvironmentWhenQueryParamOmitted() {
        String resourceCrn = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";
        String environmentCrn = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(resourceCrn);
        EffectiveScheduleResourceContext resourceContext = scheduleService.validateEffectiveScheduleResource(ACCOUNT_ID, params);
        when(resourceEnvironmentCrnResolver.resolveOptional(ACCOUNT_ID, resourceCrn, MaintenanceScopeType.DATAHUB))
                .thenReturn(Optional.of(environmentCrn));

        EffectiveScheduleLookupContext lookup = scheduleService.completeEffectiveScheduleLookup(resourceContext, params);

        assertThat(lookup.environmentCrn()).isEqualTo(environmentCrn);
        assertThat(lookup.resourceCrn()).isEqualTo(resourceCrn);
    }

    @Test
    void completeEffectiveScheduleLookupSkipsEnvironmentWhenOwnerDoesNotKnowResource() {
        String resourceCrn = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(resourceCrn);
        EffectiveScheduleResourceContext resourceContext = scheduleService.validateEffectiveScheduleResource(ACCOUNT_ID, params);
        when(resourceEnvironmentCrnResolver.resolveOptional(ACCOUNT_ID, resourceCrn, MaintenanceScopeType.DATAHUB))
                .thenReturn(Optional.empty());

        EffectiveScheduleLookupContext lookup = scheduleService.completeEffectiveScheduleLookup(resourceContext, params);

        assertThat(lookup.environmentCrn()).isNull();
    }

    @Test
    void completeEffectiveScheduleLookupRejectsMismatchedEnvironmentCrn() {
        String resourceCrn = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(resourceCrn);
        params.setEnvironmentCrn("crn:cdp:environments:us-west-1:acc-1:environment:other-env");
        EffectiveScheduleResourceContext resourceContext = scheduleService.validateEffectiveScheduleResource(ACCOUNT_ID, params);
        when(resourceEnvironmentCrnResolver.resolveOptional(ACCOUNT_ID, resourceCrn, MaintenanceScopeType.DATAHUB))
                .thenReturn(Optional.of("crn:cdp:environments:us-west-1:acc-1:environment:env-1"));

        assertThrows(BadRequestException.class, () -> scheduleService.completeEffectiveScheduleLookup(resourceContext, params));
    }

    @Test
    void completeEffectiveScheduleLookupRejectsQueryEnvironmentWhenOwnerDoesNotKnowResource() {
        String resourceCrn = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(resourceCrn);
        params.setEnvironmentCrn("crn:cdp:environments:us-west-1:acc-1:environment:env-1");
        EffectiveScheduleResourceContext resourceContext = scheduleService.validateEffectiveScheduleResource(ACCOUNT_ID, params);
        when(resourceEnvironmentCrnResolver.resolveOptional(ACCOUNT_ID, resourceCrn, MaintenanceScopeType.DATAHUB))
            .thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () -> scheduleService.completeEffectiveScheduleLookup(resourceContext, params));
    }

    @Test
    void restoreReactivatesArchivedSchedulePreservingId() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowSchedule schedule = entityFromRequest(request("tenant-default"));
        schedule.setId(1L);
        schedule.setArchived(true);
        when(scheduleRepository.findByIdAndAccountId(1L, ACCOUNT_ID)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(MaintenanceWindowSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceWindowScheduleRestoreRequest request = restoreRequest(1L, "tenant-default");

        MaintenanceWindowScheduleResponse response = scheduleService.restore(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID, request, USER_CRN);

        // Same row id reused so maintenance_window_skip rows (FK to id 1) stay valid;
        // behaviourally exercised by restoredSchedulePreservesSkipEligibility
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(schedule.getId()).isEqualTo(1L);
        assertThat(schedule.isArchived()).isFalse();
        assertThat(schedule.getUpdatedAt()).isEqualTo(NOW);
        assertThat(schedule.getUpdatedBy()).isEqualTo(USER_CRN);
        verify(scheduleValidator).validate(schedule);
        verify(scheduleRepository).save(schedule);
        verify(scheduleRepository, never()).delete(any());
        // Reactivate path, not create-a-new-row path
        verify(scheduleRepository, never()).findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(any(), any(), any());
    }

    @Test
    void restoredSchedulePreservesSkipEligibility() {
        // Regression: a skip recorded before archival (FK to schedule id 1) must still suppress the matching
        // occurrence after restore, because restore reactivates the same row rather than creating a new id.
        // Had restore created a fresh row, eligibility would resolve a new id and the id-1 skip would be orphaned.
        long windowStart = Instant.parse("2025-01-06T09:00:00Z").toEpochMilli();
        long insideWindow = Instant.parse("2025-01-06T09:30:00Z").toEpochMilli();
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowSchedule schedule = entityFromRequest(request("tenant-default"));
        schedule.setId(1L);
        schedule.setArchived(true);
        when(scheduleRepository.findByIdAndAccountId(1L, ACCOUNT_ID)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(MaintenanceWindowSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        scheduleService.restore(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID, restoreRequest(1L, "tenant-default"), USER_CRN);

        // Eligibility resolves the reactivated row; the pre-existing skip is still keyed to its (preserved) id.
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID))
                .thenReturn(Optional.of(schedule));
        MaintenanceWindowSkip skip = new MaintenanceWindowSkip();
        skip.setMaintenanceWindowSchedule(schedule);
        skip.setWindowStart(windowStart);
        when(skipRepository.findByMaintenanceWindowScheduleIdAndWindowStart(1L, windowStart)).thenReturn(Optional.of(skip));

        MaintenanceWindowResourceIdentity identity = new MaintenanceWindowResourceIdentity(ACCOUNT_ID, null, null, null);
        MaintenanceWindowScheduleEligibility eligibility = eligibilityService.checkEligibility(identity, insideWindow);

        assertThat(eligibility.dispatchable()).isFalse();
        assertThat(eligibility.currentOccurrence()).isPresent();
        assertThat(eligibility.currentOccurrence().get().windowStart()).isEqualTo(windowStart);
        verify(skipRepository).findByMaintenanceWindowScheduleIdAndWindowStart(1L, windowStart);
    }

    @Test
    void restoreRestoresNullableDescriptionToNull() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        MaintenanceWindowSchedule schedule = entityFromRequest(request("tenant-default"));
        schedule.setId(1L);
        schedule.setDescription("edited-during-failed-edit");
        when(scheduleRepository.findByIdAndAccountId(1L, ACCOUNT_ID)).thenReturn(Optional.of(schedule));
        when(scheduleRepository.save(any(MaintenanceWindowSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceWindowScheduleRestoreRequest request = restoreRequest(1L, "tenant-default");
        request.setDescription(null);

        MaintenanceWindowScheduleResponse response = scheduleService.restore(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID, request, USER_CRN);

        // Full replace restores the prior null, unlike PATCH which would keep "edited-during-failed-edit"
        assertThat(schedule.getDescription()).isNull();
        assertThat(response.getDescription()).isNull();
    }

    @Test
    void restoreFallsBackToCreateWhenScheduleRowMissing() {
        when(clock.getCurrentTimeMillis()).thenReturn(NOW);
        when(scheduleRepository.findByIdAndAccountId(99L, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(scheduleRepository.findByAccountIdAndScopeTypeAndScopeIdAndArchivedFalse(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(MaintenanceWindowSchedule.class))).thenAnswer(invocation -> {
            MaintenanceWindowSchedule schedule = invocation.getArgument(0);
            schedule.setId(2L);
            return schedule;
        });

        MaintenanceWindowScheduleRestoreRequest request = restoreRequest(99L, "tenant-default");

        MaintenanceWindowScheduleResponse response = scheduleService.restore(
                ACCOUNT_ID, MaintenanceScopeType.TENANT, ACCOUNT_ID, request, USER_CRN);

        assertThat(response.getName()).isEqualTo("tenant-default");
        verify(scheduleRepository).findByIdAndAccountId(99L, ACCOUNT_ID);
        // Create path validates the merged schedule
        verify(scheduleValidator).validate(any(MaintenanceWindowSchedule.class));
    }

    private MaintenanceWindowScheduleRestoreRequest restoreRequest(Long id, String name) {
        MaintenanceWindowScheduleRestoreRequest request = new MaintenanceWindowScheduleRestoreRequest();
        request.setId(id);
        request.setName(name);
        request.setScopeType(MaintenanceScopeType.TENANT.name());
        request.setScopeId(ACCOUNT_ID);
        request.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        request.setTimezone("UTC");
        request.setDurationMinutes(120);
        request.setStartLocalTime("09:00");
        request.setDayOfWeek(DayOfWeek.MONDAY.name());
        return request;
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
