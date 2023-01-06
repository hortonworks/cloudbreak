package com.sequenceiq.environment.environment.flow.loadbalancer.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import com.sequenceiq.environment.environment.service.LoadBalancerPollerService;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class LoadBalancerStackUpdateHandlerTest {

    private static final String ENV_CRN = "someCrnValue";

    private static final String ENV_NAME = "envName";

    private static final long ENV_ID = 100L;

    private static final String ACCOUNT_ID = "account";

    @Mock
    private LoadBalancerPollerService loadBalancerPollerService;

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

    @Captor
    private ArgumentCaptor<LoadBalancerUpdateEvent> updateEventCaptor;

    @Captor
    private ArgumentCaptor<LoadBalancerUpdateFailedEvent> failedEventCaptor;

    @InjectMocks
    private LoadBalancerStackUpdateHandler underTest;

    private Environment environment;

    @BeforeEach
    void setUp() {
        lenient().when(environmentLbDtoEvent.getData()).thenReturn(environmentLoadBalancerDto);
        lenient().when(environmentLbDtoEvent.getHeaders()).thenReturn(eventHeaders);
        lenient().when(environmentLoadBalancerDto.getEnvironmentDto()).thenReturn(environmentDto);
        lenient().when(environmentDto.getResourceCrn()).thenReturn(ENV_CRN);
        lenient().when(environmentDto.getId()).thenReturn(ENV_ID);
        lenient().when(environmentDto.getResourceId()).thenReturn(ENV_ID);
        lenient().when(environmentDto.getName()).thenReturn(ENV_NAME);
        lenient().when(environmentDto.getNetwork()).thenReturn(networkDto);
        lenient().when(environmentDto.getAccountId()).thenReturn(ACCOUNT_ID);
        BaseNetwork network = new BaseNetwork() {
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
        assertThat(underTest.selector()).isEqualTo(LoadBalancerUpdateHandlerSelectors.STACK_UPDATE_HANDLER_EVENT.name());
    }

    @Test
    void pollerFailure() {
        doThrow(new IllegalStateException("not found")).when(loadBalancerPollerService).updateStackWithLoadBalancer(
                nullable(String.class), nullable(String.class), nullable(String.class), nullable(PublicEndpointAccessGateway.class), anyBoolean());

        underTest.accept(environmentLbDtoEvent);
        verifyLoadBalancerStackUpdateFailedEvent(environmentLbDtoEvent, "not found");
    }

    @ParameterizedTest
    @EnumSource(PublicEndpointAccessGateway.class)
    void accept(PublicEndpointAccessGateway peag) {
        when(environmentLoadBalancerDto.getEndpointAccessGateway()).thenReturn(peag);
        Set<String> subnetIds = Set.of("subnet1", "subnet2");
        when(environmentLoadBalancerDto.getEndpointGatewaySubnetIds()).thenReturn(subnetIds);
        underTest.accept(environmentLbDtoEvent);
        verify(loadBalancerPollerService)
                .updateStackWithLoadBalancer(eq(ACCOUNT_ID), eq(ENV_CRN), eq(ENV_NAME), eq(peag), eq(true));
        verifyLoadBalancerStackUpdateEvent(environmentLbDtoEvent, peag, subnetIds);
    }

    private void verifyLoadBalancerStackUpdateEvent(Event<EnvironmentLoadBalancerDto> event, PublicEndpointAccessGateway peag, Set<String> subnets) {
        Event.Headers headers = event.getHeaders();
        verify(eventSender).sendEvent(updateEventCaptor.capture(), eq(headers));
        assertThat(updateEventCaptor.getValue())
                .returns(LoadBalancerUpdateStateSelectors.FINISH_LOAD_BALANCER_UPDATE_EVENT.selector(), LoadBalancerUpdateEvent::selector)
                .returns(ENV_ID, LoadBalancerUpdateEvent::getResourceId)
                .returns(ENV_NAME, LoadBalancerUpdateEvent::getResourceName)
                .returns(ENV_CRN, LoadBalancerUpdateEvent::getResourceCrn)
                .returns(environmentDto, LoadBalancerUpdateEvent::getEnvironmentDto)
                .returns(peag, LoadBalancerUpdateEvent::getEndpointAccessGateway)
                .returns(subnets, LoadBalancerUpdateEvent::getSubnetIds);
    }

    private void verifyLoadBalancerStackUpdateFailedEvent(Event<EnvironmentLoadBalancerDto> event, String message) {
        Event.Headers headers = event.getHeaders();
        verify(eventSender).sendEvent(failedEventCaptor.capture(), eq(headers));
        assertThat(failedEventCaptor.getValue())
                .returns(LoadBalancerUpdateStateSelectors.FAILED_LOAD_BALANCER_UPDATE_EVENT.selector(), LoadBalancerUpdateFailedEvent::selector)
                .returns(EnvironmentStatus.LOAD_BALANCER_STACK_UPDATE_FAILED, LoadBalancerUpdateFailedEvent::getEnvironmentStatus)
                .returns(message, e -> e.getException().getMessage());
    }
}
