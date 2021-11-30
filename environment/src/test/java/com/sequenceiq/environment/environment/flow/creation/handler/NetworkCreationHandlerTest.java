package com.sequenceiq.environment.environment.flow.creation.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.network.NetworkMetadataValidationService;
import com.sequenceiq.environment.environment.service.network.NetworkTest;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.EventBus;

public class NetworkCreationHandlerTest extends NetworkTest {

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentResourceService environmentResourceService;

    @Mock
    private CloudNetworkService cloudNetworkService;

    @Mock
    private EventBus eventBus;

    @Mock
    private EventSender eventSender;

    @Mock
    private NetworkMetadataValidationService networkMetadataValidationService;

    @InjectMocks
    private NetworkCreationHandler underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Field enabledPlatformsField = ReflectionUtils.findField(NetworkCreationHandler.class, "enabledPlatforms");
        ReflectionUtils.makeAccessible(enabledPlatformsField);
        ReflectionUtils.setField(enabledPlatformsField, underTest, Set.of("AWS", "AZURE"));

        when(eventBus.notify(any(Object.class), any(Event.class))).thenReturn(null);
    }

    @Test
    public void testWithEndpointGatewayAndProvidedSubnets() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);
        AwsNetwork network = createNetwork();
        Environment environment = createEnvironment(network);
        Optional<Environment> environmentOptional = Optional.of(environment);

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();
        Map<String, CloudSubnet> endpointGatewaySubnets = createDefaultPublicSubnets();

        when(environmentService.findEnvironmentById(any())).thenReturn(environmentOptional);
        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(networkMetadataValidationService.getEndpointGatewaySubnetMetadata(any(), any())).thenReturn(endpointGatewaySubnets);
        when(environmentResourceService.createAndSetNetwork(any(), any(), any(), any(), any())).thenReturn(network);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        assertEquals(2, environmentDto.getNetwork().getEndpointGatewaySubnetMetas().size());
        assertEquals(Set.of(PUBLIC_ID_1, PUBLIC_ID_2), environmentDto.getNetwork().getEndpointGatewaySubnetIds());
    }

    @Test
    public void testWithEndpointGatewayAndEnvironmentSubnets() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);
        AwsNetwork network = createNetwork();
        Environment environment = createEnvironment(network);
        Optional<Environment> environmentOptional = Optional.of(environment);

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();
        subnets.putAll(createDefaultPublicSubnets());

        when(environmentService.findEnvironmentById(any())).thenReturn(environmentOptional);
        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(networkMetadataValidationService.getEndpointGatewaySubnetMetadata(any(), any())).thenReturn(Map.of());
        when(environmentResourceService.createAndSetNetwork(any(), any(), any(), any(), any())).thenReturn(network);

        // Testing that underTest.accept() does not throw a BadRequestException
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        assertEquals(0, environmentDto.getNetwork().getEndpointGatewaySubnetMetas().size());
    }

    @Test
    public void testHandleValidationFailure() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);
        AwsNetwork network = createNetwork();
        Environment environment = createEnvironment(network);
        Optional<Environment> environmentOptional = Optional.of(environment);

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();

        when(environmentService.findEnvironmentById(any())).thenReturn(environmentOptional);
        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(networkMetadataValidationService.getEndpointGatewaySubnetMetadata(any(), any())).thenThrow(new BadRequestException("failure"));
        when(environmentResourceService.createAndSetNetwork(any(), any(), any(), any(), any())).thenReturn(network);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        ArgumentCaptor<Event<EnvCreationFailureEvent>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(any(Object.class), eventCaptor.capture());
        Event<EnvCreationFailureEvent> value = eventCaptor.getValue();
        assertTrue(value.getData().getException() instanceof BadRequestException);
    }
}
