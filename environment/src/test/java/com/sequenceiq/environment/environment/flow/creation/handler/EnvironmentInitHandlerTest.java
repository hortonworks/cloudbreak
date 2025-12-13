package com.sequenceiq.environment.environment.flow.creation.handler;

import static com.sequenceiq.cloudbreak.cloud.model.Region.region;
import static com.sequenceiq.environment.environment.flow.creation.event.EnvCreationStateSelectors.FAILED_ENV_CREATION_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.auth.altus.UmsVirtualGroupRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.domain.PemBasedEnvironmentDomainProvider;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.AzureNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.GcpNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.flow.reactor.api.event.EventSender;

@ExtendWith(MockitoExtension.class)
class EnvironmentInitHandlerTest {

    private static final long ENV_ID = 1L;

    private static final String CRN = "crn";

    private static final String CREATOR = "creator";

    private static final String REGION = "region";

    private static final String LOCATION = "location";

    private static final String LOCATION_DISPLAY_NAME = "locationDisplayName";

    private static final String ENV_NAME = "envName";

    private static final String ACCOUNT_ID = "accountId";

    private final EventSender eventSender = mock(EventSender.class);

    private final EnvironmentService environmentService = mock(EnvironmentService.class);

    private final EnvironmentNetworkService environmentNetworkService = mock(EnvironmentNetworkService.class);

    private final EventBus eventBus = mock(EventBus.class);

    private final VirtualGroupService virtualGroupService = mock(VirtualGroupService.class);

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap = mock(HashMap.class);

    private final PemBasedEnvironmentDomainProvider domainProvider = mock(PemBasedEnvironmentDomainProvider.class);

    private final EnvironmentInitHandler environmentInitHandler
            = new EnvironmentInitHandler(eventSender, environmentService, environmentNetworkService,
            eventBus, virtualGroupService, environmentNetworkConverterMap, domainProvider);

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE", "GCP"})
    void testInitFailure(CloudPlatform cloudPlatform) {
        EnvironmentDto dto = getEnvironmentDto();
        Event<EnvironmentDto> event = new Event<>(dto);
        Environment environment = getEnvironment(cloudPlatform);

        when(environmentService.findEnvironmentById(dto.getId())).thenReturn(Optional.of(environment));
        when(virtualGroupService.createVirtualGroups(anyString(), anyString())).thenThrow(IllegalStateException.class);

        environmentInitHandler.accept(event);

        verify(eventBus, times(1)).notify(eq(FAILED_ENV_CREATION_EVENT.name()), any(Event.class));
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE", "GCP"})
    void testInitSuccess(CloudPlatform cloudPlatform) {
        EnvironmentDto dto = getEnvironmentDto();
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .build();
        dto.setNetwork(networkDto);
        Event<EnvironmentDto> event = new Event<>(dto);
        Environment environment = getEnvironment(cloudPlatform);
        EnvironmentNetworkConverter environmentNetworkConverter = mock(EnvironmentNetworkConverter.class);
        Network network = mock(Network.class);
        String cidr = "10.0.0.0/16";
        String availabilityZone = "us-west2-a";

        when(environmentNetworkConverterMap.get(any(CloudPlatform.class))).thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convertToNetwork(environment.getNetwork())).thenReturn(network);
        when(environmentNetworkService.getNetworkCidr(any(), anyString(), any())).thenReturn(new NetworkCidr(cidr));
        when(environmentService.findEnvironmentById(dto.getId())).thenReturn(Optional.of(environment));
        Map<UmsVirtualGroupRight, String> virtualGroups = Map.of(UmsVirtualGroupRight.HBASE_ADMIN, "apple1", UmsVirtualGroupRight.ENVIRONMENT_ACCESS,
                "apple2");
        when(virtualGroupService.createVirtualGroups(ACCOUNT_ID, CRN)).thenReturn(virtualGroups);
        CloudRegions cloudRegions = new CloudRegions(Map.of(region(REGION), List.of(AvailabilityZone.availabilityZone(availabilityZone))),
                Collections.emptyMap(),
                Collections.emptyMap(), "apple", true);
        when(environmentService.getRegionsByEnvironment(environment)).thenReturn(cloudRegions);
        ReflectionTestUtils.setField(environmentInitHandler, "maxAvailabilityZones", 3);

        environmentInitHandler.accept(event);

        assertEquals(cidr, environment.getNetwork().getNetworkCidr());
        verify(environmentNetworkService, times(1))
                .getNetworkCidr(eq(network), eq(cloudPlatform.name()), eq(environment.getCredential()));
        verify(environmentService, times(0)).setAdminGroupName(environment, null);
        verify(environmentService, times(1)).assignEnvironmentAdminRole(CREATOR, CRN);
        verify(environmentService, times(1)).setLocation(environment, environment.getRegionWrapper(), cloudRegions);
        verify(environmentService, times(1)).setRegions(environment, environment.getRegionWrapper().getRegions(), cloudRegions);
        if (cloudPlatform == CloudPlatform.GCP) {
            verify(environmentNetworkConverter, times(1)).updateAvailabilityZones(environment.getNetwork(), Set.of(availabilityZone));
        }
        verify(environmentService, times(1)).save(environment);
        verify(eventSender, times(1)).sendEvent(any(), any());
    }

    private EnvironmentDto getEnvironmentDto() {
        return EnvironmentDto.builder()
                    .withId(ENV_ID)
                    .withResourceCrn(CRN)
                    .withName(ENV_NAME)
                    .build();
    }

    private Environment getEnvironment(CloudPlatform cloudPlatform) {
        Environment environment = new Environment();
        environment.setId(ENV_ID);
        environment.setResourceCrn(CRN);
        environment.setCreator(CREATOR);
        Region region = new Region();
        region.setName(REGION);
        environment.setRegions(Set.of(region));
        environment.setLocationDisplayName(LOCATION_DISPLAY_NAME);
        environment.setLocation(LOCATION);
        environment.setAccountId(ACCOUNT_ID);
        BaseNetwork baseNetwork = getEnvironmentNetwork(cloudPlatform);
        baseNetwork.setRegistrationType(RegistrationType.EXISTING);
        environment.setNetwork(baseNetwork);
        environment.setCloudPlatform(cloudPlatform.name());
        environment.setCredential(new Credential());
        return environment;
    }

    private BaseNetwork getEnvironmentNetwork(CloudPlatform cloudPlatform) {
        switch (cloudPlatform) {
            case AWS:
                return new AwsNetwork();
            case AZURE:
                return new AzureNetwork();
            case GCP:
                return new GcpNetwork();
            default:
                return null;
        }
    }
}