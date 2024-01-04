package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.environment.environment.service.EnvironmentTestData.NETWORK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import jakarta.ws.rs.BadRequestException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.ReflectionUtils;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentResourceService;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.network.NetworkMetadataValidationService;
import com.sequenceiq.environment.environment.service.network.NetworkTest;
import com.sequenceiq.environment.network.CloudNetworkService;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.flow.reactor.api.event.EventSender;

public class NetworkCreationHandlerTest extends NetworkTest {

    private static final String RESOURCE_REFERENCE = "aReference";

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

    @Mock
    private EnvironmentNetworkService environmentNetworkService;

    @InjectMocks
    private NetworkCreationHandler underTest;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);

        Field enabledPlatformsField = ReflectionUtils.findField(NetworkCreationHandler.class, "enabledPlatforms");
        ReflectionUtils.makeAccessible(enabledPlatformsField);
        ReflectionUtils.setField(enabledPlatformsField, underTest, Set.of("AWS", "AZURE"));
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

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testWithAzureSpecificResources(boolean flexibleServerDnsZoneCreated) {
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setCloudPlatform(CloudConstants.AZURE);
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);
        AzureNetwork network = getAzureNetwork();
        Environment environment = createEnvironment(network);
        Optional<Environment> environmentOptional = Optional.of(environment);

        when(environmentNetworkService.createProviderSpecificNetworkResources(eq(environmentDto), eq(network))).thenReturn(flexibleServerDnsZoneCreated
                ? List.of(buildResource(ResourceType.AZURE_NETWORK),
                buildResource(ResourceType.AZURE_PRIVATE_DNS_ZONE, "test.flexible.postgres.database.azure.com"),
                buildResource(ResourceType.AZURE_VIRTUAL_NETWORK_LINK))
                : List.of(buildResource(ResourceType.AZURE_NETWORK),
                buildResource(ResourceType.AZURE_PRIVATE_DNS_ZONE, "test.privatelink.postgres.database.azure.com"),
                buildResource(ResourceType.AZURE_VIRTUAL_NETWORK_LINK)));
        when(environmentService.findEnvironmentById(any())).thenReturn(environmentOptional);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        String databasePrivateDnsZoneId = ((AzureNetwork) environment.getNetwork()).getDatabasePrivateDnsZoneId();
        if (flexibleServerDnsZoneCreated) {
            assertEquals("test.flexible.postgres.database.azure.com", databasePrivateDnsZoneId);
        } else {
            assertNull(databasePrivateDnsZoneId);
        }
    }

    @Test
    public void testWithAzureSpecificResourcesNoDnsZone() {
        EnvironmentDto environmentDto = createEnvironmentDto();
        environmentDto.setCloudPlatform(CloudConstants.AZURE);
        Event<EnvironmentDto> environmentDtoEvent = Event.wrap(environmentDto);
        AzureNetwork network = getAzureNetwork();
        Environment environment = createEnvironment(network);
        Optional<Environment> environmentOptional = Optional.of(environment);

        when(environmentNetworkService.createProviderSpecificNetworkResources(eq(environmentDto), eq(network))).thenReturn(
                List.of(buildResource(ResourceType.AZURE_NETWORK),
                buildResource(ResourceType.AZURE_VIRTUAL_NETWORK_LINK)));
        when(environmentService.findEnvironmentById(any())).thenReturn(environmentOptional);

        ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.accept(environmentDtoEvent));

        String databasePrivateDnsZoneId = ((AzureNetwork) environment.getNetwork()).getDatabasePrivateDnsZoneId();
        assertNull(databasePrivateDnsZoneId);
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
        verify(eventBus, times(1)).notify(any(), eventCaptor.capture());
        Event<EnvCreationFailureEvent> value = eventCaptor.getValue();
        assertTrue(value.getData().getException() instanceof BadRequestException);
    }

    private AzureNetwork getAzureNetwork() {
        AzureNetwork azureNetwork = new AzureNetwork();
        azureNetwork.setNetworkId(NETWORK_ID);
        return azureNetwork;
    }

    private CloudResource buildResource(ResourceType resourceType) {
        return buildResource(resourceType, RESOURCE_REFERENCE);
    }

    private CloudResource buildResource(ResourceType resourceType, String reference) {
        return CloudResource.builder()
                .withType(resourceType)
                .withReference(reference)
                .withName("aName")
                .withStatus(CommonStatus.CREATED)
                .withParameters(Map.of())
                .build();
    }
}
