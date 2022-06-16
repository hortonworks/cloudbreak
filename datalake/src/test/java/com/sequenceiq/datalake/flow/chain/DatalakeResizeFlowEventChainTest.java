package com.sequenceiq.datalake.flow.chain;


import static com.sequenceiq.datalake.flow.atlas.updated.CheckAtlasUpdatedEvent.CHECK_ATLAS_UPDATED_EVENT;
import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_TRIGGER_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason.BACKUP_ON_RESIZE;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_TRIGGER_RESTORE_EVENT;
import static com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent.DATAHUB_REFRESH_START_EVENT;
import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_EVENT;
import static com.sequenceiq.sdx.api.model.SdxClusterShape.MEDIUM_DUTY_HA;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Queue;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.atlas.updated.event.StartCheckAtlasUpdatedEvent;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeTriggerBackupEvent;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeTriggerRestoreEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshStartEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStartStopEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class DatalakeResizeFlowEventChainTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String BACKUP_LOCATION = "s3a://path/to/backup";

    private static final Long OLD_CLUSTER_ID = 0L;

    @InjectMocks
    private DatalakeResizeFlowEventChainFactory factory;

    private SdxCluster sdxCluster;

    @Before
    public void setUp() {
        sdxCluster = getValidSdxCluster();
    }

    @Test
    public void chainCreationTest() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(
                OLD_CLUSTER_ID, sdxCluster, USER_CRN, BACKUP_LOCATION, true, true
        );
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);
        validateFullEventQueue(flowTriggerEventQueue, true, true);
    }

    @Test
    public void chainCreationNoBackupTest() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(
                OLD_CLUSTER_ID, sdxCluster, USER_CRN, BACKUP_LOCATION, false, true
        );
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);
        validateFullEventQueue(flowTriggerEventQueue, false, true);
    }

    @Test
    public void chainCreationNoRestoreTest() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(
                OLD_CLUSTER_ID, sdxCluster, USER_CRN, BACKUP_LOCATION, true, false
        );
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);
        validateFullEventQueue(flowTriggerEventQueue, true, false);
    }

    @Test
    public void chainCreationNoBackupNoRestoreTest() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(
                OLD_CLUSTER_ID, sdxCluster, USER_CRN, BACKUP_LOCATION, false, false
        );
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);
        validateFullEventQueue(flowTriggerEventQueue, false, false);
    }

    private void validateFullEventQueue(FlowTriggerEventQueue flowTriggerEventQueue, boolean backup, boolean restore) {
        int numEvents = 6 + (backup ? 1 : 0) + (restore ? 1 : 0);
        Queue<Selectable> queue = flowTriggerEventQueue.getQueue();
        assertEquals(numEvents, queue.size());

        assertCheckAtlasUpdatedEvent(queue.remove());
        if (backup) {
            assertTriggerBackupEvent(queue.remove());
        }
        assertSdxStopEvent(queue.remove());
        assertSdxStartDetachEvent(queue.remove());
        assertSdxCreationEvent(queue.remove());
        if (restore) {
            assertTriggerRestoreEvent(queue.remove());
        }
        assertDeleteEvent(queue.remove());
        assertDatahubRefreshEvent(queue.remove());
        assertEquals(0, queue.size());
    }

    private void assertCheckAtlasUpdatedEvent(Selectable event) {
        assertEquals(CHECK_ATLAS_UPDATED_EVENT.selector(), event.selector());
        assertEquals(OLD_CLUSTER_ID, event.getResourceId());
        assertTrue(event instanceof StartCheckAtlasUpdatedEvent);
    }

    private void assertTriggerBackupEvent(Selectable event) {
        assertEquals(DATALAKE_TRIGGER_BACKUP_EVENT.selector(), event.selector());
        assertEquals(OLD_CLUSTER_ID, event.getResourceId());
        assertTrue(event instanceof DatalakeTriggerBackupEvent);

        DatalakeTriggerBackupEvent datalakeTriggerBackupEvent = (DatalakeTriggerBackupEvent) event;
        assertEquals(BACKUP_LOCATION, datalakeTriggerBackupEvent.getBackupLocation());
        assertTrue(datalakeTriggerBackupEvent.getBackupName().contains("resize"));
        assertEquals(BACKUP_ON_RESIZE, datalakeTriggerBackupEvent.getReason());
    }

    private void assertSdxStopEvent(Selectable event) {
        assertEquals(SDX_STOP_EVENT.selector(), event.selector());
        assertEquals(OLD_CLUSTER_ID, event.getResourceId());
        assertTrue(event instanceof SdxStartStopEvent);

        SdxStartStopEvent sdxStartStopEvent = (SdxStartStopEvent) event;
        assertFalse(sdxStartStopEvent.isStopDataHubs());
    }

    private void assertSdxStartDetachEvent(Selectable event) {
        assertEquals(SDX_DETACH_EVENT.selector(), event.selector());
        assertEquals(OLD_CLUSTER_ID, event.getResourceId());
        assertTrue(event instanceof SdxStartDetachEvent);

        SdxStartDetachEvent sdxStartDetachEvent = (SdxStartDetachEvent) event;
        assertEquals(sdxCluster, sdxStartDetachEvent.getSdxCluster());
    }

    private void assertSdxCreationEvent(Selectable event) {
        assertEquals(SDX_VALIDATION_EVENT.selector(), event.selector());
        assertEquals(OLD_CLUSTER_ID, event.getResourceId());
        assertTrue(event instanceof SdxEvent);

        SdxEvent sdxCreationEvent = (SdxEvent) event;
        assertEquals(sdxCluster.getClusterName(), sdxCreationEvent.getSdxName());
    }

    private void assertTriggerRestoreEvent(Selectable event) {
        assertEquals(DATALAKE_TRIGGER_RESTORE_EVENT.selector(), event.selector());
        assertEquals(OLD_CLUSTER_ID, event.getResourceId());
        assertTrue(event instanceof DatalakeTriggerRestoreEvent);

        DatalakeTriggerRestoreEvent datalakeTriggerRestoreEvent = (DatalakeTriggerRestoreEvent) event;
        assertEquals(sdxCluster.getClusterName(), datalakeTriggerRestoreEvent.getSdxName());
        assertNull(datalakeTriggerRestoreEvent.getBackupId());
        assertEquals(BACKUP_LOCATION, datalakeTriggerRestoreEvent.getBackupLocation());
        assertNull(datalakeTriggerRestoreEvent.getBackupLocationOverride());
    }

    private void assertDeleteEvent(Selectable event) {
        assertEquals(SDX_DELETE_EVENT.selector(), event.selector());
        assertEquals(OLD_CLUSTER_ID, event.getResourceId());
        assertTrue(event instanceof SdxDeleteStartEvent);

        SdxDeleteStartEvent sdxDeleteStartEvent = (SdxDeleteStartEvent) event;
        assertTrue(sdxDeleteStartEvent.isForced());
    }

    private void assertDatahubRefreshEvent(Selectable event) {
        assertEquals(DATAHUB_REFRESH_START_EVENT.event(), event.selector());
        assertEquals(OLD_CLUSTER_ID, event.getResourceId());
        assertTrue(event instanceof DatahubRefreshStartEvent);

        DatahubRefreshStartEvent datahubRefreshStartEvent = (DatahubRefreshStartEvent) event;
        assertEquals(sdxCluster.getClusterName(), datahubRefreshStartEvent.getSdxName());
    }

    private SdxCluster getValidSdxCluster() {
        sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(MEDIUM_DUTY_HA);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setDatabaseCrn("crn:sdxcluster");
        return sdxCluster;
    }
}
