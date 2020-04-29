package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartContext;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerRequest;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StartDatabaseServerActionTest {

    private static final long RESOURCE_ID = 123L;

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String DB_VENDOR_DISPLAY_NAME = "dbVendorDisplayName";

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @Mock
    private FlowParameters flowParameters;

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
        RedbeamsStartContext context = new RedbeamsStartContext(flowParameters, cloudContext, cloudCredential, DB_INSTANCE_IDENTIFIER, DB_VENDOR_DISPLAY_NAME);

        StartDatabaseServerRequest startDatabaseServerRequest = (StartDatabaseServerRequest) victim.createRequest(context);

        assertEquals(cloudContext, startDatabaseServerRequest.getCloudContext());
        assertEquals(cloudCredential, startDatabaseServerRequest.getCloudCredential());
        assertEquals(DB_INSTANCE_IDENTIFIER, startDatabaseServerRequest.getDbInstanceIdentifier());
    }
}