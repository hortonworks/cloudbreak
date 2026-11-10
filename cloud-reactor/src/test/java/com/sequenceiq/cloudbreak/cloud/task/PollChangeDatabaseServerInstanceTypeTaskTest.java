package com.sequenceiq.cloudbreak.cloud.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.ResourceConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseServer;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.cloudbreak.cloud.model.database.DatabaseServerInstanceTypeState;

@ExtendWith(MockitoExtension.class)
class PollChangeDatabaseServerInstanceTypeTaskTest {

    private static final String TARGET_INSTANCE_TYPE = "db.m5.xlarge";

    private final DatabaseStack databaseStack = new DatabaseStack(null, DatabaseServer.builder().withServerId("dbserver").build(), Map.of(), "");

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceConnector resourceConnector;

    private PollChangeDatabaseServerInstanceTypeTask underTest;

    @BeforeEach
    void setUp() {
        underTest = new PollChangeDatabaseServerInstanceTypeTask(authenticatedContext, databaseStack, TARGET_INSTANCE_TYPE, resourceConnector);
    }

    @Test
    void testCallReturnsTheLiveState() throws Exception {
        DatabaseServerInstanceTypeState state = new DatabaseServerInstanceTypeState(TARGET_INSTANCE_TYPE, 100L, true, false);
        when(resourceConnector.getDatabaseServerInstanceTypeState(authenticatedContext, databaseStack)).thenReturn(state);

        assertSame(state, underTest.call());
    }

    @Test
    void testCallWrapsTheProviderError() throws Exception {
        Exception exception = new Exception("throttled");
        when(resourceConnector.getDatabaseServerInstanceTypeState(authenticatedContext, databaseStack)).thenThrow(exception);

        CloudConnectorException result = assertThrows(CloudConnectorException.class, () -> underTest.call());

        assertEquals("throttled", result.getMessage());
        assertSame(exception, result.getCause());
    }

    @ParameterizedTest
    @CsvSource({"db.m5.xlarge, false, true", "DB.M5.XLARGE, false, true", "db.m5.xlarge, true, false", "db.m5.large, false, false",
            "db.m5.large, true, false"})
    void testCompletedOnlyWhenTheTargetIsAppliedAndNoModificationIsInProgress(String instanceType, boolean modificationInProgress,
            boolean expectedCompleted) {
        DatabaseServerInstanceTypeState state = new DatabaseServerInstanceTypeState(instanceType, 100L, false, modificationInProgress);

        assertEquals(expectedCompleted, underTest.completed(state));
    }
}
