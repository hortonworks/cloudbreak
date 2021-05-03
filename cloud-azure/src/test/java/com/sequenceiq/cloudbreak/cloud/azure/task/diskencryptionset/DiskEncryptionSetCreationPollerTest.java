package com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@ExtendWith(MockitoExtension.class)
class DiskEncryptionSetCreationPollerTest {

    private static final int CREATION_CHECK_INTERVAL = 12;

    private static final int CREATION_CHECK_MAX_ATTEMPT = 34;

    private static final int MAX_TOLERABLE_FAILURE_NUMBER = 56;

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String DISK_ENCRYPTION_SET_NAME = "diskEncryptionSetName";

    @Mock
    private AzurePollTaskFactory azurePollTaskFactory;

    @Mock
    private PollTask<DiskEncryptionSetInner> checkerTask;

    @Mock
    private SyncPollingScheduler<DiskEncryptionSetInner> syncPollingScheduler;

    @Mock
    private AzureUtils azureUtils;

    @InjectMocks
    private DiskEncryptionSetCreationPoller underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private DiskEncryptionSetInner des;

    private DiskEncryptionSetCreationCheckerContext checkerContext;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(underTest, "creationCheckInterval", CREATION_CHECK_INTERVAL);
        ReflectionTestUtils.setField(underTest, "creationCheckMaxAttempt", CREATION_CHECK_MAX_ATTEMPT);
        ReflectionTestUtils.setField(underTest, "maxTolerableFailureNumber", MAX_TOLERABLE_FAILURE_NUMBER);

        checkerContext = new DiskEncryptionSetCreationCheckerContext(RESOURCE_GROUP_NAME, DISK_ENCRYPTION_SET_NAME);
    }

    @Test
    void startPollingTestWhenGeneralException() {
        RuntimeException e = new RuntimeException();
        CloudConnectorException eWrapped = new CloudConnectorException(e);
        when(azureUtils.convertToCloudConnectorException(e, "Disk Encryption Set creation")).thenReturn(eWrapped);

        when(azurePollTaskFactory.diskEncryptionSetCreationCheckerTask(authenticatedContext, checkerContext)).thenThrow(e);

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.startPolling(authenticatedContext, checkerContext, null));

        assertThat(cloudConnectorException).hasCauseReference(e);
    }

    @Test
    void startPollingTestWhenNullCheckerContext() {
        startPollingTestWhenNpeInternal(authenticatedContext, null);
    }

    private void startPollingTestWhenNpeInternal(AuthenticatedContext authenticatedContext1, DiskEncryptionSetCreationCheckerContext checkerContext) {
        when(azureUtils.convertToCloudConnectorException(any(Exception.class), eq("Disk Encryption Set creation")))
                .thenAnswer(invocation -> new CloudConnectorException(invocation.getArgument(0, Exception.class)));

        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class,
                () -> underTest.startPolling(authenticatedContext1, checkerContext, null));

        assertThat(cloudConnectorException).hasCauseInstanceOf(NullPointerException.class);
    }

    @Test
    void startPollingTestWhenNullAuthenticatedContext() {
        startPollingTestWhenNpeInternal(null, checkerContext);
    }

    @Test
    void startPollingTestWhenAlreadyCompleted() throws Exception {
        when(azurePollTaskFactory.diskEncryptionSetCreationCheckerTask(authenticatedContext, checkerContext)).thenReturn(checkerTask);
        when(checkerTask.completed(des)).thenReturn(true);

        DiskEncryptionSetInner result = underTest.startPolling(authenticatedContext, checkerContext, des);

        assertThat(result).isSameAs(des);
        verify(syncPollingScheduler, never()).schedule(checkerTask, CREATION_CHECK_INTERVAL, CREATION_CHECK_MAX_ATTEMPT, MAX_TOLERABLE_FAILURE_NUMBER);
    }

    @Test
    void startPollingTestWhenScheduling() throws Exception {
        when(azurePollTaskFactory.diskEncryptionSetCreationCheckerTask(authenticatedContext, checkerContext)).thenReturn(checkerTask);
        when(checkerTask.completed(des)).thenReturn(false);

        DiskEncryptionSetInner desScheduled = mock(DiskEncryptionSetInner.class);
        when(syncPollingScheduler.schedule(checkerTask, CREATION_CHECK_INTERVAL, CREATION_CHECK_MAX_ATTEMPT, MAX_TOLERABLE_FAILURE_NUMBER))
                .thenReturn(desScheduled);

        DiskEncryptionSetInner result = underTest.startPolling(authenticatedContext, checkerContext, des);

        assertThat(result).isSameAs(desScheduled);
    }

}