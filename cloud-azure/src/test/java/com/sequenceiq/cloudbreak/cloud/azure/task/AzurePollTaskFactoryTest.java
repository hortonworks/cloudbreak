package com.sequenceiq.cloudbreak.cloud.azure.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationCheckerTask;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@ExtendWith(MockitoExtension.class)
class AzurePollTaskFactoryTest {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String DISK_ENCRYPTION_SET_NAME = "diskEncryptionSetName";

    @Mock
    private ApplicationContext applicationContext;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @InjectMocks
    private AzurePollTaskFactory underTest;

    @Test
    void diskEncryptionSetCreationCheckerTaskTest() {
        DiskEncryptionSetCreationCheckerContext checkerContext = new DiskEncryptionSetCreationCheckerContext(RESOURCE_GROUP_NAME, DISK_ENCRYPTION_SET_NAME);
        PollTask<DiskEncryptionSetInner> checkerTask = mock(PollTask.class);
        when(applicationContext.getBean(DiskEncryptionSetCreationCheckerTask.NAME, authenticatedContext, checkerContext)).thenReturn(checkerTask);

        PollTask<DiskEncryptionSetInner> result = underTest.diskEncryptionSetCreationCheckerTask(authenticatedContext, checkerContext);

        assertThat(result).isSameAs(checkerTask);
    }

}