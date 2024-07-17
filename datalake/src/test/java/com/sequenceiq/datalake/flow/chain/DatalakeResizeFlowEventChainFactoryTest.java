package com.sequenceiq.datalake.flow.chain;

import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerBackupValidationEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerRestoreValidationEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
public class DatalakeResizeFlowEventChainFactoryTest {

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
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(sdxCluster.getId(), sdxCluster, USER_CRN, BACKUP_LOCATION,
                true, true, new DatalakeDrSkipOptions(false, false, false, false), false);
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);

        assertEquals(10, flowTriggerEventQueue.getQueue().size());
        assertTriggerBackupValidationEvent(flowTriggerEventQueue);
        assertTriggerRestoreValidationEvent(flowTriggerEventQueue);
    }

    @Test
    public void chainCreationWithRazTest() {
        SdxCluster clusterWithRaz = getValidSdxClusterwithRaz();
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(clusterWithRaz.getId(), clusterWithRaz, USER_CRN, BACKUP_LOCATION,
                true, true, new DatalakeDrSkipOptions(false, false, false, false), false);
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);

        assertEquals(10, flowTriggerEventQueue.getQueue().size());
        assertTriggerBackupValidationEvent(flowTriggerEventQueue);
        assertTriggerRestoreValidationEvent(flowTriggerEventQueue);
    }

    private void assertTriggerBackupValidationEvent(FlowTriggerEventQueue flowChainQueue) {
        flowChainQueue.getQueue().remove();
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT.selector(), triggerEvent.selector());
        assertEquals(sdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(DatalakeTriggerBackupValidationEvent.class, triggerEvent);
        DatalakeTriggerBackupValidationEvent event = (DatalakeTriggerBackupValidationEvent) triggerEvent;
        assertEquals(BACKUP_LOCATION, event.getBackupLocation());
    }

    private SdxCluster getValidSdxCluster() {
        sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setId(1L);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn("crn:sdxcluster");
        sdxCluster.setSdxDatabase(sdxDatabase);
        return sdxCluster;
    }

    private SdxCluster getValidSdxClusterwithRaz() {
        sdxCluster = new SdxCluster();
        sdxCluster.setClusterName("test-sdx-cluster");
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setRangerRazEnabled(true);
        sdxCluster.setId(1L);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn("crn:sdxcluster");
        sdxCluster.setSdxDatabase(sdxDatabase);
        return sdxCluster;
    }

    private void assertTriggerRestoreValidationEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT.selector(), triggerEvent.selector());
        assertEquals(sdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(DatalakeTriggerRestoreValidationEvent.class, triggerEvent);
        DatalakeTriggerRestoreValidationEvent event = (DatalakeTriggerRestoreValidationEvent) triggerEvent;
        assertEquals(BACKUP_LOCATION, event.getBackupLocation());
    }
}
