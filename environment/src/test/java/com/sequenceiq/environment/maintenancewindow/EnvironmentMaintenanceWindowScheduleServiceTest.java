package com.sequenceiq.environment.maintenancewindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentMaintenanceWindowScheduleEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceRecurrenceKind;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.endpoint.MaintenanceWindowScheduleEndpoint;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRestoreRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.UpdateMaintenanceWindowScheduleRequest;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;

@ExtendWith(MockitoExtension.class)
class EnvironmentMaintenanceWindowScheduleServiceTest {

    private static final String ACCOUNT_ID = "acc-12345";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env-1";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:acc-12345:user:1";

    @Mock
    private MaintenanceWindowScheduleEndpoint maintenanceWindowScheduleEndpoint;

    @Mock
    private MaintenanceWindowScheduleInternalEndpoint maintenanceWindowScheduleInternalEndpoint;

    @InjectMocks
    private EnvironmentMaintenanceWindowScheduleService underTest;

    @Test
    void createEnvironmentScheduleIfPresentSkipsWhenRequestNull() {
        underTest.createEnvironmentScheduleIfPresent(ENV_CRN, null);

        verify(maintenanceWindowScheduleEndpoint, never()).create(any());
    }

    @Test
    void createEnvironmentScheduleIfPresentCallsMaintenanceCreate() {
        EnvironmentMaintenanceWindowScheduleRequest request = createScheduleRequest();
        when(maintenanceWindowScheduleEndpoint.create(any(MaintenanceWindowScheduleRequest.class)))
                .thenReturn(createdResponse());

        underTest.createEnvironmentScheduleIfPresent(ENV_CRN, request);

        verify(maintenanceWindowScheduleEndpoint).create(any(MaintenanceWindowScheduleRequest.class));
    }

    @Test
    void createEnvironmentScheduleIfPresentThrowsOnNonCreatedResponse() {
        EnvironmentMaintenanceWindowScheduleRequest request = createScheduleRequest();
        when(maintenanceWindowScheduleEndpoint.create(any(MaintenanceWindowScheduleRequest.class)))
                .thenReturn(errorResponse(Response.Status.FORBIDDEN.getStatusCode()));

        assertThatThrownBy(() -> underTest.createEnvironmentScheduleIfPresent(ENV_CRN, request))
                .isInstanceOf(WebApplicationException.class)
                .satisfies(ex -> assertThat(((WebApplicationException) ex).getResponse().getStatus())
                        .isEqualTo(Response.Status.FORBIDDEN.getStatusCode()));
    }

