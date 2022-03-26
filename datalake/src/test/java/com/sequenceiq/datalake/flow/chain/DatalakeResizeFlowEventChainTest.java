package com.sequenceiq.datalake.flow.chain;


import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_TRIGGER_BACKUP_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeTriggerBackupEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class DatalakeResizeFlowEventChainTest {

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String BACKUP_LOCATION = "s3a://path/to/backup";

    @InjectMocks
    private DatalakeResizeFlowEventChainFactory factory;

    private SdxCluster sdxCluster;

    @Before
    public void setUp() {
        sdxCluster = getValidSdxCluster();
    }

    @Test
    public void chainCreationTest() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(sdxCluster.getId(), sdxCluster, USER_CRN, BACKUP_LOCATION, true);
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);
        assertEquals(7, flowTriggerEventQueue.getQueue().size());
        assertTriggerBackupEvent(flowTriggerEventQueue);
    }

    private void assertTriggerBackupEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerBackupEvent = flowChainQueue.getQueue().remove();
        assertEquals(DATALAKE_TRIGGER_BACKUP_EVENT.selector(), triggerBackupEvent.selector());
        assertEquals(sdxCluster.getId(), triggerBackupEvent.getResourceId());
        assertTrue(triggerBackupEvent instanceof DatalakeTriggerBackupEvent);
        DatalakeTriggerBackupEvent event = (DatalakeTriggerBackupEvent) triggerBackupEvent;
        assertEquals(BACKUP_LOCATION, event.getBackupLocation());
        assertTrue(event.getBackupName().startsWith("resize"));

    }

    private SdxCluster getValidSdxCluster() {
        sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setDatabaseCrn("crn:sdxcluster");
        sdxCluster.setId(1L);
        return sdxCluster;
    }
}
