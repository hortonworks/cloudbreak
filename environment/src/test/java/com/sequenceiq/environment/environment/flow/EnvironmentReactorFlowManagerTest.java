package com.sequenceiq.environment.environment.flow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.deletion.event.EnvDeleteEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.create.event.ExternalizedComputeClusterCreationStateSelectors;
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationEvent;
import com.sequenceiq.environment.environment.flow.externalizedcluster.reinitialization.event.ExternalizedComputeClusterReInitializationStateSelectors;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationDefaultEvent;
import com.sequenceiq.environment.environment.flow.modify.proxy.event.EnvProxyModificationStateSelectors;
import com.sequenceiq.environment.environment.service.stack.StackService;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.core.FlowConstants;
import com.sequenceiq.flow.reactor.api.event.BaseFlowEvent;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.flow.service.FlowCancelService;

@ExtendWith(MockitoExtension.class)
class EnvironmentReactorFlowManagerTest {

    private static final long ENVIRONMENT_ID = 12L;

    private static final String ENVIRONMENT_NAME = "environmentName";

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String USER_CRN = "userCrn";

    @Mock
    private EventSender eventSender;

    @Mock
    private FlowCancelService flowCancelService;

    @Mock
    private StackService stackService;

    @Mock
    private EntitlementService entitlementService;

    @InjectMocks
    private EnvironmentReactorFlowManager underTest;

    @Mock
    private FlowIdentifier flowIdentifier;

    @Captor
    private ArgumentCaptor<EnvDeleteEvent> envDeleteEventCaptor;

    @Captor
    private ArgumentCaptor<EnvProxyModificationDefaultEvent> envProxyModificationDefaultEventCaptor;

    @Captor
    private ArgumentCaptor<LoadBalancerUpdateEvent> loadbalancerUpdateEventCaptor;

    @Captor
    private ArgumentCaptor<Event.Headers> headersCaptor;

