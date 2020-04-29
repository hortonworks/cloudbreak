package com.sequenceiq.redbeams.flow.redbeams.start.actions;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.redbeams.api.model.common.DetailedDBStackStatus;
import com.sequenceiq.redbeams.flow.redbeams.common.RedbeamsEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartContext;
import com.sequenceiq.redbeams.flow.redbeams.start.RedbeamsStartEvent;
import com.sequenceiq.redbeams.flow.redbeams.start.event.StartDatabaseServerSuccess;
import com.sequenceiq.redbeams.metrics.MetricType;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricService;
import com.sequenceiq.redbeams.metrics.RedbeamsMetricTag;
import com.sequenceiq.redbeams.service.stack.DBStackStatusUpdater;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class StartDatabaseServerFinishedActionTest {

    private static final long RESOURCE_ID = 123L;

    private static final String DB_INSTANCE_IDENTIFIER = "dbInstanceIdentifier";

    private static final String DB_VENDOR_DISPLAY_NAME = "dbVendorDisplayName";

    @Mock
    private DBStackStatusUpdater dbStackStatusUpdater;

    @Mock
    private RedbeamsMetricService metricService;

    @Mock
    private FlowParameters flowParameters;

    @Mock
    private CloudContext cloudContext;

    @Mock
    private CloudCredential cloudCredential;

    @InjectMocks
    private StartDatabaseServerFinishedAction victim;

    @Test
    public void shouldUpdateStatusOnPrepare() {
        StartDatabaseServerSuccess event = new StartDatabaseServerSuccess(RESOURCE_ID);

        victim.prepareExecution(event, null);

        verify(dbStackStatusUpdater).updateStatus(RESOURCE_ID, DetailedDBStackStatus.STARTED);
    }

    @Test
    public void shouldIncrementMetricOnCreateRequest() {
        RedbeamsStartContext context = new RedbeamsStartContext(flowParameters, cloudContext, cloudCredential, DB_INSTANCE_IDENTIFIER, DB_VENDOR_DISPLAY_NAME);

        RedbeamsEvent redbeamsEvent = (RedbeamsEvent) victim.createRequest(context);

        verify(metricService).incrementMetricCounter(MetricType.DB_START_FINISHED, RedbeamsMetricTag.DATABASE_VENDOR.name(), DB_VENDOR_DISPLAY_NAME);
        assertEquals(RedbeamsStartEvent.REDBEAMS_START_FINISHED_EVENT.name(), redbeamsEvent.selector());
    }
}