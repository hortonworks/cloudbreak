package com.sequenceiq.freeipa.flow.stack.upgrade.ccm.handler;

import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_DEREGISTER_MINA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_OBTAIN_AGENT_DATA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_RECONFIGURE_NGINX_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_REGISTER_CLUSTER_PROXY_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_REMOVE_MINA_FINISHED_EVENT;
import static com.sequenceiq.freeipa.flow.stack.upgrade.ccm.selector.UpgradeCcmStateSelector.UPGRADE_CCM_UPGRADE_FINISHED_EVENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.internal.configuration.DefaultInjectionEngine;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.verification.VerificationMode;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.UpgradeCcmService;
import com.sequenceiq.freeipa.flow.stack.upgrade.ccm.event.UpgradeCcmEvent;
import com.sequenceiq.freeipa.service.upgrade.ccm.CcmParametersConfigService;

import reactor.bus.Event;
import reactor.bus.EventBus;

/**
 * This test class dynamically checks all handlers for Upgrade CCM flow where there is an operation needed in case of CCMv1 original tunnel,
 * but a no-operation is needed in CCMv2 (or any other case, for which the state is invalid anyway)
 * Argument provider is used to dynamically construct similarly structured handler classes and dynamically verify the method calls on the mocked service class
 */
@ExtendWith(MockitoExtension.class)
class UpgradeCcmGenericHandlerTest {

    private static final long STACK_ID = 234L;

    @Mock
    private UpgradeCcmService upgradeCcmService;

    @Mock
    private EventBus eventBus;

    @Mock
    private CcmParametersConfigService ccmParametersConfigService;

    @Captor
    private ArgumentCaptor<Event<UpgradeCcmEvent>> eventCaptor;

    private AbstractUpgradeCcmEventHandler underTest;

    @ArgumentsSource(TestScenarios.class)
    @ParameterizedTest(name = "Handler: {0}, Verified method: {2}, Tunnel: {3}")
    void testHandlerVerifyServiceMethodCall(String testname, Class<? extends AbstractUpgradeCcmEventHandler> handlerClass,
            String methodToVerify, Tunnel tunnel, VerificationMode mockVerificationMode, String expectedEndState) throws Exception {

        injectMocks(handlerClass);
        UpgradeCcmEvent upgradeCcmEvent = new UpgradeCcmEvent("selector", STACK_ID, tunnel, null);
        Event<UpgradeCcmEvent> event = new Event<>(upgradeCcmEvent);
        underTest.accept(event);
        InOrder inOrder = inOrder(upgradeCcmService);
        inOrder.verify(upgradeCcmService).changeTunnel(STACK_ID, Tunnel.latestUpgradeTarget());
        UpgradeCcmService.class.getMethod(methodToVerify, Long.class).invoke(inOrder.verify(upgradeCcmService, mockVerificationMode), STACK_ID);
        verify(eventBus).notify(eq(expectedEndState), eventCaptor.capture());
        Event<UpgradeCcmEvent> eventResult = eventCaptor.getValue();
        assertThat(eventResult.getData().getOldTunnel()).isEqualTo(tunnel);
        assertThat(eventResult.getData().selector()).isEqualTo(expectedEndState);
        assertThat(eventResult.getData().getResourceId()).isEqualTo(STACK_ID);
    }

    private void injectMocks(Class<? extends AbstractUpgradeCcmEventHandler> testedClass) throws Exception {
        underTest = testedClass.getDeclaredConstructor().newInstance();
        new DefaultInjectionEngine().injectMocksOnFields(Set.of(getClass().getDeclaredField("underTest")), Set.of(upgradeCcmService, eventBus,
                ccmParametersConfigService), this);
    }

    static class TestScenarios implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
            //CHECKSTYLE:OFF: checkstyle:LineLength
            return Stream.of(
                    argumentsMaker(UpgradeCcmDeregisterMinaHandler.class, "deregisterMina", UPGRADE_CCM_DEREGISTER_MINA_FINISHED_EVENT.event()).stream(),
                    argumentsMaker(UpgradeCcmObtainAgentDataHandler.class, "obtainAgentData", UPGRADE_CCM_OBTAIN_AGENT_DATA_FINISHED_EVENT.event()).stream(),
                    argumentsMaker(UpgradeCcmPushSaltStatesHandler.class, "pushSaltStates", UPGRADE_CCM_PUSH_SALT_STATES_FINISHED_EVENT.event()).stream(),
                    argumentsMaker(UpgradeCcmReconfigureNginxHandler.class, "reconfigureNginx", UPGRADE_CCM_RECONFIGURE_NGINX_FINISHED_EVENT.event()).stream(),
                    argumentsMaker(UpgradeCcmRegisterClusterProxyHandler.class, "registerClusterProxyAndCheckHealth", UPGRADE_CCM_REGISTER_CLUSTER_PROXY_FINISHED_EVENT.event()).stream(),
                    argumentsMaker(UpgradeCcmRemoveMinaHandler.class, "removeMina", UPGRADE_CCM_REMOVE_MINA_FINISHED_EVENT.event()).stream(),
                    argumentsMaker(UpgradeCcmUpgradeHandler.class, "upgrade", UPGRADE_CCM_UPGRADE_FINISHED_EVENT.event()).stream()
            ).flatMap(Function.identity());
            //CHECKSTYLE:ON: checkstyle:LineLength
        }

        private List<Arguments> argumentsMaker(Class<?> clazz, String methodNameForCheck, String selector) {
            List<Arguments> argumentsList = new ArrayList<>();
            argumentsList.add(Arguments.of(clazz.getSimpleName(), clazz, methodNameForCheck, Tunnel.CCM, times(1), selector));
            Arrays.stream(Tunnel.values()).filter(tunnel -> tunnel != Tunnel.CCM).forEach(tunnel -> {
                argumentsList.add(Arguments.of(clazz.getSimpleName(), clazz, methodNameForCheck, tunnel, never(), selector));
            });
            return argumentsList;

        }
    }

}
