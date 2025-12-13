package com.sequenceiq.cloudbreak.core.flow2.cluster.java;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.ClusterServicesRestartEvent.CLUSTER_SERVICES_RESTART_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Queue;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.core.flow2.cluster.restartcm.RestartClusterManagerFlowEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.salt.update.SaltUpdateEvent;
import com.sequenceiq.cloudbreak.core.flow2.cluster.services.restart.event.ClusterServicesRestartTriggerEvent;
import com.sequenceiq.cloudbreak.reactor.api.event.StackEvent;
import com.sequenceiq.flow.core.chain.config.FlowTriggerEventQueue;

class SetDefaultJavaVersionFlowChainFactoryTest {

    @Test
    void testCreateFlowTriggerEventQueueWithRestartCMAndRestartServicesAndRollingRestart() {
        SetDefaultJavaVersionFlowChainFactory setDefaultJavaVersionFlowChainFactory = new SetDefaultJavaVersionFlowChainFactory();
        SetDefaultJavaVersionTriggerEvent event =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), 0L,
                        "17", true, true, true);
        FlowTriggerEventQueue result = setDefaultJavaVersionFlowChainFactory.createFlowTriggerEventQueue(event);
        Queue<Selectable> queue = result.getQueue();
        SetDefaultJavaVersionTriggerEvent firstFlow = (SetDefaultJavaVersionTriggerEvent) queue.poll();
        assertEquals(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), firstFlow.getSelector());
        StackEvent secondFlow = (StackEvent) queue.poll();
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), secondFlow.getSelector());
        StackEvent thirdFlow = (StackEvent) queue.poll();
        assertEquals(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), thirdFlow.getSelector());
        ClusterServicesRestartTriggerEvent fourthFlow = (ClusterServicesRestartTriggerEvent) queue.poll();
        assertEquals(CLUSTER_SERVICES_RESTART_TRIGGER_EVENT.event(), fourthFlow.getSelector());
        assertTrue(fourthFlow.isRollingRestart());
    }

    @Test
    void testCreateFlowTriggerEventQueueWithRestartCMAndRestartServicesAndNoRollingRestart() {
        SetDefaultJavaVersionFlowChainFactory setDefaultJavaVersionFlowChainFactory = new SetDefaultJavaVersionFlowChainFactory();
        SetDefaultJavaVersionTriggerEvent event =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), 0L,
                        "17", true, true, false);
        FlowTriggerEventQueue result = setDefaultJavaVersionFlowChainFactory.createFlowTriggerEventQueue(event);
        Queue<Selectable> queue = result.getQueue();
        SetDefaultJavaVersionTriggerEvent firstFlow = (SetDefaultJavaVersionTriggerEvent) queue.poll();
        assertEquals(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), firstFlow.getSelector());
        StackEvent secondFlow = (StackEvent) queue.poll();
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), secondFlow.getSelector());
        StackEvent thirdFlow = (StackEvent) queue.poll();
        assertEquals(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), thirdFlow.getSelector());
        ClusterServicesRestartTriggerEvent fourthFlow = (ClusterServicesRestartTriggerEvent) queue.poll();
        assertEquals(CLUSTER_SERVICES_RESTART_TRIGGER_EVENT.event(), fourthFlow.getSelector());
        assertFalse(fourthFlow.isRollingRestart());
    }

    @Test
    void testCreateFlowTriggerEventQueueWithRestartCMAndNoRestartServices() {
        SetDefaultJavaVersionFlowChainFactory setDefaultJavaVersionFlowChainFactory = new SetDefaultJavaVersionFlowChainFactory();
        SetDefaultJavaVersionTriggerEvent event =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), 0L,
                        "17", false, true, false);
        FlowTriggerEventQueue result = setDefaultJavaVersionFlowChainFactory.createFlowTriggerEventQueue(event);
        Queue<Selectable> queue = result.getQueue();
        assertEquals(3, queue.size());
        SetDefaultJavaVersionTriggerEvent firstFlow = (SetDefaultJavaVersionTriggerEvent) queue.poll();
        assertEquals(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), firstFlow.getSelector());
        StackEvent secondFlow = (StackEvent) queue.poll();
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), secondFlow.getSelector());
        StackEvent thirdFlow = (StackEvent) queue.poll();
        assertEquals(RestartClusterManagerFlowEvent.RESTART_CLUSTER_MANAGER_TRIGGER_EVENT.event(), thirdFlow.getSelector());
    }

    @Test
    void testCreateFlowTriggerEventQueueWithNoRestartCMAndNoRestartServices() {
        SetDefaultJavaVersionFlowChainFactory setDefaultJavaVersionFlowChainFactory = new SetDefaultJavaVersionFlowChainFactory();
        SetDefaultJavaVersionTriggerEvent event =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), 0L,
                        "17", false, false, false);
        FlowTriggerEventQueue result = setDefaultJavaVersionFlowChainFactory.createFlowTriggerEventQueue(event);
        Queue<Selectable> queue = result.getQueue();
        assertEquals(2, queue.size());
        SetDefaultJavaVersionTriggerEvent firstFlow = (SetDefaultJavaVersionTriggerEvent) queue.poll();
        assertEquals(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), firstFlow.getSelector());
        StackEvent secondFlow = (StackEvent) queue.poll();
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), secondFlow.getSelector());
    }

    @Test
    void testCreateFlowTriggerEventQueueWithNoRestartCMAndRestartServices() {
        SetDefaultJavaVersionFlowChainFactory setDefaultJavaVersionFlowChainFactory = new SetDefaultJavaVersionFlowChainFactory();
        SetDefaultJavaVersionTriggerEvent event =
                new SetDefaultJavaVersionTriggerEvent(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), 0L,
                        "17", true, false, true);
        FlowTriggerEventQueue result = setDefaultJavaVersionFlowChainFactory.createFlowTriggerEventQueue(event);
        Queue<Selectable> queue = result.getQueue();
        assertEquals(3, queue.size());
        SetDefaultJavaVersionTriggerEvent firstFlow = (SetDefaultJavaVersionTriggerEvent) queue.poll();
        assertEquals(SetDefaultJavaVersionFlowEvent.SET_DEFAULT_JAVA_VERSION_EVENT.event(), firstFlow.getSelector());
        StackEvent secondFlow = (StackEvent) queue.poll();
        assertEquals(SaltUpdateEvent.SALT_UPDATE_EVENT.event(), secondFlow.getSelector());
        ClusterServicesRestartTriggerEvent thirdFlow = (ClusterServicesRestartTriggerEvent) queue.poll();
        assertEquals(CLUSTER_SERVICES_RESTART_TRIGGER_EVENT.event(), thirdFlow.getSelector());
        assertTrue(thirdFlow.isRollingRestart());
    }

}