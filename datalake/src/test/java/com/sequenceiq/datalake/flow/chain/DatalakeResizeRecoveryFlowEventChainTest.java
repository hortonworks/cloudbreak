package com.sequenceiq.datalake.flow.chain;

import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachRecoveryEvent.SDX_DETACH_RECOVERY_EVENT;
import static com.sequenceiq.datalake.flow.start.SdxStartEvent.SDX_START_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Queue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeRecoveryFlowChainStartEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachRecoveryEvent;
import com.sequenceiq.datalake.flow.start.event.SdxStartStartEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

public class DatalakeResizeRecoveryFlowEventChainTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final long OLD_CLUSTER_SDX_ID = 1L;

    private static final long NEW_CLUSTER_SDX_ID = 2L;

    @InjectMocks
    private DatalakeResizeRecoveryFlowEventChainFactory factory;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void chainCreationTestAllEvents() {
        SdxCluster oldCluster = new SdxCluster();
        oldCluster.setId(OLD_CLUSTER_SDX_ID);
        oldCluster.setDetached(true);
        SdxCluster newCluster = new SdxCluster();
        newCluster.setId(NEW_CLUSTER_SDX_ID);

        DatalakeResizeRecoveryFlowChainStartEvent event = new DatalakeResizeRecoveryFlowChainStartEvent(
                oldCluster, newCluster, USER_CRN
        );
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);
        assertEquals(4, flowTriggerEventQueue.getQueue().size());

        Queue<Selectable> flowQueue = flowTriggerEventQueue.getQueue();
        checkEventIsDetach(flowQueue.remove());
        checkEventIsDeletion(flowQueue.remove());
        checkEventIsDetachRecovery(flowQueue.remove());
        checkEventIsStart(flowQueue.remove());
    }

    @Test
    public void chainCreationTestNewDLAlreadyDeleted() {
        SdxCluster oldCluster = new SdxCluster();
        oldCluster.setId(OLD_CLUSTER_SDX_ID);
        oldCluster.setDetached(true);
        SdxCluster newCluster = new SdxCluster();
        newCluster.setDeleted(1L);

        DatalakeResizeRecoveryFlowChainStartEvent event = new DatalakeResizeRecoveryFlowChainStartEvent(
                oldCluster, newCluster, USER_CRN
        );
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);
        assertEquals(2, flowTriggerEventQueue.getQueue().size());

        Queue<Selectable> flowQueue = flowTriggerEventQueue.getQueue();
        checkEventIsDetachRecovery(flowQueue.remove());
        checkEventIsStart(flowQueue.remove());
    }

    @Test
    public void chainCreationTestNewDLNull() {
        SdxCluster oldCluster = new SdxCluster();
        oldCluster.setId(OLD_CLUSTER_SDX_ID);
        oldCluster.setDetached(true);

        DatalakeResizeRecoveryFlowChainStartEvent event = new DatalakeResizeRecoveryFlowChainStartEvent(
                oldCluster, null, USER_CRN
        );
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);
        assertEquals(2, flowTriggerEventQueue.getQueue().size());

        Queue<Selectable> flowQueue = flowTriggerEventQueue.getQueue();
        checkEventIsDetachRecovery(flowQueue.remove());
        checkEventIsStart(flowQueue.remove());
    }

    @Test
    public void chainCreationTestNewDLAlreadyDeletedAndOldDLNotDetached() {
        SdxCluster oldCluster = new SdxCluster();
        oldCluster.setId(OLD_CLUSTER_SDX_ID);
        oldCluster.setDetached(false);
        SdxCluster newCluster = new SdxCluster();
        newCluster.setDeleted(1L);

        DatalakeResizeRecoveryFlowChainStartEvent event = new DatalakeResizeRecoveryFlowChainStartEvent(
                oldCluster, newCluster, USER_CRN
        );
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);
        assertEquals(1, flowTriggerEventQueue.getQueue().size());

        Queue<Selectable> flowQueue = flowTriggerEventQueue.getQueue();
        checkEventIsStart(flowQueue.remove());
    }

    private void checkEventIsDetach(Selectable event) {
        assertEquals(SDX_DETACH_EVENT.selector(), event.selector());
        assertTrue(event instanceof SdxStartDetachEvent);
        assertEquals(NEW_CLUSTER_SDX_ID, event.getResourceId());
        assertTrue(((SdxStartDetachEvent) event).isDetachDuringRecovery());
    }

    private void checkEventIsDeletion(Selectable event) {
        assertEquals(SDX_DELETE_EVENT.selector(), event.selector());
        assertTrue(event instanceof SdxDeleteStartEvent);
        assertEquals(NEW_CLUSTER_SDX_ID, event.getResourceId());
        SdxDeleteStartEvent sdxDeleteStartEvent = (SdxDeleteStartEvent) event;
        assertTrue(sdxDeleteStartEvent.isForced());
    }

    private void checkEventIsDetachRecovery(Selectable event) {
        assertEquals(SDX_DETACH_RECOVERY_EVENT.selector(), event.selector());
        assertTrue(event instanceof SdxStartDetachRecoveryEvent);
        assertEquals(OLD_CLUSTER_SDX_ID, event.getResourceId());
    }

    private void checkEventIsStart(Selectable event) {
        assertEquals(SDX_START_EVENT.selector(), event.selector());
        assertTrue(event instanceof SdxStartStartEvent);
        assertEquals(OLD_CLUSTER_SDX_ID, event.getResourceId());
    }
}
