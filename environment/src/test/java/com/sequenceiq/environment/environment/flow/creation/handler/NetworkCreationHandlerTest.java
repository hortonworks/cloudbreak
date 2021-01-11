package com.sequenceiq.environment.environment.flow.creation.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.ReflectionUtils;

import reactor.bus.Event;
import reactor.bus.EventBus;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.cloud.model.network.SubnetType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.event.EventSender;

public class NetworkCreationHandlerTest {

    private static final String UNMATCHED_AZ_MSG = "Please provide public subnets in each of the following availability zones:";

    private static final String AZ_1 = "AZ-1";

    private static final String AZ_2 = "AZ-2";

    private static final String ID_1 = "id1";

    private static final String ID_2 = "id2";

    private static final String PUBLIC_ID_1 = "public-id1";

    private static final String PUBLIC_ID_2 = "public-id2";

    private static final String USER_CRN = "crn:cdp:iam:us-west-1:" + UUID.randomUUID() + ":user:" + UUID.randomUUID();

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
    private EntitlementService entitlementService;

    @InjectMocks
    private NetworkCreationHandler underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        Field enabledPlatformsField = ReflectionUtils.findField(NetworkCreationHandler.class, "enabledPlatforms");
        ReflectionUtils.makeAccessible(enabledPlatformsField);
        ReflectionUtils.setField(enabledPlatformsField, underTest, Set.of("AWS", "AZURE"));

