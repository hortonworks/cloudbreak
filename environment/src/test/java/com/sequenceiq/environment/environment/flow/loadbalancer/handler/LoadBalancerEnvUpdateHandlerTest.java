package com.sequenceiq.environment.environment.flow.loadbalancer.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateFailedEvent;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateHandlerSelectors;
import com.sequenceiq.environment.environment.flow.loadbalancer.event.LoadBalancerUpdateStateSelectors;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.network.NetworkMetadataValidationService;
import com.sequenceiq.environment.network.NetworkService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class LoadBalancerEnvUpdateHandlerTest {

    private static final String ENV_CRN = "someCrnValue";

    private static final String ENV_NAME = "envName";

    private static final long ENV_ID = 100L;

    @Mock
    private NetworkMetadataValidationService networkValidationService;

    @Mock
    private NetworkService networkService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private EventSender eventSender;

    @Mock
    private Event<EnvironmentLoadBalancerDto> environmentLbDtoEvent;

    @Mock
    private EnvironmentLoadBalancerDto environmentLoadBalancerDto;

    @Mock
    private EnvironmentDto environmentDto;

    @Mock
    private Event.Headers eventHeaders;

    @Mock
    private NetworkDto networkDto;

    private BaseNetwork network;

    @Captor
    private ArgumentCaptor<LoadBalancerUpdateEvent> updateEventCaptor;

    @Captor
    private ArgumentCaptor<LoadBalancerUpdateFailedEvent> failedEventCaptor;

    @Captor
    private ArgumentCaptor<BaseNetwork> networkCaptor;

    @InjectMocks
    private LoadBalancerEnvUpdateHandler underTest;

    private Environment environment;

    @BeforeEach
    void setUp() {
        lenient().when(environmentLbDtoEvent.getData()).thenReturn(environmentLoadBalancerDto);
        lenient().when(environmentLbDtoEvent.getHeaders()).thenReturn(eventHeaders);
        lenient().when(environmentLoadBalancerDto.getEnvironmentDto()).thenReturn(environmentDto);
        lenient().when(environmentDto.getResourceCrn()).thenReturn(ENV_CRN);
        lenient().when(environmentDto.getId()).thenReturn(ENV_ID);
        lenient().when(environmentDto.getResourceId()).thenReturn(ENV_ID);
        lenient().when(environmentDto.getNetwork()).thenReturn(networkDto);
        network = new BaseNetwork() {
            @Override
            public String getNetworkId() {
                return "networkId";
            }

            @Override
            public String toString() {
                return super.toString();
            }
        };
        environment = new Environment();
        environment.setId(ENV_ID);
        environment.setName(ENV_NAME);
        environment.setResourceCrn(ENV_CRN);
        environment.setNetwork(network);
    }

    @Test
    void selector() {
        assertThat(underTest.selector()).isEqualTo(LoadBalancerUpdateHandlerSelectors.ENVIRONMENT_UPDATE_HANDLER_EVENT.name());
    }

    @Test
    void noEnvironment() {
        when(environmentService.findEnvironmentByIdOrThrow(ENV_ID)).thenThrow(new IllegalStateException("not found"));

        underTest.accept(environmentLbDtoEvent);
        verifyLoadBalancerEnvUpdateFailedEvent(environmentLbDtoEvent, "not found");
    }

    @ParameterizedTest(name = "PublicEndpointAccessGateway = {0} Subnets = {1} Targeting enabled = {2}")
    @MethodSource("noLbScenario")
    void acceptNoLoadBalancerNeeded(PublicEndpointAccessGateway peag, Set<String> subnets, boolean targetingEnabled) {
        when(environmentService.findEnvironmentByIdOrThrow(ENV_ID)).thenReturn(environment);
        when(environmentLoadBalancerDto.getEndpointAccessGateway()).thenReturn(peag);
        when(environmentLoadBalancerDto.getEndpointGatewaySubnetIds()).thenReturn(subnets);
        when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(any())).thenReturn(targetingEnabled);
        underTest.accept(environmentLbDtoEvent);

        verifyNoInteractions(networkService);
        verifyLoadBalancerEnvUpdateEvent(environmentLbDtoEvent, peag, subnets);
    }

    public static Stream<Arguments> noLbScenario() {
        return Stream.of(
                Arguments.of(null, Set.of("subnet1", "subnet2"), false),
                Arguments.of(PublicEndpointAccessGateway.DISABLED, Set.of("subnet1", "subnet2"), false),
                Arguments.of(PublicEndpointAccessGateway.DISABLED, Set.of(), true),
                Arguments.of(PublicEndpointAccessGateway.DISABLED, null, true)
        );
    }

    @ParameterizedTest(name = "PublicEndpointAccessGateway = {0} Subnets = {1} Targeting enabled = {2}")
    @MethodSource("lbScenario")
    void acceptLoadBalancerNeeded(PublicEndpointAccessGateway peag, Set<String> subnets, boolean targetingEnabled) {
        when(environmentService.findEnvironmentByIdOrThrow(ENV_ID)).thenReturn(environment);
        when(environmentLoadBalancerDto.getEndpointAccessGateway()).thenReturn(peag);
        when(environmentLoadBalancerDto.getEndpointGatewaySubnetIds()).thenReturn(subnets);
        lenient().when(entitlementService.isTargetingSubnetsForEndpointAccessGatewayEnabled(any())).thenReturn(targetingEnabled);
        Map<String, CloudSubnet> cloudSubnets = Map.of("subnet1",
                new CloudSubnet.Builder()
                    .id("sn1")
                    .name("name1")
                    .build(),
                "subnet2",
                new CloudSubnet.Builder()
                    .id("sn2")
                    .name("name2")
                    .build());
        when(networkValidationService.getEndpointGatewaySubnetMetadata(environment, environmentDto)).thenReturn(cloudSubnets);
        underTest.accept(environmentLbDtoEvent);

        verify(networkService).save(networkCaptor.capture());
        BaseNetwork network = networkCaptor.getValue();
        assertThat(network.getPublicEndpointAccessGateway()).isEqualTo(peag);
        assertThat(network.getEndpointGatewaySubnetMetas()).isEqualTo(cloudSubnets);
        verifyLoadBalancerEnvUpdateEvent(environmentLbDtoEvent, peag, subnets);
    }

    public static Stream<Arguments> lbScenario() {
        return Stream.of(
                Arguments.of(PublicEndpointAccessGateway.ENABLED, Set.of(), false),
                Arguments.of(PublicEndpointAccessGateway.ENABLED, Set.of(), true),
                Arguments.of(PublicEndpointAccessGateway.ENABLED, Set.of("subnet1", "subnet2"), true),
                Arguments.of(PublicEndpointAccessGateway.DISABLED, Set.of("subnet1", "subnet2"), true),
                Arguments.of(null, Set.of("subnet1", "subnet2"), true)
        );
    }

    private void verifyLoadBalancerEnvUpdateEvent(Event<EnvironmentLoadBalancerDto> event, PublicEndpointAccessGateway peag, Set<String> subnets) {
        Event.Headers headers = event.getHeaders();
        verify(eventSender).sendEvent(updateEventCaptor.capture(), eq(headers));
        assertThat(updateEventCaptor.getValue())
                .returns(LoadBalancerUpdateStateSelectors.LOAD_BALANCER_STACK_UPDATE_EVENT.selector(), LoadBalancerUpdateEvent::selector)
                .returns(ENV_ID, LoadBalancerUpdateEvent::getResourceId)
                .returns(ENV_NAME, LoadBalancerUpdateEvent::getResourceName)
                .returns(ENV_CRN, LoadBalancerUpdateEvent::getResourceCrn)
                .returns(environment, LoadBalancerUpdateEvent::getEnvironment)
                .returns(environmentDto, LoadBalancerUpdateEvent::getEnvironmentDto)
                .returns(peag, LoadBalancerUpdateEvent::getEndpointAccessGateway)
                .returns(subnets, LoadBalancerUpdateEvent::getSubnetIds);
    }

    private void verifyLoadBalancerEnvUpdateFailedEvent(Event<EnvironmentLoadBalancerDto> event, String message) {
        Event.Headers headers = event.getHeaders();
        verify(eventSender).sendEvent(failedEventCaptor.capture(), eq(headers));
        assertThat(failedEventCaptor.getValue())
                .returns(LoadBalancerUpdateStateSelectors.FAILED_LOAD_BALANCER_UPDATE_EVENT.selector(), LoadBalancerUpdateFailedEvent::selector)
                .returns(EnvironmentStatus.LOAD_BALANCER_ENV_UPDATE_FAILED, LoadBalancerUpdateFailedEvent::getEnvironmentStatus)
                .returns(message, e -> e.getException().getMessage());
    }
}
