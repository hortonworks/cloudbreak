package com.sequenceiq.cloudbreak.core.flow2;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.core.task.AsyncTaskExecutor;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.autoscales.request.InstanceGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.HostGroupAdjustmentV4Request;
import com.sequenceiq.cloudbreak.common.event.AcceptResult;
import com.sequenceiq.cloudbreak.common.event.Acceptable;
import com.sequenceiq.cloudbreak.common.type.ScalingType;
import com.sequenceiq.cloudbreak.core.flow2.chain.FlowChainTriggers;
import com.sequenceiq.cloudbreak.core.flow2.event.MaintenanceModeValidationTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackAndClusterUpscaleTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.event.StackImageUpdateTriggerEvent;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.core.flow2.service.TerminationTriggerService;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.service.stack.repair.UnhealthyInstances;
import com.sequenceiq.flow.core.model.FlowAcceptResult;
import com.sequenceiq.flow.service.FlowCancelService;

import reactor.rx.Promise;

@RunWith(MockitoJUnitRunner.class)
public class ReactorFlowManagerTest {

    private static final Long STACK_ID = 1L;

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:tenantName:user:userName";

    @Mock
    private ReactorNotifier reactorNotifier;

    @Mock
    private AsyncTaskExecutor asyncTaskExecutor;

    @Mock
    private TerminationTriggerService terminationTriggerService;

    @Mock
    private FlowCancelService flowCancelService;

    private Stack stack;

    @InjectMocks
    private ReactorFlowManager underTest;

    @Before
    public void setUp() {
        reset(reactorNotifier);
        stack = TestUtil.stack();
        stack.setCluster(TestUtil.cluster());
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        when(asyncTaskExecutor.submit(captor.capture())).then(invocation -> {
            captor.getValue().run();
            return null;
        });
    }

    @Test
    public void
    shouldReturnTheNextFailureTransition() {
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        HostGroupAdjustmentV4Request hostGroupAdjustment = new HostGroupAdjustmentV4Request();
        Map<String, Set<Long>> instanceIdsByHostgroup = new HashMap<>();
        instanceIdsByHostgroup.put("hostrgroup", Collections.singleton(1L));

        underTest.triggerProvisioning(STACK_ID);
        underTest.triggerClusterInstall(STACK_ID);
        underTest.triggerClusterReInstall(STACK_ID);
        underTest.triggerStackStop(STACK_ID);
        underTest.triggerStackStart(STACK_ID);
        underTest.triggerClusterStop(STACK_ID);
        underTest.triggerClusterStart(STACK_ID);
        underTest.triggerTermination(STACK_ID, false);
        underTest.triggerStackUpscale(STACK_ID, instanceGroupAdjustment, true);
        underTest.triggerStackDownscale(STACK_ID, instanceGroupAdjustment);
        underTest.triggerStackRemoveInstance(STACK_ID, "hostgroup", 5L);
        underTest.triggerStackRemoveInstance(STACK_ID, "hostgroup", 5L, false);
        underTest.triggerStackRemoveInstances(STACK_ID, instanceIdsByHostgroup, false);
        underTest.triggerClusterUpscale(STACK_ID, hostGroupAdjustment);
        underTest.triggerClusterDownscale(STACK_ID, hostGroupAdjustment);
        underTest.triggerClusterSync(STACK_ID);
        underTest.triggerClusterSyncWithoutCheck(STACK_ID);
        underTest.triggerStackSync(STACK_ID);
        underTest.triggerFullSync(STACK_ID);
        underTest.triggerFullSyncWithoutCheck(STACK_ID);
        underTest.triggerClusterCredentialReplace(STACK_ID, "admin", "admin1");
        underTest.triggerClusterCredentialUpdate(STACK_ID, "admin1");
        underTest.triggerClusterTermination(stack, false, USER_CRN);
        underTest.triggerClusterTermination(stack, true, USER_CRN);
        underTest.triggerManualRepairFlow(STACK_ID);
        underTest.triggerStackRepairFlow(STACK_ID, new UnhealthyInstances());
        underTest.triggerClusterRepairFlow(STACK_ID, new HashMap<>(), true);
        underTest.triggerEphemeralUpdate(STACK_ID);
        underTest.triggerStackImageUpdate(STACK_ID, "asdf", null, null);
        underTest.triggerMaintenanceModeValidationFlow(STACK_ID);
        underTest.triggerClusterCertificationRenewal(STACK_ID);
        underTest.triggerDatalakeClusterUpgrade(STACK_ID, null);
        underTest.triggerSaltUpdate(STACK_ID);

        int count = 0;
        for (Method method : underTest.getClass().getDeclaredMethods()) {
            if (method.getName().startsWith("trigger")) {
                count++;
            }
        }
        // -3: 2 notifyWithoutCheck, 1 terminationTriggerService, 1 triggerStackRemoveInstance internal
        verify(reactorNotifier, times(count - 3)).notify(anyLong(), anyString(), any(Acceptable.class));
        verify(reactorNotifier, times(2)).notifyWithoutCheck(anyLong(), anyString(), any(Acceptable.class));
        verify(terminationTriggerService, times(1)).triggerTermination(stack, true);
        verify(terminationTriggerService, times(1)).triggerTermination(stack, false);
    }

