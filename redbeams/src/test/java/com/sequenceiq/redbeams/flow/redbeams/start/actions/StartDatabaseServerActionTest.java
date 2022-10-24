package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.DatabaseStack;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.domain.stack.DBStack;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsContext;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;

@ExtendWith(MockitoExtension.class)
public class StartDatabaseServerActionTest {

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

    @Mock
    private DBStack dbStack;

    @InjectMocks
    private StartDatabaseServerAction victim;

    @Test
    public void shouldUpdateStatusOnPrepare() {
        RedbeamsEvent event = new RedbeamsEvent(RESOURCE_ID);

        victim.prepareExecution(event, null);

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.START_IN_PROGRESS);
    }

    @Test
    public void createRequestShouldReturnStartDatabaseServerRequest() {
        RedbeamsContext context = new RedbeamsContext(flowParameters, cloudContext, cloudCredential, databaseStack, dbStack);

        StartDatabaseServerRequest startDatabaseServerRequest = (StartDatabaseServerRequest) victim.createRequest(context);

        assertEquals(cloudContext, startDatabaseServerRequest.getCloudContext());
        assertEquals(cloudCredential, startDatabaseServerRequest.getCloudCredential());
        assertEquals(databaseStack, startDatabaseServerRequest.getDbStack());
    }
}