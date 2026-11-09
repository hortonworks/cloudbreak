package com.sequenceiq.maintenance.client.schedule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.common.exception.WebApplicationExceptionMessageExtractor;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.request.MaintenanceWindowScheduleRequest;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleProvisionerTest {

    private static final MaintenanceScopeType SCOPE_TYPE = MaintenanceScopeType.DATAHUB;

    private static final String ACCOUNT_ID = "acc-1";

    private static final String SCOPE_ID = "crn:cdp:datahub:us-west-1:" + ACCOUNT_ID + ":cluster:dh-1";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + ACCOUNT_ID + ":user:1";

    @Mock
    private MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint;

    @Mock
    private WebApplicationExceptionMessageExtractor exceptionMessageExtractor;

    private MaintenanceWindowScheduleProvisioner underTest;

    @BeforeEach
    void setUp() {
        underTest = new MaintenanceWindowScheduleProvisioner(scheduleInternalEndpoint, SCOPE_TYPE, exceptionMessageExtractor);
    }

    @Test
    void createIfPresentDoesNothingWhenRequestIsNull() {
        underTest.createIfPresent(SCOPE_ID, null);

        verifyNoInteractions(scheduleInternalEndpoint);
    }

    @Test
    void createIfPresentCreatesScopedScheduleFromFieldsWithAccountFromCrnAndInitiatorUser() {
        MaintenanceWindowScheduleRequest source = new MaintenanceWindowScheduleRequest();
        source.setName("nightly");
        source.setRecurrenceKind("MONTHLY_DAY_OF_MONTH");
        source.setTimezone("UTC");
        source.setDayOfMonth(31);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createIfPresent(SCOPE_ID, source));

        ArgumentCaptor<MaintenanceWindowScheduleRequest> captor = ArgumentCaptor.forClass(MaintenanceWindowScheduleRequest.class);
        verify(scheduleInternalEndpoint).create(eq(ACCOUNT_ID), eq(USER_CRN), captor.capture());
        MaintenanceWindowScheduleRequest created = captor.getValue();
        assertThat(created.getScopeType()).isEqualTo(SCOPE_TYPE.name());
        assertThat(created.getScopeId()).isEqualTo(SCOPE_ID);
        assertThat(created.getName()).isEqualTo("nightly");
        assertThat(created.getRecurrenceKind()).isEqualTo("MONTHLY_DAY_OF_MONTH");
        assertThat(created.getTimezone()).isEqualTo("UTC");
        assertThat(created.getDayOfMonth()).isEqualTo(31);
    }

    @Test
    void createIfPresentTranslatesClientErrorToBadRequestWithServiceMessage() {
        MaintenanceWindowScheduleRequest source = new MaintenanceWindowScheduleRequest();
        WebApplicationException rejected = new WebApplicationException(Response.status(Response.Status.BAD_REQUEST).build());
        when(scheduleInternalEndpoint.create(eq(ACCOUNT_ID), eq(USER_CRN), any())).thenThrow(rejected);
        when(exceptionMessageExtractor.getErrorMessage(rejected))
                .thenReturn("Schedule must have at least one occurrence within the next 90 days");

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createIfPresent(SCOPE_ID, source)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Schedule must have at least one occurrence within the next 90 days")
                .hasCause(rejected);
    }

    @Test
    void createIfPresentRethrowsServerErrorWebApplicationExceptionUntranslated() {
        MaintenanceWindowScheduleRequest source = new MaintenanceWindowScheduleRequest();
        WebApplicationException serverError = new WebApplicationException(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
        when(scheduleInternalEndpoint.create(eq(ACCOUNT_ID), eq(USER_CRN), any())).thenThrow(serverError);

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.createIfPresent(SCOPE_ID, source)))
                .isSameAs(serverError);
        verifyNoInteractions(exceptionMessageExtractor);
    }

    @Test
    void clearIfPresentDeletesScheduleForScopeWithAccountFromCrnAndInitiatorUser() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.clearIfPresent(SCOPE_ID));

        verify(scheduleInternalEndpoint).delete(ACCOUNT_ID, USER_CRN, SCOPE_TYPE.name(), SCOPE_ID);
    }

    @Test
    void clearIfPresentSwallowsNotFound() {
        doThrow(notFound()).when(scheduleInternalEndpoint).delete(ACCOUNT_ID, USER_CRN, SCOPE_TYPE.name(), SCOPE_ID);

        assertThatCode(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.clearIfPresent(SCOPE_ID)))
                .doesNotThrowAnyException();
    }

    @Test
    void clearIfPresentSwallowsNotFoundNestedInCauseChain() {
        doThrow(new IllegalStateException("wrapped", notFound()))
                .when(scheduleInternalEndpoint).delete(ACCOUNT_ID, USER_CRN, SCOPE_TYPE.name(), SCOPE_ID);

        assertThatCode(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.clearIfPresent(SCOPE_ID)))
                .doesNotThrowAnyException();
    }

    @Test
    void clearIfPresentRethrowsNonNotFoundWebApplicationException() {
        WebApplicationException serviceUnavailable = new WebApplicationException(Response.status(503).build());
        doThrow(serviceUnavailable).when(scheduleInternalEndpoint).delete(ACCOUNT_ID, USER_CRN, SCOPE_TYPE.name(), SCOPE_ID);

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.clearIfPresent(SCOPE_ID)))
                .isSameAs(serviceUnavailable);
    }

    @Test
    void clearIfPresentRethrowsRuntimeExceptionWithoutWebApplicationException() {
        RuntimeException failure = new IllegalStateException("boom");
        doThrow(failure).when(scheduleInternalEndpoint).delete(ACCOUNT_ID, USER_CRN, SCOPE_TYPE.name(), SCOPE_ID);

        assertThatThrownBy(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.clearIfPresent(SCOPE_ID)))
                .isSameAs(failure);
    }

    @Test
    void clearQuietlyDeletesScheduleForScope() {
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.clearQuietly(SCOPE_ID));

        verify(scheduleInternalEndpoint).delete(ACCOUNT_ID, USER_CRN, SCOPE_TYPE.name(), SCOPE_ID);
    }

    @Test
    void clearQuietlySwallowsRuntimeException() {
        doThrow(new IllegalStateException("maintenance service down"))
                .when(scheduleInternalEndpoint).delete(ACCOUNT_ID, USER_CRN, SCOPE_TYPE.name(), SCOPE_ID);

        assertThatCode(() -> ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.clearQuietly(SCOPE_ID)))
                .doesNotThrowAnyException();
    }

    private static WebApplicationException notFound() {
        return new WebApplicationException(Response.status(Response.Status.NOT_FOUND).build());
    }
}