    @ParameterizedTest(name = "forced={0}")
    @ValueSource(booleans = {false, true})
    void triggerDeleteFlowTest(boolean forced) {
        EnvironmentView environment = environmentView();
        when(eventSender.sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class))).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.triggerDeleteFlow(environment, USER_CRN, forced);

        assertThat(result).isSameAs(flowIdentifier);
        verify(flowCancelService).cancelRunningFlows(ENVIRONMENT_ID);
        verify(eventSender).sendEvent(envDeleteEventCaptor.capture(), headersCaptor.capture());
        verifyEnvDeleteEvent(forced, "START_FREEIPA_DELETE_EVENT");
        verifyHeaders();
    }

    @ParameterizedTest(name = "forced={0}")
    @ValueSource(booleans = {false, true})
    void triggerCascadingDeleteFlowTest(boolean forced) {
        EnvironmentView environment = environmentView();
        when(eventSender.sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class))).thenReturn(flowIdentifier);

        FlowIdentifier result = underTest.triggerCascadingDeleteFlow(environment, USER_CRN, forced);

        assertThat(result).isSameAs(flowIdentifier);
        verify(flowCancelService).cancelRunningFlows(ENVIRONMENT_ID);
        verify(eventSender).sendEvent(envDeleteEventCaptor.capture(), headersCaptor.capture());
        verifyEnvDeleteEvent(forced, "ENV_DELETE_CLUSTERS_TRIGGER_EVENT");
        verifyHeaders();
    }

    @Test
    void triggerEnvironmentProxyConfigModification() {
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        when(environmentDto.getResourceCrn()).thenReturn(ENVIRONMENT_CRN);
        when(environmentDto.getName()).thenReturn(ENVIRONMENT_NAME);
        when(environmentDto.getId()).thenReturn(ENVIRONMENT_ID);
        ProxyConfig proxyConfig = mock(ProxyConfig.class);
        when(proxyConfig.getResourceCrn()).thenReturn("proxy-crn");
        when(eventSender.sendEvent(any(EnvProxyModificationDefaultEvent.class), any(Event.Headers.class))).thenReturn(flowIdentifier);

        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.triggerEnvironmentProxyConfigModification(environmentDto, proxyConfig));

        assertThat(result).isSameAs(flowIdentifier);
        verify(eventSender).sendEvent(envProxyModificationDefaultEventCaptor.capture(), headersCaptor.capture());
        EnvProxyModificationDefaultEvent event = envProxyModificationDefaultEventCaptor.getValue();
        assertThat(event)
                .returns(EnvProxyModificationStateSelectors.MODIFY_PROXY_START_EVENT.selector(), BaseFlowEvent::selector)
                .returns(ENVIRONMENT_CRN, EnvProxyModificationDefaultEvent::getResourceCrn)
                .returns(ENVIRONMENT_NAME, EnvProxyModificationDefaultEvent::getResourceName)
                .returns(ENVIRONMENT_ID, EnvProxyModificationDefaultEvent::getResourceId)
                .returns("proxy-crn", EnvProxyModificationDefaultEvent::getProxyConfigCrn);
        verifyHeaders();
    }

    @ParameterizedTest
    @MethodSource("triggerLoadBalancerUpdateFlowScenarios")
    void triggerLoadBalancerUpdateFlow(boolean targetingEntitled, PublicEndpointAccessGateway peag) {
        EnvironmentDto environmentDto = mock(EnvironmentDto.class);
        lenient().when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(any())).thenReturn(targetingEntitled);
        when(eventSender.sendEvent(any(LoadBalancerUpdateEvent.class), any(Event.Headers.class))).thenReturn(flowIdentifier);

        Set<String> subnets = Set.of("subnetId1", "subnetId2");
        FlowIdentifier result = ThreadBasedUserCrnProvider.doAs(USER_CRN,
                () -> underTest.triggerLoadBalancerUpdateFlow(environmentDto, ENVIRONMENT_ID, ENVIRONMENT_NAME, ENVIRONMENT_CRN,
                        peag, subnets, USER_CRN));

        assertThat(result).isEqualTo(flowIdentifier);
        verify(eventSender).sendEvent(loadbalancerUpdateEventCaptor.capture(), headersCaptor.capture());
        LoadBalancerUpdateEvent event = loadbalancerUpdateEventCaptor.getValue();
        assertThat(event)
                .returns(LoadBalancerUpdateStateSelectors.LOAD_BALANCER_UPDATE_START_EVENT.selector(), BaseFlowEvent::selector)
                .returns(environmentDto, LoadBalancerUpdateEvent::getEnvironmentDto)
                .returns(ENVIRONMENT_CRN, LoadBalancerUpdateEvent::getResourceCrn)
                .returns(ENVIRONMENT_ID, LoadBalancerUpdateEvent::getResourceId)
                .returns(ENVIRONMENT_NAME, LoadBalancerUpdateEvent::getResourceName)
                .returns(peag, LoadBalancerUpdateEvent::getEndpointAccessGateway)
                .returns(subnets, LoadBalancerUpdateEvent::getSubnetIds);
        verifyHeaders();
    }

    @Test
    void triggerExternalizedComputeClusterCreationFlowTest() {
        Environment environment = mock(Environment.class);
        when(environment.getResourceCrn()).thenReturn(ENVIRONMENT_CRN);
        when(environment.getResourceName()).thenReturn(ENVIRONMENT_NAME);
        when(environment.getId()).thenReturn(ENVIRONMENT_ID);
        when(eventSender.sendEvent(any(ExternalizedComputeClusterCreationEvent.class), any(Event.Headers.class))).thenReturn(flowIdentifier);
        FlowIdentifier result = underTest.triggerExternalizedComputeClusterCreationFlow(USER_CRN, environment);

        assertThat(result).isSameAs(flowIdentifier);
        ArgumentCaptor<ExternalizedComputeClusterCreationEvent> argumentCaptor = ArgumentCaptor.forClass(
                ExternalizedComputeClusterCreationEvent.class);
        verify(eventSender).sendEvent(argumentCaptor.capture(), headersCaptor.capture());
        ExternalizedComputeClusterCreationEvent event = argumentCaptor.getValue();
        assertThat(event)
                .returns(ExternalizedComputeClusterCreationStateSelectors.DEFAULT_COMPUTE_CLUSTER_CREATION_START_EVENT.selector(), BaseFlowEvent::selector)
                .returns(ENVIRONMENT_CRN, ExternalizedComputeClusterCreationEvent::getResourceCrn)
                .returns(ENVIRONMENT_NAME, ExternalizedComputeClusterCreationEvent::getResourceName)
                .returns(ENVIRONMENT_ID, ExternalizedComputeClusterCreationEvent::getResourceId);
        verifyHeaders();
    }

    @Test
    void triggerExternalizedComputeClusterReinitializationFlowTest() {
        Environment environment = mock(Environment.class);
        when(environment.getResourceCrn()).thenReturn(ENVIRONMENT_CRN);
        when(environment.getResourceName()).thenReturn(ENVIRONMENT_NAME);
        when(environment.getId()).thenReturn(ENVIRONMENT_ID);
        when(eventSender.sendEvent(any(ExternalizedComputeClusterReInitializationEvent.class), any(Event.Headers.class))).thenReturn(flowIdentifier);
        FlowIdentifier result = underTest.triggerExternalizedComputeReinitializationFlow(USER_CRN, environment, true);

        assertThat(result).isSameAs(flowIdentifier);
        ArgumentCaptor<ExternalizedComputeClusterReInitializationEvent> argumentCaptor = ArgumentCaptor.forClass(
                ExternalizedComputeClusterReInitializationEvent.class);
        verify(eventSender).sendEvent(argumentCaptor.capture(), headersCaptor.capture());
        ExternalizedComputeClusterReInitializationEvent event = argumentCaptor.getValue();
        assertThat(event)
                .returns(ExternalizedComputeClusterReInitializationStateSelectors.DEFAULT_COMPUTE_CLUSTER_REINITIALIZATION_START_EVENT.selector(),
                        BaseFlowEvent::selector)
                .returns(ENVIRONMENT_CRN, ExternalizedComputeClusterReInitializationEvent::getResourceCrn)
                .returns(ENVIRONMENT_NAME, ExternalizedComputeClusterReInitializationEvent::getResourceName)
                .returns(ENVIRONMENT_ID, ExternalizedComputeClusterReInitializationEvent::getResourceId)
                .returns(true, ExternalizedComputeClusterReInitializationEvent::isForce);
        verifyHeaders();
    }

    public static Stream<Arguments> triggerLoadBalancerUpdateFlowScenarios() {
        return Stream.of(
                Arguments.of(true, PublicEndpointAccessGateway.ENABLED),
                Arguments.of(false, PublicEndpointAccessGateway.ENABLED),
                Arguments.of(true, PublicEndpointAccessGateway.DISABLED),
                Arguments.of(false, PublicEndpointAccessGateway.DISABLED)
        );
    }

    private void verifyEnvDeleteEvent(boolean forcedExpected, String selectorExpected) {
        EnvDeleteEvent envDeleteEvent = envDeleteEventCaptor.getValue();
        assertThat(envDeleteEvent).isNotNull();
        assertThat(envDeleteEvent.accepted()).isNotNull();
        assertThat(envDeleteEvent.getSelector()).isEqualTo(selectorExpected);
        assertThat(envDeleteEvent.getResourceId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(envDeleteEvent.getResourceName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(envDeleteEvent.isForceDelete()).isEqualTo(forcedExpected);
    }

    private void verifyHeaders() {
        Event.Headers headers = headersCaptor.getValue();
        assertThat(headers).isNotNull();
        assertThat(headers.asMap()).containsOnly(Map.entry(FlowConstants.FLOW_TRIGGER_USERCRN, USER_CRN));
    }

    private EnvironmentView environmentView() {
        EnvironmentView environment = new EnvironmentView();
        environment.setId(ENVIRONMENT_ID);
        environment.setName(ENVIRONMENT_NAME);
        return environment;
    }

}
