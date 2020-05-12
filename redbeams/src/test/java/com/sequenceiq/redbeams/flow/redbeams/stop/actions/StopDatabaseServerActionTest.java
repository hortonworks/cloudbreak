package com.sequenceiq.redbeams.flow.redbeams.stop.actions;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.stop.RedbeamsStopContext;
import com.sequenceiq.redbeams.flow.redbeams.stop.event.StopDatabaseServerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StopDatabaseServerActionTest {

    private static final long RESOURCE_ID = 123L;

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private DatabaseStack databaseStack;

    @InjectMocks
    private StopDatabaseServerAction victim;

    @Test
    public void shouldUpdateStatusOnPrepare() {
        RedbeamsEvent event = new RedbeamsEvent(RESOURCE_ID);

        victim.prepareExecution(event, null);

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.STOP_IN_PROGRESS);
    }

    @Test
    public void createRequestShouldReturnStopDatabaseServerRequest() {
        RedbeamsStopContext context = new RedbeamsStopContext(flowParameters, cloudContext, cloudCredential, databaseStack);

        StopDatabaseServerRequest stopDatabaseServerRequest = (StopDatabaseServerRequest) victim.createRequest(context);

        assertEquals(cloudContext, stopDatabaseServerRequest.getCloudContext());
        assertEquals(cloudCredential, stopDatabaseServerRequest.getCloudCredential());
    }
}