    @Test
    public void testClusterTerminationNotForced() {
        underTest.triggerClusterTermination(stack, false, USER_CRN);

        verify(terminationTriggerService, times(1)).triggerTermination(stack, false);
    }

    @Test
    public void testClusterTerminationForced() {
        underTest.triggerClusterTermination(stack, true, USER_CRN);

        verify(terminationTriggerService, times(1)).triggerTermination(stack, true);
    }

    @Test
    public void testTriggerUpscaleWithoutClusterEvent() {
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setInstanceGroup("ig");
        instanceGroupAdjustment.setScalingAdjustment(3);

        underTest.triggerStackUpscale(stack.getId(), instanceGroupAdjustment, false);

        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier, times(1)).notify(eq(stack.getId()), eq(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT), captor.capture());
        StackAndClusterUpscaleTriggerEvent event = (StackAndClusterUpscaleTriggerEvent) captor.getValue();
        assertEquals(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.selector());
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(instanceGroupAdjustment.getInstanceGroup(), event.getInstanceGroup());
        assertEquals(instanceGroupAdjustment.getScalingAdjustment(), event.getAdjustment());
        assertEquals(ScalingType.UPSCALE_ONLY_STACK, event.getScalingType());
    }

    @Test
    public void testTriggerUpscaleWithClusterEvent() {
        InstanceGroupAdjustmentV4Request instanceGroupAdjustment = new InstanceGroupAdjustmentV4Request();
        instanceGroupAdjustment.setInstanceGroup("ig");
        instanceGroupAdjustment.setScalingAdjustment(3);

        underTest.triggerStackUpscale(stack.getId(), instanceGroupAdjustment, true);

        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier, times(1)).notify(eq(stack.getId()), eq(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT), captor.capture());
        StackAndClusterUpscaleTriggerEvent event = (StackAndClusterUpscaleTriggerEvent) captor.getValue();
        assertEquals(FlowChainTriggers.FULL_UPSCALE_TRIGGER_EVENT, event.selector());
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(instanceGroupAdjustment.getInstanceGroup(), event.getInstanceGroup());
        assertEquals(instanceGroupAdjustment.getScalingAdjustment(), event.getAdjustment());
        assertEquals(ScalingType.UPSCALE_TOGETHER, event.getScalingType());
    }

    @Test
    public void testtriggerStackImageUpdate() {
        long stackId = 1L;
        String imageID = "imageID";
        String imageCatalogName = "imageCatalogName";
        String imageCatalogUrl = "imageCatalogUrl";
        underTest.triggerStackImageUpdate(stackId, imageID, imageCatalogName, imageCatalogUrl);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier).notify(eq(stackId), eq(FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT), captor.capture());
        StackImageUpdateTriggerEvent event = (StackImageUpdateTriggerEvent) captor.getValue();
        assertEquals(FlowChainTriggers.STACK_IMAGE_UPDATE_TRIGGER_EVENT, event.selector());
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(imageCatalogName, event.getImageCatalogName());
        assertEquals(imageID, event.getNewImageId());
        assertEquals(imageCatalogUrl, event.getImageCatalogUrl());
    }

    @Test
    public void testTriggerMaintenanceModeValidationFlow() {
        long stackId = 1L;
        underTest.triggerMaintenanceModeValidationFlow(stackId);
        ArgumentCaptor<Acceptable> captor = ArgumentCaptor.forClass(Acceptable.class);
        verify(reactorNotifier).notify(eq(stackId), eq(FlowChainTriggers.CLUSTER_MAINTENANCE_MODE_VALIDATION_TRIGGER_EVENT), captor.capture());
        MaintenanceModeValidationTriggerEvent event = (MaintenanceModeValidationTriggerEvent) captor.getValue();
        assertEquals(stack.getId(), event.getResourceId());
        assertEquals(FlowChainTriggers.CLUSTER_MAINTENANCE_MODE_VALIDATION_TRIGGER_EVENT, event.selector());
    }

    private static class TestAcceptable implements Acceptable {
        @Override
        public Promise<AcceptResult> accepted() {
            Promise<AcceptResult> a = new Promise<>();
            a.accept(FlowAcceptResult.runningInFlow("FLOW_ID"));
            return a;
        }

        @Override
        public Long getResourceId() {
            return STACK_ID;
        }
    }
}
