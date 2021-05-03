package com.sequenceiq.cloudbreak.cloud.azure.task;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.microsoft.azure.management.compute.implementation.DiskEncryptionSetInner;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationCheckerContext;
import com.sequenceiq.cloudbreak.cloud.azure.task.diskencryptionset.DiskEncryptionSetCreationCheckerTask;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.task.PollTask;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = AzurePollTaskFactoryIntegrationTest.TestAppContext.class)
class AzurePollTaskFactoryIntegrationTest {

    private static final String RESOURCE_GROUP_NAME = "resourceGroupName";

    private static final String DISK_ENCRYPTION_SET_NAME = "diskEncryptionSetName";

    @Inject
    private AzurePollTaskFactory underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private AzureClient azureClient;

    @Test
    void diskEncryptionSetCreationCheckerTaskTest() {
        when(authenticatedContext.getParameter(AzureClient.class)).thenReturn(azureClient);
        DiskEncryptionSetCreationCheckerContext checkerContext = new DiskEncryptionSetCreationCheckerContext(RESOURCE_GROUP_NAME, DISK_ENCRYPTION_SET_NAME);

        PollTask<DiskEncryptionSetInner> result = underTest.diskEncryptionSetCreationCheckerTask(authenticatedContext, checkerContext);

        assertThat(result).isInstanceOf(DiskEncryptionSetCreationCheckerTask.class);

        DiskEncryptionSetCreationCheckerTask checkerTask = (DiskEncryptionSetCreationCheckerTask) result;
        assertThat(checkerTask.getAuthenticatedContext()).isSameAs(authenticatedContext);
    }

    @Configuration
    @EnableConfigurationProperties
    @Import({
            AzurePollTaskFactory.class,
            DiskEncryptionSetCreationCheckerTask.class
    })
    static class TestAppContext {
    }

}