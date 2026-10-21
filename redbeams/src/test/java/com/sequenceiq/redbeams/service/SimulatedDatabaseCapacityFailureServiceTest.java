package com.sequenceiq.redbeams.service;

import static com.sequenceiq.redbeams.api.model.common.SimulatedDatabaseCapacityFailureTag.ALL_ATTEMPTS;
import static com.sequenceiq.redbeams.api.model.common.SimulatedDatabaseCapacityFailureTag.KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;

@ExtendWith(MockitoExtension.class)
class SimulatedDatabaseCapacityFailureServiceTest {

    private static final String ACCOUNT_ID = "accountId";

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private SimulatedDatabaseCapacityFailureService underTest;

    @Test
    void testApplyIfRequestedWhenEntitledAndNumberThenParameterSet() {
        when(entitlementService.isSimulateDatabaseCapacityFailureEnabled(ACCOUNT_ID)).thenReturn(true);
        DatabaseStack databaseStack = databaseStack(Map.of(KEY, "1"));

        underTest.applyIfRequested(ACCOUNT_ID, databaseStack);

        assertThat(databaseStack.getDatabaseServer().getParameters()).containsEntry(DatabaseServer.SIMULATED_CAPACITY_FAILURES, 1);
        assertThat(databaseStack.getDatabaseServer().getSimulatedCapacityFailures()).isEqualTo(1);
    }

    @Test
    void testApplyIfRequestedWhenEntitledAndZeroThenParameterSetToZero() {
        when(entitlementService.isSimulateDatabaseCapacityFailureEnabled(ACCOUNT_ID)).thenReturn(true);
        DatabaseStack databaseStack = databaseStack(Map.of(KEY, "0"));

        underTest.applyIfRequested(ACCOUNT_ID, databaseStack);

        assertThat(databaseStack.getDatabaseServer().getParameters()).containsEntry(DatabaseServer.SIMULATED_CAPACITY_FAILURES, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"all", "ALL", "All", " all "})
    void testApplyIfRequestedWhenEntitledAndAllThenParameterSetToMax(String tagValue) {
        when(entitlementService.isSimulateDatabaseCapacityFailureEnabled(ACCOUNT_ID)).thenReturn(true);
        DatabaseStack databaseStack = databaseStack(Map.of(KEY, tagValue));

        underTest.applyIfRequested(ACCOUNT_ID, databaseStack);

        assertThat(databaseStack.getDatabaseServer().getParameters()).containsEntry(DatabaseServer.SIMULATED_CAPACITY_FAILURES, Integer.MAX_VALUE);
    }

    @Test
    void testApplyIfRequestedWhenEntitledAndPaddedNumberThenTrimmed() {
        when(entitlementService.isSimulateDatabaseCapacityFailureEnabled(ACCOUNT_ID)).thenReturn(true);
        DatabaseStack databaseStack = databaseStack(Map.of(KEY, " 2 "));

        underTest.applyIfRequested(ACCOUNT_ID, databaseStack);

        assertThat(databaseStack.getDatabaseServer().getParameters()).containsEntry(DatabaseServer.SIMULATED_CAPACITY_FAILURES, 2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", "1.5", "99999999999"})
    void testApplyIfRequestedWhenEntitledAndInvalidValueThenParameterNotSet(String tagValue) {
        when(entitlementService.isSimulateDatabaseCapacityFailureEnabled(ACCOUNT_ID)).thenReturn(true);
        DatabaseStack databaseStack = databaseStack(Map.of(KEY, tagValue));

        underTest.applyIfRequested(ACCOUNT_ID, databaseStack);

        assertThat(databaseStack.getDatabaseServer().getParameters()).doesNotContainKey(DatabaseServer.SIMULATED_CAPACITY_FAILURES);
        assertThat(databaseStack.getDatabaseServer().getSimulatedCapacityFailures()).isZero();
    }

    @Test
    void testApplyIfRequestedWhenNotEntitledThenParameterNotSet() {
        when(entitlementService.isSimulateDatabaseCapacityFailureEnabled(ACCOUNT_ID)).thenReturn(false);
        DatabaseStack databaseStack = databaseStack(Map.of(KEY, ALL_ATTEMPTS));

        underTest.applyIfRequested(ACCOUNT_ID, databaseStack);

        verify(entitlementService).isSimulateDatabaseCapacityFailureEnabled(ACCOUNT_ID);
        assertThat(databaseStack.getDatabaseServer().getParameters()).doesNotContainKey(DatabaseServer.SIMULATED_CAPACITY_FAILURES);
    }

    @Test
    void testApplyIfRequestedWhenNoTagThenEntitlementNotChecked() {
        DatabaseStack databaseStack = databaseStack(Map.of("otherTag", "1"));

        underTest.applyIfRequested(ACCOUNT_ID, databaseStack);

        verifyNoInteractions(entitlementService);
        assertThat(databaseStack.getDatabaseServer().getParameters()).doesNotContainKey(DatabaseServer.SIMULATED_CAPACITY_FAILURES);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  "})
    void testApplyIfRequestedWhenBlankTagThenEntitlementNotChecked(String tagValue) {
        DatabaseStack databaseStack = databaseStack(Map.of(KEY, tagValue));

        underTest.applyIfRequested(ACCOUNT_ID, databaseStack);

        verifyNoInteractions(entitlementService);
        assertThat(databaseStack.getDatabaseServer().getParameters()).doesNotContainKey(DatabaseServer.SIMULATED_CAPACITY_FAILURES);
    }

    @Test
    void testApplyIfRequestedWhenNullTagsThenEntitlementNotChecked() {
        DatabaseStack databaseStack = mock(DatabaseStack.class);
        when(databaseStack.getTags()).thenReturn(null);

        underTest.applyIfRequested(ACCOUNT_ID, databaseStack);

        verifyNoInteractions(entitlementService);
        verify(databaseStack, never()).getDatabaseServer();
    }

    private DatabaseStack databaseStack(Map<String, String> tags) {
        DatabaseServer databaseServer = DatabaseServer.builder()
                .withFlavor("db.m5.large")
                .build();
        return new DatabaseStack(null, databaseServer, tags, "");
    }
}