    @Test
    void applyEnvironmentScheduleOnEditUpdatesWhenScheduleExists() {
        when(maintenanceWindowScheduleEndpoint.get(MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenReturn(new MaintenanceWindowScheduleResponse());

        underTest.applyEnvironmentScheduleOnEdit(ENV_CRN, editScheduleRequest());

        verify(maintenanceWindowScheduleEndpoint).update(
                eq(MaintenanceScopeType.ENVIRONMENT.name()), eq(ENV_CRN), any(UpdateMaintenanceWindowScheduleRequest.class));
        verify(maintenanceWindowScheduleEndpoint, never()).create(any());
    }

    @Test
    void applyEnvironmentScheduleOnEditCreatesWhenScheduleMissing() {
        when(maintenanceWindowScheduleEndpoint.get(MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenThrow(notFound());
        when(maintenanceWindowScheduleEndpoint.create(any(MaintenanceWindowScheduleRequest.class)))
                .thenReturn(createdResponse());

        underTest.applyEnvironmentScheduleOnEdit(ENV_CRN, editScheduleRequest());

        verify(maintenanceWindowScheduleEndpoint).create(any(MaintenanceWindowScheduleRequest.class));
    }

    @Test
    void applyEnvironmentScheduleOnEditThrowsWhenCreateFallbackFails() {
        when(maintenanceWindowScheduleEndpoint.get(MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenThrow(notFound());
        when(maintenanceWindowScheduleEndpoint.create(any(MaintenanceWindowScheduleRequest.class)))
                .thenReturn(errorResponse(Response.Status.CONFLICT.getStatusCode()));

        assertThatThrownBy(() -> underTest.applyEnvironmentScheduleOnEdit(ENV_CRN, editScheduleRequest()))
                .isInstanceOf(WebApplicationException.class)
                .satisfies(ex -> assertThat(((WebApplicationException) ex).getResponse().getStatus())
                        .isEqualTo(Response.Status.CONFLICT.getStatusCode()));
    }

    @Test
    void clearEnvironmentScheduleIfPresentDeletesSchedule() {
        underTest.clearEnvironmentScheduleIfPresent(ENV_CRN);

        verify(maintenanceWindowScheduleEndpoint).delete(MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);
    }

    @Test
    void clearEnvironmentScheduleIfPresentIgnoresNotFound() {
        doThrow(notFound()).when(maintenanceWindowScheduleEndpoint).delete(MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);

        underTest.clearEnvironmentScheduleIfPresent(ENV_CRN);
    }

    @Test
    void getEnvironmentScheduleReturnsEmptyOnNotFound() {
        when(maintenanceWindowScheduleEndpoint.get(MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenThrow(notFound());

        assertThat(underTest.getEnvironmentSchedule(ENV_CRN)).isEmpty();
    }

    @Test
    void getEnvironmentScheduleReturnsScheduleWhenPresent() {
        MaintenanceWindowScheduleResponse schedule = new MaintenanceWindowScheduleResponse();
        schedule.setScopeId(ENV_CRN);
        when(maintenanceWindowScheduleEndpoint.get(MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN)).thenReturn(schedule);

        Optional<MaintenanceWindowScheduleResponse> result = underTest.getEnvironmentSchedule(ENV_CRN);

        assertThat(result).contains(schedule);
    }

    @Test
    void getEnvironmentSchedulePropagatesNonNotFoundErrors() {
        when(maintenanceWindowScheduleEndpoint.get(MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenThrow(new WebApplicationException(Response.status(503).build()));

        assertThatThrownBy(() -> underTest.getEnvironmentSchedule(ENV_CRN))
                .isInstanceOf(WebApplicationException.class);
    }

    @Test
    void getEnvironmentScheduleForDescribeUsesInternalEndpoint() {
        MaintenanceWindowScheduleResponse schedule = new MaintenanceWindowScheduleResponse();
        when(maintenanceWindowScheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenReturn(schedule);

        Optional<MaintenanceWindowScheduleResponse> result = underTest.getEnvironmentScheduleForDescribe(ACCOUNT_ID, ENV_CRN);

        assertThat(result).contains(schedule);
        verify(maintenanceWindowScheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);
        verify(maintenanceWindowScheduleEndpoint, never()).get(any(), any());
    }

    @Test
    void getEnvironmentScheduleForDescribeReturnsEmptyOnNotFound() {
        when(maintenanceWindowScheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenThrow(notFound());

        assertThat(underTest.getEnvironmentScheduleForDescribe(ACCOUNT_ID, ENV_CRN)).isEmpty();
    }

    @Test
    void getEnvironmentScheduleForDescribeDegradesOnServiceError() {
        when(maintenanceWindowScheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenThrow(new WebApplicationException(Response.status(503).build()));

        assertThat(underTest.getEnvironmentScheduleForDescribe(ACCOUNT_ID, ENV_CRN)).isEmpty();
    }

    @Test
    void restoreEnvironmentScheduleClearsWhenThereWasNoPriorSchedule() {
        underTest.restoreEnvironmentSchedule(ENV_CRN, Optional.empty());

        verify(maintenanceWindowScheduleEndpoint).delete(MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);
    }

    @Test
    void restoreEnvironmentScheduleRestoresPriorStateByIdUnderInternalActor() {
        MaintenanceWindowScheduleResponse prior = priorScheduleResponse();
        when(maintenanceWindowScheduleInternalEndpoint.restore(
                eq(ACCOUNT_ID), eq(USER_CRN), eq(MaintenanceScopeType.ENVIRONMENT.name()), eq(ENV_CRN),
                any(MaintenanceWindowScheduleRestoreRequest.class)))
                .thenReturn(new MaintenanceWindowScheduleResponse());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.restoreEnvironmentSchedule(ENV_CRN, Optional.of(prior)));

        ArgumentCaptor<MaintenanceWindowScheduleRestoreRequest> captor =
                ArgumentCaptor.forClass(MaintenanceWindowScheduleRestoreRequest.class);
        verify(maintenanceWindowScheduleInternalEndpoint).restore(
                eq(ACCOUNT_ID), eq(USER_CRN), eq(MaintenanceScopeType.ENVIRONMENT.name()), eq(ENV_CRN), captor.capture());
        MaintenanceWindowScheduleRestoreRequest request = captor.getValue();
        // Prior id is carried so the maintenance service reactivates the same row (skips preserved)
        assertThat(request.getId()).isEqualTo(42L);
        assertThat(request.getName()).isEqualTo("prior");
        assertThat(request.getDurationMinutes()).isEqualTo(60);
        assertThat(request.getScopeType()).isEqualTo(MaintenanceScopeType.ENVIRONMENT.name());
        assertThat(request.getScopeId()).isEqualTo(ENV_CRN);
        // No null-ignoring PATCH and no new-id create on the restore path
        verify(maintenanceWindowScheduleEndpoint, never()).update(any(), any(), any());
        verify(maintenanceWindowScheduleEndpoint, never()).create(any());
    }

    @Test
    void restoreEnvironmentScheduleRestoresNullableFieldToNull() {
        MaintenanceWindowScheduleResponse prior = priorScheduleResponse();
        prior.setDescription(null);
        when(maintenanceWindowScheduleInternalEndpoint.restore(
                eq(ACCOUNT_ID), eq(USER_CRN), eq(MaintenanceScopeType.ENVIRONMENT.name()), eq(ENV_CRN),
                any(MaintenanceWindowScheduleRestoreRequest.class)))
                .thenReturn(new MaintenanceWindowScheduleResponse());

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.restoreEnvironmentSchedule(ENV_CRN, Optional.of(prior)));

        ArgumentCaptor<MaintenanceWindowScheduleRestoreRequest> captor =
                ArgumentCaptor.forClass(MaintenanceWindowScheduleRestoreRequest.class);
        verify(maintenanceWindowScheduleInternalEndpoint).restore(
                eq(ACCOUNT_ID), eq(USER_CRN), eq(MaintenanceScopeType.ENVIRONMENT.name()), eq(ENV_CRN), captor.capture());
        assertThat(captor.getValue().getDescription()).isNull();
    }

    @Test
    void restoreEnvironmentScheduleDoesNotPropagateMaintenanceErrors() {
        MaintenanceWindowScheduleResponse prior = priorScheduleResponse();
        when(maintenanceWindowScheduleInternalEndpoint.restore(
                eq(ACCOUNT_ID), eq(USER_CRN), eq(MaintenanceScopeType.ENVIRONMENT.name()), eq(ENV_CRN),
                any(MaintenanceWindowScheduleRestoreRequest.class)))
                .thenThrow(new RuntimeException("maintenance unavailable"));

        assertThatCode(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.restoreEnvironmentSchedule(ENV_CRN, Optional.of(prior))))
                .doesNotThrowAnyException();
    }

    private static MaintenanceWindowScheduleResponse priorScheduleResponse() {
        MaintenanceWindowScheduleResponse prior = new MaintenanceWindowScheduleResponse();
        prior.setId(42L);
        prior.setAccountId(ACCOUNT_ID);
        prior.setName("prior");
        prior.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        prior.setDurationMinutes(60);
        prior.setStartLocalTime("01:00");
        prior.setDayOfWeek("SATURDAY");
        prior.setTimezone("UTC");
        return prior;
    }

    private static EnvironmentMaintenanceWindowScheduleRequest createScheduleRequest() {
        EnvironmentMaintenanceWindowScheduleRequest request = new EnvironmentMaintenanceWindowScheduleRequest();
        request.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        request.setDurationMinutes(120);
        request.setStartLocalTime("02:00");
        request.setDayOfWeek("SUNDAY");
        request.setTimezone("UTC");
        return request;
    }

    private static EnvironmentMaintenanceWindowScheduleEditRequest editScheduleRequest() {
        EnvironmentMaintenanceWindowScheduleEditRequest request = new EnvironmentMaintenanceWindowScheduleEditRequest();
        request.setRecurrenceKind(MaintenanceRecurrenceKind.WEEKLY.name());
        request.setDurationMinutes(120);
        request.setStartLocalTime("02:00");
        request.setDayOfWeek("SUNDAY");
        request.setTimezone("UTC");
        return request;
    }

    private static WebApplicationException notFound() {
        return new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
    }

    private static Response createdResponse() {
        return Response.status(Response.Status.CREATED).build();
    }

    private static Response errorResponse(int status) {
        return Response.status(status).build();
    }
}
