package com.sequenceiq.maintenance.client.schedule;

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
import org.junit.jupiter.api.Nested;
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
import com.sequenceiq.maintenance.api.model.MaintenanceSubmitterService;
import com.sequenceiq.maintenance.api.v1.schedule.model.response.MaintenanceWindowScheduleResponse;

@ExtendWith(MockitoExtension.class)
class MaintenanceWindowScheduleLookupTest {

    private static final String ACCOUNT_ID = "acc-12345";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:acc-12345:environment:env-1";

    private static final String DATAHUB_CRN = "crn:cdp:datahub:us-west-1:acc-12345:cluster:dh-1";

    private static final String DATALAKE_CRN = "crn:cdp:datalake:us-west-1:acc-12345:datalake:dl-1";

    private static final String FREEIPA_CRN = "crn:cdp:freeipa:us-west-1:acc-12345:freeipa:ipa-1";

    @Mock
    private MaintenanceWindowScheduleInternalEndpoint scheduleInternalEndpoint;

    private MockedStatic<ThreadBasedUserCrnProvider> userCrnProvider;

    @BeforeEach
    void setUp() {
        userCrnProvider = mockStatic(ThreadBasedUserCrnProvider.class);
        userCrnProvider.when(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class), eq(ACCOUNT_ID)))
                .thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
    }

    @AfterEach
    void tearDown() {
        userCrnProvider.close();
    }

    private static WebApplicationException notFound() {
        return new WebApplicationException(Response.status(404).build());
    }

    private static WebApplicationException serviceUnavailable() {
        return new WebApplicationException(Response.status(503).build());
    }

    @Nested
    class CloudbreakSubmitter {

        private MaintenanceWindowScheduleLookup underTest;

        @BeforeEach
        void initLookup() {
            underTest = new MaintenanceWindowScheduleLookup(scheduleInternalEndpoint, MaintenanceSubmitterService.CLOUDBREAK);
        }

        @Test
        void returnsTrueWhenDatahubScheduleExistsWithoutCheckingWeakerScopes() {
            when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), DATAHUB_CRN))
                    .thenReturn(new MaintenanceWindowScheduleResponse());

            assertThat(underTest.hasConfiguredSchedule(ACCOUNT_ID, DATAHUB_CRN, ENV_CRN)).isTrue();

            verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), DATAHUB_CRN);
            verify(scheduleInternalEndpoint, never()).get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);
        }

        /**
         * Data Lake resources are registered by the datalake submitter. Rejecting them here keeps a Data Lake CRN from
         * getting SECRET_ROTATION tasks from both services.
         */
        @Test
        void rejectsDatalakeResourceCrn() {
            assertThatThrownBy(() -> underTest.hasConfiguredSchedule(ACCOUNT_ID, DATALAKE_CRN, ENV_CRN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(MaintenanceScopeType.DATALAKE.name());

            verify(scheduleInternalEndpoint, never()).get(any(), any(), any());
        }

        @Test
        void returnsTrueWhenTenantScheduleExistsAfterStrongerScopesNotFound() {
            when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), DATAHUB_CRN))
                    .thenThrow(notFound());
            when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN))
                    .thenThrow(notFound());
            when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.TENANT.name(), ACCOUNT_ID))
                    .thenReturn(new MaintenanceWindowScheduleResponse());

            assertThat(underTest.hasConfiguredSchedule(ACCOUNT_ID, DATAHUB_CRN, ENV_CRN)).isTrue();
        }

        @Test
        void rejectsFreeipaResourceCrn() {
            assertThatThrownBy(() -> underTest.hasConfiguredSchedule(ACCOUNT_ID, FREEIPA_CRN, ENV_CRN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("cloudbreak")
                    .hasMessageContaining(MaintenanceScopeType.FREEIPA.name());

            verify(scheduleInternalEndpoint, never()).get(any(), any(), any());
        }
    }

    @Nested
    class DatalakeSubmitter {

        private MaintenanceWindowScheduleLookup underTest;

        @BeforeEach
        void initLookup() {
            underTest = new MaintenanceWindowScheduleLookup(scheduleInternalEndpoint, MaintenanceSubmitterService.DATALAKE);
        }

        @Test
        void returnsTrueWhenDatalakeScheduleExistsWithoutCheckingWeakerScopes() {
            when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATALAKE.name(), DATALAKE_CRN))
                    .thenReturn(new MaintenanceWindowScheduleResponse());

            assertThat(underTest.hasConfiguredSchedule(ACCOUNT_ID, DATALAKE_CRN, ENV_CRN)).isTrue();

            verify(scheduleInternalEndpoint).get(ACCOUNT_ID, MaintenanceScopeType.DATALAKE.name(), DATALAKE_CRN);
            verify(scheduleInternalEndpoint, never()).get(ACCOUNT_ID, MaintenanceScopeType.ENVIRONMENT.name(), ENV_CRN);
        }

        @Test
        void fallsBackToEnvironmentWhenDatalakeResourceScheduleNotFound() {
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
        void rejectsDatahubResourceCrn() {
            assertThatThrownBy(() -> underTest.hasConfiguredSchedule(ACCOUNT_ID, DATAHUB_CRN, ENV_CRN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("datalake")
                    .hasMessageContaining(MaintenanceScopeType.DATAHUB.name());

            verify(scheduleInternalEndpoint, never()).get(any(), any(), any());
        }
    }

    @Nested
    class SharedBehavior {

        private MaintenanceWindowScheduleLookup underTest;

        @BeforeEach
        void initLookup() {
            underTest = new MaintenanceWindowScheduleLookup(scheduleInternalEndpoint, MaintenanceSubmitterService.CLOUDBREAK);
        }

        @Test
        void returnsFalseWhenAllScopesReturnNotFound() {
            when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.DATAHUB.name()), eq(DATAHUB_CRN)))
                    .thenThrow(notFound());
            when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.ENVIRONMENT.name()), eq(ENV_CRN)))
                    .thenThrow(notFound());
            when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.TENANT.name()), eq(ACCOUNT_ID)))
                    .thenThrow(notFound());

            assertThat(underTest.hasConfiguredSchedule(ACCOUNT_ID, DATAHUB_CRN, ENV_CRN)).isFalse();
        }

        @Test
        void propagatesNonNotFoundErrorsFromFirstScope() {
            when(scheduleInternalEndpoint.get(ACCOUNT_ID, MaintenanceScopeType.DATAHUB.name(), DATAHUB_CRN))
                    .thenThrow(serviceUnavailable());

            assertThatThrownBy(() -> underTest.hasConfiguredSchedule(ACCOUNT_ID, DATAHUB_CRN, ENV_CRN))
                    .isInstanceOf(WebApplicationException.class);
        }

        @Test
        void usesInternalActorWithTenantAccountId() {
            when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.DATAHUB.name()), eq(DATAHUB_CRN)))
                    .thenThrow(notFound());
            when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.ENVIRONMENT.name()), eq(ENV_CRN)))
                    .thenThrow(notFound());
            when(scheduleInternalEndpoint.get(eq(ACCOUNT_ID), eq(MaintenanceScopeType.TENANT.name()), eq(ACCOUNT_ID)))
                    .thenThrow(notFound());

            underTest.hasConfiguredSchedule(ACCOUNT_ID, DATAHUB_CRN, ENV_CRN);

            userCrnProvider.verify(() -> ThreadBasedUserCrnProvider.doAsInternalActor(any(Supplier.class), eq(ACCOUNT_ID)));
        }

        @Test
        void rejectsResourceCrnThatIsNotACrn() {
            assertThatThrownBy(() -> underTest.hasConfiguredSchedule(ACCOUNT_ID, "not-a-crn", ENV_CRN))
                    .isInstanceOf(BadRequestException.class);

            verify(scheduleInternalEndpoint, never()).get(any(), any(), any());
        }
    }
}
