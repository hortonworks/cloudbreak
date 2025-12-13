package com.sequenceiq.datalake.flow.chain;

import static com.sequenceiq.datalake.flow.create.SdxCreateEvent.SDX_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.delete.SdxDeleteEvent.SDX_DELETE_EVENT;
import static com.sequenceiq.datalake.flow.detach.SdxDetachEvent.SDX_DETACH_EVENT;
import static com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupEvent.DATALAKE_TRIGGER_BACKUP_EVENT;
import static com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreEvent.DATALAKE_TRIGGER_RESTORE_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeBackupValidationEvent.DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.dr.validation.DatalakeRestoreValidationEvent.DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT;
import static com.sequenceiq.datalake.flow.graph.FlowOfflineStateGraphGenerator.FLOW_CONFIGS_PACKAGE_NAME;
import static com.sequenceiq.datalake.flow.loadbalancer.dns.UpdateLoadBalancerDNSEvent.UPDATE_LOAD_BALANCER_DNS_IPA_EVENT;
import static com.sequenceiq.datalake.flow.stop.SdxStopEvent.SDX_STOP_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.datalakedr.DatalakeDrSkipOptions;
import com.sequenceiq.datalake.entity.SdxCluster;
import com.sequenceiq.datalake.entity.SdxDatabase;
import com.sequenceiq.datalake.flow.SdxEvent;
import com.sequenceiq.datalake.flow.delete.event.SdxDeleteStartEvent;
import com.sequenceiq.datalake.flow.detach.event.DatalakeResizeFlowChainStartEvent;
import com.sequenceiq.datalake.flow.detach.event.SdxStartDetachEvent;
import com.sequenceiq.datalake.flow.dr.backup.DatalakeBackupFailureReason;
import com.sequenceiq.datalake.flow.dr.backup.event.DatalakeTriggerBackupEvent;
import com.sequenceiq.datalake.flow.dr.restore.DatalakeRestoreFailureReason;
import com.sequenceiq.datalake.flow.dr.restore.event.DatalakeTriggerRestoreEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerBackupValidationEvent;
import com.sequenceiq.datalake.flow.dr.validation.event.DatalakeTriggerRestoreValidationEvent;
import com.sequenceiq.datalake.flow.loadbalancer.dns.event.StartUpdateLoadBalancerDNSEvent;
import com.sequenceiq.datalake.flow.refresh.DatahubRefreshFlowEvent;
import com.sequenceiq.datalake.flow.refresh.event.DatahubRefreshStartEvent;
import com.sequenceiq.datalake.flow.stop.event.SdxStartStopEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;
import com.sequenceiq.flow.graph.FlowChainConfigGraphGeneratorUtil;
import com.sequenceiq.sdx.api.model.SdxClusterShape;

@ExtendWith(MockitoExtension.class)
class DatalakeResizeFlowEventChainFactoryTest {
    private static final String USER_CRN = "crn:cdp:iam:us-west-1:1234:user:1";

    private static final String BACKUP_LOCATION = "s3a://path/to/backup";

    private static final Long OLD_SDX_ID = 1L;

    private static final Long NEW_SDX_ID = 2L;

    private static final String OLD_CLUSTER_NAME = "old-test-dl";

    private static final String NEW_CLUSTER_NAME = "new-test-dl";

    @InjectMocks
    private DatalakeResizeFlowEventChainFactory factory;

    private SdxCluster oldSdxCluster;

    private SdxCluster newSdxCluster;

    @BeforeEach
    public void setUp() {
        oldSdxCluster = getValidSdxCluster(OLD_SDX_ID, OLD_CLUSTER_NAME);
        newSdxCluster = getValidSdxCluster(NEW_SDX_ID, NEW_CLUSTER_NAME);
    }

