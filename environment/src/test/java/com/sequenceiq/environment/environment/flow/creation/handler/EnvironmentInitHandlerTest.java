package com.sequenceiq.environment.environment.flow.creation.handler;

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
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.UmsRight;
import com.sequenceiq.cloudbreak.auth.altus.VirtualGroupService;
import com.sequenceiq.cloudbreak.cloud.model.CloudRegions;
import com.sequenceiq.cloudbreak.cloud.model.Network;
import com.sequenceiq.cloudbreak.cloud.network.NetworkCidr;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.domain.PemBasedEnvironmentDomainProvider;
import com.sequenceiq.environment.network.EnvironmentNetworkService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dao.domain.RegistrationType;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.flow.reactor.api.event.EventSender;

import reactor.bus.Event;
import reactor.bus.EventBus;

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

    private final EventSender eventSender = Mockito.mock(EventSender.class);

    private final EnvironmentService environmentService = Mockito.mock(EnvironmentService.class);

    private final EnvironmentNetworkService environmentNetworkService = Mockito.mock(EnvironmentNetworkService.class);

    private final EventBus eventBus = Mockito.mock(EventBus.class);

    private final VirtualGroupService virtualGroupService = Mockito.mock(VirtualGroupService.class);

    private final Map<CloudPlatform, EnvironmentNetworkConverter> environmentNetworkConverterMap = Mockito.mock(HashMap.class);

    private final PemBasedEnvironmentDomainProvider domainProvider = Mockito.mock(PemBasedEnvironmentDomainProvider.class);

    private final EnvironmentInitHandler environmentInitHandler
            = new EnvironmentInitHandler(eventSender, environmentService, environmentNetworkService,
            eventBus, virtualGroupService, environmentNetworkConverterMap, domainProvider);

    @Test
    void testInitFailure() {
        EnvironmentDto dto = getEnvironmentDto();
        Event<EnvironmentDto> event = new Event<>(dto);
        Environment environment = getEnvironment();

        when(environmentService.findEnvironmentById(dto.getId())).thenReturn(Optional.of(environment));
        when(virtualGroupService.createVirtualGroups(anyString(), anyString())).thenThrow(IllegalStateException.class);

        environmentInitHandler.accept(event);

        verify(eventBus, times(1)).notify(eq(FAILED_ENV_CREATION_EVENT.name()), any(Event.class));
    }

    @Test
    void testInitSuccess() {
        EnvironmentDto dto = getEnvironmentDto();
        NetworkDto networkDto = NetworkDto.builder()
                .withRegistrationType(RegistrationType.EXISTING)
                .build();
        dto.setNetwork(networkDto);
        Event<EnvironmentDto> event = new Event<>(dto);
        Environment environment = getEnvironment();
        EnvironmentNetworkConverter environmentNetworkConverter = mock(EnvironmentNetworkConverter.class);
        Network network = mock(Network.class);
        String cidr = "10.0.0.0/16";

        when(environmentNetworkConverterMap.get(any(CloudPlatform.class))).thenReturn(environmentNetworkConverter);
        when(environmentNetworkConverter.convertToNetwork(environment.getNetwork())).thenReturn(network);
        when(environmentNetworkService.getNetworkCidr(any(), anyString(), any())).thenReturn(new NetworkCidr(cidr));
        when(environmentService.findEnvironmentById(dto.getId())).thenReturn(Optional.of(environment));
        Map<UmsRight, String> virtualGroups = Map.of(UmsRight.HBASE_ADMIN, "apple1", UmsRight.ENVIRONMENT_ACCESS, "apple2");
        when(virtualGroupService.createVirtualGroups(ACCOUNT_ID, CRN)).thenReturn(virtualGroups);
        CloudRegions cloudRegions = new CloudRegions(Collections.emptyMap(), Collections.emptyMap(),
                Collections.emptyMap(), "apple", true);
        when(environmentService.getRegionsByEnvironment(environment)).thenReturn(cloudRegions);

        environmentInitHandler.accept(event);

        assertEquals(cidr, environment.getNetwork().getNetworkCidr());
        verify(environmentNetworkService, times(1))
                .getNetworkCidr(eq(network), eq(CloudPlatform.AWS.name()), eq(environment.getCredential()));
        verify(environmentService, times(0)).setAdminGroupName(environment, null);
        verify(environmentService, times(1)).assignEnvironmentAdminRole(CREATOR, CRN);
        verify(environmentService, times(1)).setLocation(environment, environment.getRegionWrapper(), cloudRegions);
        verify(environmentService, times(1)).setRegions(environment, environment.getRegionWrapper().getRegions(), cloudRegions);
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

    private Environment getEnvironment() {
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
        BaseNetwork baseNetwork = new AwsNetwork();
        baseNetwork.setRegistrationType(RegistrationType.EXISTING);
        environment.setNetwork(baseNetwork);
        environment.setCloudPlatform(CloudPlatform.AWS.name());
        environment.setCredential(new Credential());
        return environment;
    }
}