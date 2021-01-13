package com.sequenceiq.cloudbreak.cloud.azure.task.image;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.cloud.azure.image.AzureImageInfo;
import com.sequenceiq.cloudbreak.cloud.azure.task.AzurePollTaskFactory;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.scheduler.SyncPollingScheduler;

@RunWith(MockitoJUnitRunner.class)
public class AzureManagedImageCreationPollerTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private AzurePollTaskFactory azurePollTaskFactory;

    @Mock
    private SyncPollingScheduler<Boolean> syncPollingScheduler;

    @InjectMocks
    private AzureManagedImageCreationPoller underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testWhenTimeoutExceptionThenIsNotCaught() throws Exception {
        AuthenticatedContext ac = mock(AuthenticatedContext.class);
        AzureManagedImageCreationCheckerContext checkerContext = mock(AzureManagedImageCreationCheckerContext.class);
        when(checkerContext.getAzureImageInfo()).thenReturn(new AzureImageInfo("", "", "", "", ""));

        when(syncPollingScheduler.schedule(any(), anyInt(), anyInt(), anyInt())).thenThrow(new TimeoutException());
        thrown.expect(TimeoutException.class);

        underTest.startPolling(ac, checkerContext);
    }
}
