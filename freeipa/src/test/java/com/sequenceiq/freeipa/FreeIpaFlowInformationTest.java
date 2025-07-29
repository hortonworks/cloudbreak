package com.sequenceiq.freeipa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reflections.Reflections;

import com.sequenceiq.flow.core.FlowEvent;
import com.sequenceiq.flow.core.config.AbstractFlowConfiguration;
import com.sequenceiq.flow.core.config.FlowConfiguration;
import com.sequenceiq.flow.domain.FlowLog;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.StackStatus;
import com.sequenceiq.freeipa.flow.freeipa.imdupdate.event.FreeIpaInstanceMetadataUpdateEvent;
import com.sequenceiq.freeipa.flow.freeipa.loadbalancer.FreeIpaLoadBalancerCreationEvent;
import com.sequenceiq.freeipa.flow.freeipa.provision.FreeIpaProvisionEvent;
import com.sequenceiq.freeipa.flow.freeipa.rebuild.FreeIpaRebuildFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.cancel.FreeIpaCancelTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.finish.FreeIpaFinishTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.freeipa.trust.setup.FreeIpaTrustSetupFlowEvent;
import com.sequenceiq.freeipa.flow.stack.provision.StackProvisionEvent;
import com.sequenceiq.freeipa.flow.stack.start.StackStartEvent;
import com.sequenceiq.freeipa.flow.stack.stop.StackStopEvent;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class FreeIpaFlowInformationTest {

    /**
     * Most of the FreeIPA flows should be able to run parallel to
     *
     * @see com.sequenceiq.freeipa.flow.freeipa.binduser.create.CreateBindUserFlowConfig and
     * @see com.sequenceiq.freeipa.flow.freeipa.cleanup.FreeIpaCleanupFlowConfig
     * The actual validation if the flow should be able to start next to another happens in
     * @see com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaParallelFlowValidator
     */
    private static final Set<String> NON_PARALLEL_FLOWS = Set.of(
            StackStartEvent.STACK_START_EVENT.event(),
            StackStopEvent.STACK_STOP_EVENT.event(),
            FreeIpaProvisionEvent.FREEIPA_PROVISION_EVENT.event(),
            StackProvisionEvent.START_CREATION_EVENT.event(),
            FreeIpaRebuildFlowEvent.REBUILD_EVENT.event(),
            FreeIpaInstanceMetadataUpdateEvent.STACK_IMDUPDATE_EVENT.event(),
            FreeIpaLoadBalancerCreationEvent.FREEIPA_LOAD_BALANCER_CREATION_EVENT.event(),
            FreeIpaTrustSetupFlowEvent.TRUST_SETUP_EVENT.event(),
            FreeIpaFinishTrustSetupFlowEvent.FINISH_TRUST_SETUP_EVENT.event(),
            FreeIpaCancelTrustSetupFlowEvent.CANCEL_TRUST_SETUP_EVENT.event()
    );

    @Mock
    private StackService stackService;

    @InjectMocks
    private FreeIpaFlowInformation freeIpaFlowInformation;

    @Test
    void handleFlowFailTest() {
        FlowLog flowLog = mock(FlowLog.class);
        when(flowLog.getResourceId()).thenReturn(1L);
        Stack stack = new Stack();
        StackStatus stackStatus = mock(StackStatus.class);
        when(stackStatus.getStatus()).thenReturn(Status.UPGRADE_CCM_IN_PROGRESS);
        stack.setStackStatus(stackStatus);
        when(stackService.getStackById(1L)).thenReturn(stack);
        freeIpaFlowInformation.handleFlowFail(flowLog);
        ArgumentCaptor<Stack> stackArgumentCaptor = ArgumentCaptor.forClass(Stack.class);
        verify(stackService, times(1)).save(stackArgumentCaptor.capture());
        assertEquals(Status.UPGRADE_CCM_FAILED, stackArgumentCaptor.getValue().getStackStatus().getStatus());
    }

    @Test
    void testParallelFlows() {
        Set<Class<? extends AbstractFlowConfiguration>> flowConfigs =
                new Reflections("com.sequenceiq.freeipa.flow").getSubTypesOf(AbstractFlowConfiguration.class);
        Set<String> initEvents = flowConfigs.stream()
                .map(clazz -> {
                    try {
                        Constructor<? extends AbstractFlowConfiguration> constructor = clazz.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        return constructor.newInstance();
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(FlowConfiguration::getInitEvents)
                .flatMap(Arrays::stream).map(FlowEvent::event)
                .collect(Collectors.toSet());
        List<String> allowedParallelFlows = new FreeIpaFlowInformation().getAllowedParallelFlows();
        initEvents.removeAll(allowedParallelFlows);
        initEvents.removeAll(NON_PARALLEL_FLOWS);

        assertTrue(initEvents.isEmpty(), () -> "Please ensure your flow either parallel, if it needs to run next to bind user creation/cleanup, "
                + "or added to exception list above. Flow init events causing issue: " + initEvents);
    }
}