        doNothing().when(eventSender).sendEvent(any(), any());
        when(eventBus.notify(any(Object.class), any(Event.class))).thenReturn(null);
        when(entitlementService.publicEndpointAccessGatewayEnabled(any())).thenReturn(true);
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
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(endpointGatewaySubnets);
        when(environmentResourceService.createAndSetNetwork(any(), any(), any(), any(), any())).thenReturn(network);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        assertEquals(2, environmentDto.getNetwork().getEndpointGatewaySubnetMetas().size());
        assertEquals(Set.of(PUBLIC_ID_1, PUBLIC_ID_2), environmentDto.getNetwork().getEndpointGatewaySubnetIds());
    }

    @Test
    public void testWithEndpointGatewayRemovePrivateSubnets() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);
        AwsNetwork network = createNetwork();
        Environment environment = createEnvironment(network);
        Optional<Environment> environmentOptional = Optional.of(environment);

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();
        Map<String, CloudSubnet> endpointGatewaySubnets = createDefaultPublicSubnets();
        endpointGatewaySubnets.putAll(createDefaultPrivateSubnets());

        when(environmentService.findEnvironmentById(any())).thenReturn(environmentOptional);
        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(endpointGatewaySubnets);
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
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(Map.of());
        when(environmentResourceService.createAndSetNetwork(any(), any(), any(), any(), any())).thenReturn(network);

        // Testing that underTest.accept() does not throw a BadRequestException
        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        assertEquals(0, environmentDto.getNetwork().getEndpointGatewaySubnetMetas().size());
    }

    @Test
    public void testWithEndpointGatewayAndNoPublicSubnetsProvided() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);
        AwsNetwork network = createNetwork();
        Environment environment = createEnvironment(network);
        Optional<Environment> environmentOptional = Optional.of(environment);

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();

        when(environmentService.findEnvironmentById(any())).thenReturn(environmentOptional);
        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(Map.of());
        when(environmentResourceService.createAndSetNetwork(any(), any(), any(), any(), any())).thenReturn(network);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        ArgumentCaptor<Event<EnvCreationFailureEvent>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(any(Object.class), eventCaptor.capture());
        Event<EnvCreationFailureEvent> value = eventCaptor.getValue();
        assertTrue(value.getData().getException() instanceof BadRequestException);
        assertTrue(value.getData().getException().getMessage().startsWith(UNMATCHED_AZ_MSG));
    }

    @Test
    public void testWithEndpointGatewayWithMissingPublicSubnets() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);
        AwsNetwork network = createNetwork();
        Environment environment = createEnvironment(network);
        Optional<Environment> environmentOptional = Optional.of(environment);

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();
        subnets.put("id3", createPrivateSubnet("id3", "AZ-3"));
        Map<String, CloudSubnet> endpointGatewaySubnets = createDefaultPublicSubnets();

        when(environmentService.findEnvironmentById(any())).thenReturn(environmentOptional);
        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(endpointGatewaySubnets);
        when(environmentResourceService.createAndSetNetwork(any(), any(), any(), any(), any())).thenReturn(network);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        ArgumentCaptor<Event<EnvCreationFailureEvent>> eventCaptor = ArgumentCaptor.forClass(Event.class);
        verify(eventBus, times(1)).notify(any(Object.class), eventCaptor.capture());
        Event<EnvCreationFailureEvent> value = eventCaptor.getValue();
        assertTrue(value.getData().getException() instanceof BadRequestException);
        assertTrue(value.getData().getException().getMessage().startsWith(UNMATCHED_AZ_MSG));
    }

    @Test
    public void testEndpointGatewayFieldsUnsetIfEntitlementIsMissing() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);
        AwsNetwork network = createNetwork();
        Environment environment = createEnvironment(network);
        Optional<Environment> environmentOptional = Optional.of(environment);

        Map<String, CloudSubnet> subnets = createDefaultPrivateSubnets();
        Map<String, CloudSubnet> endpointGatewaySubnets = createDefaultPublicSubnets();

        when(environmentService.findEnvironmentById(any())).thenReturn(environmentOptional);
        when(cloudNetworkService.retrieveSubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(subnets);
        when(cloudNetworkService.retrieveEndpointGatewaySubnetMetadata(any(EnvironmentDto.class), any())).thenReturn(endpointGatewaySubnets);
        when(environmentResourceService.createAndSetNetwork(any(), any(), any(), any(), any())).thenReturn(network);
        when(entitlementService.publicEndpointAccessGatewayEnabled(any())).thenReturn(false);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        assertNull(environmentDto.getNetwork().getEndpointGatewaySubnetMetas());
        assertEquals(PublicEndpointAccessGateway.DISABLED, environmentDto.getNetwork().getPublicEndpointAccessGateway());
    }

    private EnvironmentDto createEnvironmentDto() {
        NetworkDto networkDto = NetworkDto.builder()
            .build();

        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(123L);
        environmentDto.setName("name");
        environmentDto.setNetwork(networkDto);
        return environmentDto;
    }

    private AwsNetwork createNetwork() {
        AwsNetwork network = new AwsNetwork();
        network.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        network.setRegistrationType(RegistrationType.EXISTING);
        return network;
    }

    private Environment createEnvironment(BaseNetwork network) {
        Environment environment = new Environment();
        environment.setName("name");
        environment.setAccountId("1234");
        environment.setNetwork(network);
        environment.setCloudPlatform("AWS");
        return environment;
    }

    private CloudSubnet createPrivateSubnet(String id, String aZ) {
        return new CloudSubnet(id, "name", aZ, "cidr", true, false, false, SubnetType.PRIVATE);
    }

    private CloudSubnet createPublicSubnet(String id, String aZ) {
        return new CloudSubnet(id, "name", aZ, "cidr", false, true, true, SubnetType.PUBLIC);
    }

    private Map<String, CloudSubnet> createDefaultPrivateSubnets() {
        Map<String, CloudSubnet> subnets = new HashMap<>();
        subnets.put(ID_1, createPrivateSubnet(ID_1, AZ_1));
        subnets.put(ID_2, createPrivateSubnet(ID_2, AZ_2));
        return subnets;
    }

    private Map<String, CloudSubnet> createDefaultPublicSubnets() {
        Map<String, CloudSubnet> subnets = new HashMap<>();
        subnets.put(PUBLIC_ID_1, createPublicSubnet(PUBLIC_ID_1, AZ_1));
        subnets.put(PUBLIC_ID_2, createPublicSubnet(PUBLIC_ID_2, AZ_2));
        return subnets;
    }
}
