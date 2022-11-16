package com.sequenceiq.cloudbreak.cloud.azure.task.image;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageInfo;
import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;

@ExtendWith(MockitoExtension.class)
public class AzureManagedImageCreationPollerTest {

    @Mock
    private AzurePollTaskFactory azurePollTaskFactory;

    @Mock
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @InjectMocks
    private AzureManagedImageCreationPoller underTest;

    @Test
    public void testWhenTimeoutExceptionThenIsNotCaught() throws Exception {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AzureManagedImageCreationCheckerContext checkerContext = mock(AzureManagedImageCreationCheckerContext.class);
        when(checkerContext.getAzureImageInfo()).thenReturn(new AzureImageInfo("", "", "", "", ""));

        when(syncPollingScheduler.schedule(any(), anyInt(), anyInt(), anyInt())).thenThrow(new TimeoutException("some text"));

        TimeoutException exception = assertThrows(TimeoutException.class, () -> {
            underTest.startPolling(ac, checkerContext);
        });

        assertEquals("some text", exception.getMessage());
    }
}
