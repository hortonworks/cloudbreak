package com.sequenceiq.cloudbreak.maintenancewindow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.maintenance.api.internal.schedule.endpoint.MaintenanceWindowScheduleInternalEndpoint;
import com.sequenceiq.maintenance.api.model.MaintenanceScopeType;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleLookupServiceTest {

    private static final String ACCOUNT_ID = "acc-12345";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env-1";

    private static final String RESOURCE_CRN = "crn:cdp:datahub:us-west-1:acc-12345:cluster:dh-1";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc-12345:datalake:dl-1";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:acc-12345:freeipa:ipa-1";

    @Mock
    private MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint;

    private MockedStatic<ThreadBasedUserCrnProvider> userCrnProvider;

    private MaintenanceWindowScheduleLookupService underTest;

    @BeforeEach
    void setUp() {
        underTest = new MaintenanceWindowScheduleLookupService(scheduleInternalEndpoint);
        userCrnProvider = mockStatic(ThreadBasedUserCrnProvider.class);
        userCrnProvider.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class), eq(ACCOUNT_ID)))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
    }

    @AfterEach
    void tearDown() {
        userCrnProvider.close();
    }

    @Test
    void returnsTrueWhenDatahubScheduleExistsWithoutCheckingWeakerScopes() {
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), RESOURCE_CRN))
                .thenReturn(new MaintenanceWindowScheduleResponse());

        assertThat(underTest.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).isTrue();

        verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), RESOURCE_CRN);
        verify(scheduleInternalEndpoint, never()).get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);
        verify(scheduleInternalEndpoint, never()).get(ACCOUNT_ID, MaintenanceScopeType.TENANT.name(), ACCOUNT_ID);
    }

    @Test
    void returnsTrueWhenEnvironmentScheduleExistsAfterDatahubNotFound() {
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), RESOURCE_CRN))
                .thenThrow(notFound());
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenReturn(new MaintenanceWindowScheduleResponse());

        assertThat(underTest.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).isTrue();

        InOrder inOrder = inOrder(scheduleInternalEndpoint);
        inOrder.verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), RESOURCE_CRN);
        inOrder.verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);
        verify(scheduleInternalEndpoint, never()).get(ACCOUNT_ID, MaintenanceScopeType.TENANT.name(), ACCOUNT_ID);
    }

    @Test
    void returnsTrueWhenTenantScheduleExistsAfterDatahubAndEnvironmentNotFound() {
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), RESOURCE_CRN))
                .thenThrow(notFound());
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenThrow(notFound());
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.TENANT.name(), ACCOUNT_ID))
                .thenReturn(new MaintenanceWindowScheduleResponse());

        assertThat(underTest.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).isTrue();

        InOrder inOrder = inOrder(scheduleInternalEndpoint);
        inOrder.verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), RESOURCE_CRN);
        inOrder.verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);
        inOrder.verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.TENANT.name(), ACCOUNT_ID);
    }

    @Test
    void returnsFalseWhenAllScopesReturnNotFound() {
        when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.DATAHUB.name()), eq(RESOURCE_CRN)))
                .thenThrow(notFound());
        when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.ENVIRONMENT.name()), eq(ENV_CRN)))
                .thenThrow(notFound());
        when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.TENANT.name()), eq(ACCOUNT_ID)))
                .thenThrow(notFound());

        assertThat(underTest.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN)).isFalse();
    }

    @Test
    void propagatesNonNotFoundErrorsFromFirstScope() {
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), RESOURCE_CRN))
                .thenThrow(serviceUnavailable());

        assertThatThrownBy(() -> underTest.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN))
                .isInstanceOf(WebApplicationException.class);
        verify(scheduleInternalEndpoint, never()).get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);
    }

    @Test
    void propagatesNonNotFoundErrorsFromEnvironmentScope() {
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), RESOURCE_CRN))
                .thenThrow(notFound());
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenThrow(serviceUnavailable());

        assertThatThrownBy(() -> underTest.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN))
                .isInstanceOf(WebApplicationException.class);
        verify(scheduleInternalEndpoint, never()).get(ACCOUNT_ID, MaintenanceScopeType.TENANT.name(), ACCOUNT_ID);
    }

    @Test
    void usesInternalActorWithTenantAccountId() {
        when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.DATAHUB.name()), eq(RESOURCE_CRN)))
                .thenThrow(notFound());
        when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.ENVIRONMENT.name()), eq(ENV_CRN)))
                .thenThrow(notFound());
        when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.TENANT.name()), eq(ACCOUNT_ID)))
                .thenThrow(notFound());

        underTest.hasConfiguredSchedule(ACCOUNT_ID, RESOURCE_CRN, ENV_CRN);

        userCrnProvider.verify(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class), eq(ACCOUNT_ID)));
    }

    /**
     * The resource scope must be derived from the CRN, not pinned to DATAHUB: a Data Lake schedule is registered under
     * DATALAKE (see MaintenanceScopeTypes), so looking it up under DATAHUB would miss it and silently skip registration.
     */
    @Test
    void looksUpDatalakeResourceUnderDatalakeScope() {
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATALAKE.name(), DATALAKE_CRN))
                .thenReturn(new MaintenanceWindowScheduleResponse());

        assertThat(underTest.hasConfiguredSchedule(ACCOUNT_ID, DATALAKE_CRN, ENV_CRN)).isTrue();

        verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.DATALAKE.name(), DATALAKE_CRN);
        verify(scheduleInternalEndpoint, never()).get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), DATALAKE_CRN);
    }

    @Test
    void fallsBackToWeakerScopesWhenDatalakeResourceScheduleNotFound() {
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATALAKE.name(), DATALAKE_CRN))
                .thenThrow(notFound());
        when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                .thenReturn(new MaintenanceWindowScheduleResponse());

        assertThat(underTest.hasConfiguredSchedule(ACCOUNT_ID, DATALAKE_CRN, ENV_CRN)).isTrue();

        InOrder inOrder = inOrder(scheduleInternalEndpoint);
        inOrder.verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.DATALAKE.name(), DATALAKE_CRN);
        inOrder.verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);
    }

    @Test
    void rejectsFreeipaResourceCrnWhichCoreDoesNotOwn() {
        assertThatThrownBy(() -> underTest.hasConfiguredSchedule(ACCOUNT_ID, FREEIPA_CRN, ENV_CRN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Data Hub and Data Lake")
                .hasMessageContaining(MaintenanceScopeType.FREEIPA.name());

        verify(scheduleInternalEndpoint, never()).get(any(), any(), any());
    }

    @Test
    void rejectsResourceCrnThatIsNotACrn() {
        assertThatThrownBy(() -> underTest.hasConfiguredSchedule(ACCOUNT_ID, "not-a-crn", ENV_CRN))
                .isInstanceOf(BadRequestException.class);

        verify(scheduleInternalEndpoint, never()).get(any(), any(), any());
    }

    private static WebApplicationException notFound() {
        return new WebApplicationException(Response.status(404).build());
    }

    private static WebApplicationException serviceUnavailable() {
        return new WebApplicationException(Response.status(503).build());
    }
}