    @Test
    void chainCreationTestValidationOnly() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(oldSdxCluster.getId(), newSdxCluster, USER_CRN, BACKUP_LOCATION,
                true, true, new DatalakeDrSkipOptions(false, false, false, false), true);
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);

        assertEquals(4, flowTriggerEventQueue.getQueue().size());
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(flowTriggerEventQueue.getQueue());
        flowTriggerEventQueue.getQueue().remove();
        assertTriggerBackupValidationEvent(flowTriggerEventQueue);
        assertTriggerRestoreValidationEvent(flowTriggerEventQueue);

        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(factory, FLOW_CONFIGS_PACKAGE_NAME, flowTriggerEventQueue, "VALIDATION_ONLY");
    }

    @Test
    void chainCreationTestSkipValidation() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(oldSdxCluster.getId(), newSdxCluster, USER_CRN, BACKUP_LOCATION,
                true, true, new DatalakeDrSkipOptions(true, false, false, false), false);
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);

        assertEquals(10, flowTriggerEventQueue.getQueue().size());
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(flowTriggerEventQueue.getQueue());
        flowTriggerEventQueue.getQueue().remove();
        assertBackupEvent(flowTriggerEventQueue);
        assertStopEvent(flowTriggerEventQueue);
        assertDetachEvent(flowTriggerEventQueue);
        assertCreateEvent(flowTriggerEventQueue);
        assertRestoreEvent(flowTriggerEventQueue);
        assertDeleteEvent(flowTriggerEventQueue);
        assertUpdateLBDNSEvent(flowTriggerEventQueue);
        assertDatahubRefreshEvent(flowTriggerEventQueue);

        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(factory, FLOW_CONFIGS_PACKAGE_NAME, flowTriggerEventQueue, "WITHOUT_VALIDATION");
    }

    @Test
    void chainCreationTestSkipBackup() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(oldSdxCluster.getId(), newSdxCluster, USER_CRN, BACKUP_LOCATION,
                false, true, new DatalakeDrSkipOptions(false, false, false, false), false);
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);

        assertEquals(11, flowTriggerEventQueue.getQueue().size());
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(flowTriggerEventQueue.getQueue());
        flowTriggerEventQueue.getQueue().remove();
        assertTriggerBackupValidationEvent(flowTriggerEventQueue);
        assertTriggerRestoreValidationEvent(flowTriggerEventQueue);
        assertStopEvent(flowTriggerEventQueue);
        assertDetachEvent(flowTriggerEventQueue);
        assertCreateEvent(flowTriggerEventQueue);
        assertRestoreEvent(flowTriggerEventQueue);
        assertDeleteEvent(flowTriggerEventQueue);
        assertUpdateLBDNSEvent(flowTriggerEventQueue);
        assertDatahubRefreshEvent(flowTriggerEventQueue);

        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(factory, FLOW_CONFIGS_PACKAGE_NAME, flowTriggerEventQueue, "WITHOUT_BACKUP");
    }

    @Test
    void chainCreationTestSkipRestore() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(oldSdxCluster.getId(), newSdxCluster, USER_CRN, BACKUP_LOCATION,
                true, false, new DatalakeDrSkipOptions(false, false, false, false), false);
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);

        assertEquals(11, flowTriggerEventQueue.getQueue().size());
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(flowTriggerEventQueue.getQueue());
        flowTriggerEventQueue.getQueue().remove();
        assertTriggerBackupValidationEvent(flowTriggerEventQueue);
        assertTriggerRestoreValidationEvent(flowTriggerEventQueue);
        assertBackupEvent(flowTriggerEventQueue);
        assertStopEvent(flowTriggerEventQueue);
        assertDetachEvent(flowTriggerEventQueue);
        assertCreateEvent(flowTriggerEventQueue);
        assertDeleteEvent(flowTriggerEventQueue);
        assertUpdateLBDNSEvent(flowTriggerEventQueue);
        assertDatahubRefreshEvent(flowTriggerEventQueue);

        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(factory, FLOW_CONFIGS_PACKAGE_NAME, flowTriggerEventQueue, "WITHOUT_RESTORE");
    }

    @Test
    void chainCreationTestSkipBackupAndRestore() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(oldSdxCluster.getId(), newSdxCluster, USER_CRN, BACKUP_LOCATION,
                false, false, new DatalakeDrSkipOptions(false, false, false, false), false);
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);

        assertEquals(10, flowTriggerEventQueue.getQueue().size());
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(flowTriggerEventQueue.getQueue());
        flowTriggerEventQueue.getQueue().remove();
        assertTriggerBackupValidationEvent(flowTriggerEventQueue);
        assertTriggerRestoreValidationEvent(flowTriggerEventQueue);
        assertStopEvent(flowTriggerEventQueue);
        assertDetachEvent(flowTriggerEventQueue);
        assertCreateEvent(flowTriggerEventQueue);
        assertDeleteEvent(flowTriggerEventQueue);
        assertUpdateLBDNSEvent(flowTriggerEventQueue);
        assertDatahubRefreshEvent(flowTriggerEventQueue);

        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(factory, FLOW_CONFIGS_PACKAGE_NAME, flowTriggerEventQueue, "WITHOUT_BACKUP_AND_RESTORE");
    }

    @Test
    void fullChainCreationTest() {
        DatalakeResizeFlowChainStartEvent event = new DatalakeResizeFlowChainStartEvent(oldSdxCluster.getId(), newSdxCluster, USER_CRN, BACKUP_LOCATION,
                true, true, new DatalakeDrSkipOptions(false, false, false, false), false);
        FlowTriggerEventQueue flowTriggerEventQueue = factory.createFlowTriggerEventQueue(event);

        assertEquals(12, flowTriggerEventQueue.getQueue().size());
        Queue<Selectable> restrainedQueueData = new ConcurrentLinkedQueue<>(flowTriggerEventQueue.getQueue());
        flowTriggerEventQueue.getQueue().remove();
        assertTriggerBackupValidationEvent(flowTriggerEventQueue);
        assertTriggerRestoreValidationEvent(flowTriggerEventQueue);
        assertBackupEvent(flowTriggerEventQueue);
        assertStopEvent(flowTriggerEventQueue);
        assertDetachEvent(flowTriggerEventQueue);
        assertCreateEvent(flowTriggerEventQueue);
        assertRestoreEvent(flowTriggerEventQueue);
        assertDeleteEvent(flowTriggerEventQueue);
        assertUpdateLBDNSEvent(flowTriggerEventQueue);
        assertDatahubRefreshEvent(flowTriggerEventQueue);

        flowTriggerEventQueue.getQueue().addAll(restrainedQueueData);
        FlowChainConfigGraphGeneratorUtil.generateFor(factory, FLOW_CONFIGS_PACKAGE_NAME, flowTriggerEventQueue, "FULL_CHAIN");
    }

    private void assertTriggerBackupValidationEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(DATALAKE_TRIGGER_BACKUP_VALIDATION_EVENT.selector(), triggerEvent.selector());
        assertEquals(oldSdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(DatalakeTriggerBackupValidationEvent.class, triggerEvent);
        DatalakeTriggerBackupValidationEvent event = (DatalakeTriggerBackupValidationEvent) triggerEvent;
        assertEquals(BACKUP_LOCATION, event.getBackupLocation());
    }

    private void assertTriggerRestoreValidationEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(DATALAKE_TRIGGER_RESTORE_VALIDATION_EVENT.selector(), triggerEvent.selector());
        assertEquals(oldSdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(DatalakeTriggerRestoreValidationEvent.class, triggerEvent);
        DatalakeTriggerRestoreValidationEvent event = (DatalakeTriggerRestoreValidationEvent) triggerEvent;
        assertEquals(BACKUP_LOCATION, event.getBackupLocation());
    }

    private void assertBackupEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(DATALAKE_TRIGGER_BACKUP_EVENT.selector(), triggerEvent.selector());
        assertEquals(oldSdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(DatalakeTriggerBackupEvent.class, triggerEvent);
        DatalakeTriggerBackupEvent event = (DatalakeTriggerBackupEvent) triggerEvent;
        assertEquals(BACKUP_LOCATION, event.getBackupLocation());
        assertTrue(event.getBackupName().startsWith("resize"));
        assertEquals(DatalakeBackupFailureReason.BACKUP_ON_RESIZE, event.getReason());
    }

    private void assertStopEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(SDX_STOP_EVENT.selector(), triggerEvent.selector());
        assertEquals(oldSdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(SdxStartStopEvent.class, triggerEvent);
        SdxStartStopEvent event = (SdxStartStopEvent) triggerEvent;
        assertFalse(event.isStopDataHubs());
    }

    private void assertDetachEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(SDX_DETACH_EVENT.selector(), triggerEvent.selector());
        assertEquals(oldSdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(SdxStartDetachEvent.class, triggerEvent);
        SdxStartDetachEvent event = (SdxStartDetachEvent) triggerEvent;
        assertEquals(newSdxCluster, event.getSdxCluster());
    }

    private void assertCreateEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(SDX_VALIDATION_EVENT.selector(), triggerEvent.selector());
        assertEquals(oldSdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(SdxEvent.class, triggerEvent);
        SdxEvent event = (SdxEvent) triggerEvent;
        assertEquals(NEW_CLUSTER_NAME, event.getSdxName());
    }

    private void assertRestoreEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(DATALAKE_TRIGGER_RESTORE_EVENT.selector(), triggerEvent.selector());
        assertEquals(oldSdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(DatalakeTriggerRestoreEvent.class, triggerEvent);
        DatalakeTriggerRestoreEvent event = (DatalakeTriggerRestoreEvent) triggerEvent;
        assertEquals(NEW_CLUSTER_NAME, event.getSdxName());
        assertNull(event.getBackupId());
        assertEquals(BACKUP_LOCATION, event.getBackupLocation());
        assertNull(event.getBackupLocationOverride());
        assertEquals(DatalakeRestoreFailureReason.RESTORE_ON_RESIZE, event.getReason());
        assertFalse(event.isValidationOnly());
    }

    private void assertDeleteEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(SDX_DELETE_EVENT.selector(), triggerEvent.selector());
        assertEquals(oldSdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(SdxDeleteStartEvent.class, triggerEvent);
        SdxDeleteStartEvent event = (SdxDeleteStartEvent) triggerEvent;
        assertTrue(event.isForced());
    }

    private void assertUpdateLBDNSEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(UPDATE_LOAD_BALANCER_DNS_IPA_EVENT.selector(), triggerEvent.selector());
        assertEquals(oldSdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(StartUpdateLoadBalancerDNSEvent.class, triggerEvent);
        StartUpdateLoadBalancerDNSEvent event = (StartUpdateLoadBalancerDNSEvent) triggerEvent;
        assertEquals(NEW_CLUSTER_NAME, event.getSdxName());
    }

    private void assertDatahubRefreshEvent(FlowTriggerEventQueue flowChainQueue) {
        Selectable triggerEvent = flowChainQueue.getQueue().remove();
        assertEquals(DatahubRefreshFlowEvent.DATAHUB_REFRESH_START_EVENT.event(), triggerEvent.selector());
        assertEquals(oldSdxCluster.getId(), triggerEvent.getResourceId());
        assertInstanceOf(DatahubRefreshStartEvent.class, triggerEvent);
        DatahubRefreshStartEvent event = (DatahubRefreshStartEvent) triggerEvent;
        assertEquals(NEW_CLUSTER_NAME, event.getSdxName());
    }

    private SdxCluster getValidSdxCluster(Long sdxId, String clusterName) {
        SdxCluster sdxCluster = new SdxCluster();
        sdxCluster.setClusterName(clusterName);
        sdxCluster.setClusterShape(SdxClusterShape.LIGHT_DUTY);
        sdxCluster.setEnvName("test-env");
        sdxCluster.setCrn("crn:sdxcluster");
        sdxCluster.setRangerRazEnabled(true);
        sdxCluster.setId(sdxId);
        SdxDatabase sdxDatabase = new SdxDatabase();
        sdxDatabase.setDatabaseCrn("crn:sdxcluster");
        sdxCluster.setSdxDatabase(sdxDatabase);
        return sdxCluster;
    }
}
