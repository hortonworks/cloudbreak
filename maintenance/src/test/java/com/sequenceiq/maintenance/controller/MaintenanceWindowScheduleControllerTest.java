package com.sequenceiq.maintenance.controller;

import static com.sequenceiq.maintenance.authorization.MaintenanceWindowScheduleAccessMode.READ;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.ws.rs.ForbiddenException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.cloudbreak.common.service.Clock;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowEffectiveScheduleParams;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowSchedulePreviewRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowEffectiveScheduleResponse;
import com.sequenceiq.maintenance.authorization.MaintenanceWindowScheduleAuthorizationService;
import com.sequenceiq.maintenance.authorization.MaintenanceWindowScheduleEntitlementService;
import com.sequenceiq.maintenance.domain.MaintenanceWindowSchedule;
import com.sequenceiq.maintenance.repository.MaintenanceWindowScheduleRepository;
import com.sequenceiq.maintenance.service.MaintenanceOccurrenceCalculator;
import com.sequenceiq.maintenance.service.MaintenanceResourceEnvironmentCrnResolver;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleConverter;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleEligibilityService;
import com.sequenceiq.maintenance.service.MaintenanceWindowSchedulePreviewService;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleScopeValidator;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleService;
import com.sequenceiq.maintenance.service.MaintenanceWindowScheduleValidator;
import com.sequenceiq.maintenance.service.MaintenanceWindowSkipService;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleLookupContext;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleResolution;
import com.sequenceiq.maintenance.service.model.EffectiveScheduleResourceContext;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleControllerTest {

    private static final String ACCOUNT_ID = "acc-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc-1:cluster:dh-1";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-1:environment:env-1";

    @Mock
    private MaintenanceWindowScheduleService scheduleService;

    @Mock
    private MaintenanceWindowScheduleRepository scheduleRepository;

    @Mock
    private MaintenanceWindowScheduleValidator scheduleValidator;

    @Mock
    private MaintenanceWindowScheduleEligibilityService scheduleEligibilityService;

    @Mock
    private MaintenanceResourceEnvironmentCrnResolver resourceEnvironmentCrnResolver;

    @Mock
    private Clock clock;

    @Mock
    private MaintenanceWindowSkipService skipService;

    @Mock
    private MaintenanceWindowSchedulePreviewService previewService;

    @Mock
    private MaintenanceWindowScheduleAuthorizationService scheduleAuthorization;

    @Mock
    private MaintenanceWindowScheduleEntitlementService scheduleEntitlementService;

    @InjectMocks
    private MaintenanceWindowScheduleController controller;

    private MaintenanceWindowScheduleService scheduleServiceWithRealValidation;

    @BeforeEach
    void setUpScheduleServiceWithRealValidation() {
        MaintenanceWindowScheduleConverter scheduleConverter =
                new MaintenanceWindowScheduleConverter(new MaintenanceOccurrenceCalculator(), clock);
        scheduleServiceWithRealValidation = new MaintenanceWindowScheduleService(
                scheduleRepository,
                scheduleValidator,
                new MaintenanceWindowScheduleScopeValidator(),
                scheduleConverter,
                scheduleEligibilityService,
                resourceEnvironmentCrnResolver,
                clock);
    }

    private MaintenanceWindowScheduleController controllerWithRealValidation() {
        return new MaintenanceWindowScheduleController(
                scheduleServiceWithRealValidation, skipService, previewService, scheduleAuthorization, scheduleEntitlementService);
    }

    @Test
    void getEffectiveRejectsForeignAccountResourceCrnBeforeAuthorization() {
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn("crn:cdp:datahub:us-west-1:other-acc:cluster:dh-1");
        MaintenanceWindowScheduleController testController = controllerWithRealValidation();

        try (var userCrn = org.mockito.Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrn.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);

            BadRequestException error = assertThrows(BadRequestException.class, () -> testController.getEffective(params));

            assertThat(error.getMessage()).contains("scopeId must belong to the same account");
            verify(scheduleAuthorization, never()).authorize(any(), any(), any());
        }
    }

    @Test
    void getEffectiveRejectsNonEnvironmentEnvironmentCrnBeforeAuthorization() {
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(DATAHUB_CRN);
        params.setEnvironmentCrn(DATAHUB_CRN);
        MaintenanceWindowScheduleController testController = controllerWithRealValidation();

        try (var userCrn = org.mockito.Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrn.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);

            BadRequestException error = assertThrows(BadRequestException.class, () -> testController.getEffective(params));

            assertThat(error.getMessage()).isEqualTo("environmentCrn must reference an environment resource.");
            verify(scheduleAuthorization, never()).authorize(any(), any(), any());
        }
    }

    @Test
    void getEffectiveReturnsNotFoundWhenNoScheduleApplies() {
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(DATAHUB_CRN);
        when(resourceEnvironmentCrnResolver.resolveOptional(ACCOUNT_ID, DATAHUB_CRN, MaintenanceScopeType.DATAHUB))
                .thenReturn(Optional.empty());
        when(scheduleEligibilityService.resolveEffectiveScheduleWithSource(any()))
                .thenReturn(Optional.empty());
        MaintenanceWindowScheduleController testController = controllerWithRealValidation();

        try (var userCrn = org.mockito.Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrn.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);

            assertThrows(NotFoundException.class, () -> testController.getEffective(params));

            verify(scheduleAuthorization).authorize(MaintenanceScopeType.DATAHUB, DATAHUB_CRN, READ);
        }
    }

    @Test
    void getEffectiveAuthorizesEnvironmentWhenWinningSourceIsEnvironment() {
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(DATAHUB_CRN);
        EffectiveScheduleResourceContext resourceContext = new EffectiveScheduleResourceContext(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, DATAHUB_CRN);
        EffectiveScheduleLookupContext lookup = new EffectiveScheduleLookupContext(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, DATAHUB_CRN, ENV_CRN);
        MaintenanceWindowSchedule envSchedule = new MaintenanceWindowSchedule();
        envSchedule.setScopeType(MaintenanceScopeType.ENVIRONMENT);
        envSchedule.setScopeId(ENV_CRN);
        EffectiveScheduleResolution resolution = new EffectiveScheduleResolution(
                envSchedule, MaintenanceScopeType.ENVIRONMENT, ENV_CRN);
        MaintenanceWindowEffectiveScheduleResponse response = new MaintenanceWindowEffectiveScheduleResponse();

        try (var userCrn = org.mockito.Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrn.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            when(scheduleService.validateEffectiveScheduleResource(ACCOUNT_ID, params)).thenReturn(resourceContext);
            when(scheduleService.completeEffectiveScheduleLookup(resourceContext, params)).thenReturn(lookup);
            when(scheduleService.resolveEffectiveSchedule(lookup)).thenReturn(resolution);
            when(scheduleService.toEffectiveResponse(resolution)).thenReturn(response);

            assertThat(controller.getEffective(params)).isSameAs(response);

            verify(scheduleAuthorization).authorize(MaintenanceScopeType.DATAHUB, DATAHUB_CRN, READ);
            verify(scheduleAuthorization).authorize(MaintenanceScopeType.ENVIRONMENT, ENV_CRN, READ);
            verify(scheduleService, times(1)).resolveEffectiveSchedule(lookup);
        }
    }

    @Test
    void getEffectiveSkipsEnvironmentAuthorizationWhenWinningSourceIsResource() {
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(DATAHUB_CRN);
        EffectiveScheduleResourceContext resourceContext = new EffectiveScheduleResourceContext(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, DATAHUB_CRN);
        EffectiveScheduleLookupContext lookup = new EffectiveScheduleLookupContext(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, DATAHUB_CRN, ENV_CRN);
        MaintenanceWindowSchedule resourceSchedule = new MaintenanceWindowSchedule();
        resourceSchedule.setScopeType(MaintenanceScopeType.DATAHUB);
        resourceSchedule.setScopeId(DATAHUB_CRN);
        EffectiveScheduleResolution resolution = new EffectiveScheduleResolution(
                resourceSchedule, MaintenanceScopeType.DATAHUB, DATAHUB_CRN);

        try (var userCrn = org.mockito.Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrn.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            when(scheduleService.validateEffectiveScheduleResource(ACCOUNT_ID, params)).thenReturn(resourceContext);
            when(scheduleService.completeEffectiveScheduleLookup(resourceContext, params)).thenReturn(lookup);
            when(scheduleService.resolveEffectiveSchedule(lookup)).thenReturn(resolution);
            when(scheduleService.toEffectiveResponse(resolution)).thenReturn(new MaintenanceWindowEffectiveScheduleResponse());

            controller.getEffective(params);

            verify(scheduleAuthorization).authorize(MaintenanceScopeType.DATAHUB, DATAHUB_CRN, READ);
            verify(scheduleAuthorization, never()).authorize(MaintenanceScopeType.ENVIRONMENT, ENV_CRN, READ);
            verify(scheduleService, times(1)).resolveEffectiveSchedule(lookup);
        }
    }

    @Test
    void getEffectiveAllowsTenantWinningSourceWithResourceReadOnly() {
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(DATAHUB_CRN);
        EffectiveScheduleResourceContext resourceContext = new EffectiveScheduleResourceContext(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, DATAHUB_CRN);
        EffectiveScheduleLookupContext lookup = new EffectiveScheduleLookupContext(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, DATAHUB_CRN, ENV_CRN);
        MaintenanceWindowSchedule tenantSchedule = new MaintenanceWindowSchedule();
        tenantSchedule.setScopeType(MaintenanceScopeType.TENANT);
        tenantSchedule.setScopeId(ACCOUNT_ID);
        EffectiveScheduleResolution resolution = new EffectiveScheduleResolution(
                tenantSchedule, MaintenanceScopeType.TENANT, ACCOUNT_ID);
        MaintenanceWindowEffectiveScheduleResponse response = new MaintenanceWindowEffectiveScheduleResponse();

        try (var userCrn = org.mockito.Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrn.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            when(scheduleService.validateEffectiveScheduleResource(ACCOUNT_ID, params)).thenReturn(resourceContext);
            when(scheduleService.completeEffectiveScheduleLookup(resourceContext, params)).thenReturn(lookup);
            when(scheduleService.resolveEffectiveSchedule(lookup)).thenReturn(resolution);
            when(scheduleService.toEffectiveResponse(resolution)).thenReturn(response);

            assertThat(controller.getEffective(params)).isSameAs(response);

            verify(scheduleAuthorization).authorize(MaintenanceScopeType.DATAHUB, DATAHUB_CRN, READ);
            verify(scheduleAuthorization, never()).authorize(MaintenanceScopeType.TENANT, ACCOUNT_ID, READ);
        }
    }

    @Test
    void getEffectiveDeniedWhenCallerLacksResourceReadEvenIfTenantScheduleWins() {
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(DATAHUB_CRN);
        EffectiveScheduleResourceContext resourceContext = new EffectiveScheduleResourceContext(
                ACCOUNT_ID, MaintenanceScopeType.DATAHUB, DATAHUB_CRN);
        doThrow(new ForbiddenException("no datahub read"))
                .when(scheduleAuthorization)
                .authorize(MaintenanceScopeType.DATAHUB, DATAHUB_CRN, READ);

        try (var userCrn = org.mockito.Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrn.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);
            when(scheduleService.validateEffectiveScheduleResource(ACCOUNT_ID, params)).thenReturn(resourceContext);

            assertThrows(ForbiddenException.class, () -> controller.getEffective(params));

            verify(scheduleAuthorization).authorize(MaintenanceScopeType.DATAHUB, DATAHUB_CRN, READ);
            verify(scheduleAuthorization, never()).authorize(MaintenanceScopeType.TENANT, ACCOUNT_ID, READ);
            verify(scheduleService, never()).resolveEffectiveSchedule(any());
        }
    }

    @Test
    void getEffectiveDeniedWhenEntitlementMissing() {
        MaintenanceWindowEffectiveScheduleParams params = new MaintenanceWindowEffectiveScheduleParams();
        params.setResourceCrn(DATAHUB_CRN);
        doThrow(new ForbiddenException("CDP_MAINTENANCE_WINDOW missing"))
                .when(scheduleEntitlementService).verifyEntitled(ACCOUNT_ID);

        try (var userCrn = org.mockito.Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrn.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);

            assertThrows(ForbiddenException.class, () -> controller.getEffective(params));

            // Entitlement is enforced before any IAM check or resource resolution, matching every other public operation.
            verify(scheduleEntitlementService).verifyEntitled(ACCOUNT_ID);
            verify(scheduleService, never()).validateEffectiveScheduleResource(any(), any());
            verify(scheduleAuthorization, never()).authorize(any(), any(), any());
        }
    }

    @Test
    void previewDeniedWhenEntitlementMissing() {
        MaintenanceWindowSchedulePreviewRequest request = new MaintenanceWindowSchedulePreviewRequest();
        doThrow(new ForbiddenException("CDP_MAINTENANCE_WINDOW missing"))
                .when(scheduleEntitlementService).verifyEntitled(ACCOUNT_ID);

        try (var userCrn = org.mockito.Mockito.mockStatic(ThreadBasedUserCrnProvider.class)) {
            userCrn.when(ThreadBasedUserCrnProvider::getAccountId).thenReturn(ACCOUNT_ID);

            assertThrows(ForbiddenException.class, () -> controller.preview(request));

            // Entitlement is enforced before any IAM check or preview computation, matching every other public operation.
            verify(scheduleEntitlementService).verifyEntitled(ACCOUNT_ID);
            verify(scheduleAuthorization, never()).authorize(any(), any(), any());
            verify(previewService, never()).preview(any(), any());
        }
    }
}
