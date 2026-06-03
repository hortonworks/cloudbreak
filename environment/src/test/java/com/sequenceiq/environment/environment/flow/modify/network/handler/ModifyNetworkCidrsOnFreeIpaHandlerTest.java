package com.sequenceiq.environment.environment.flow.modify.network.handler;

import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationHandlerSelectors.MODIFY_NETWORK_CIDRS_ON_FREEIPA_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.FAILED_MODIFY_NETWORK_CIDRS_EVENT;
import static com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationStateSelectors.START_MODIFY_NETWORK_CIDRS_DATALAKE_AND_DATAHUBS_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudSubnet;
import com.sequenceiq.cloudbreak.common.event.Selectable;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDtoConverter;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationEvent;
import com.sequenceiq.environment.environment.flow.modify.network.event.EnvNetworkCidrsModificationFailureEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaPollerService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.handler.HandlerEvent;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetIdsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
class ModifyNetworkCidrsOnFreeIpaHandlerTest {
    private static final long ENV_ID = 1L;

    private static final String ENV_NAME = "envName";

    private static final String ENV_CRN = "crn";

    private static final String NETWORK_ID = "vpc-1";

    private static final List<String> NETWORK_CIDRS = List.of("10.84.128.0/17", "10.84.0.0/17");

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private FreeIpaPollerService freeIpaPollerService;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private EnvironmentDtoConverter environmentDtoConverter;

    @Mock
    private DnsV1Endpoint dnsV1Endpoint;

    @InjectMocks
    private ModifyNetworkCidrsOnFreeIpaHandler underTest;

    private HandlerEvent<EnvNetworkCidrsModificationEvent> event;

    @BeforeEach
    void setUp() {
        EnvNetworkCidrsModificationEvent request = EnvNetworkCidrsModificationEvent.builder()
                .withSelector(MODIFY_NETWORK_CIDRS_ON_FREEIPA_EVENT.selector())
                .withResourceId(ENV_ID)
                .withResourceName(ENV_NAME)
                .withResourceCrn(ENV_CRN)
                .withNetworkCidrs(NETWORK_CIDRS)
                .build();
        event = new HandlerEvent<>(new Event<>(request));
    }

    @Test
    void testDoAcceptSuccessTriggersReverseDnsZoneUpdate() {
        Environment environment = new Environment();
        Map<String, CloudSubnet> subnetMetas = new HashMap<>();
        subnetMetas.put("subnet-1", new CloudSubnet());
        subnetMetas.put("subnet-2", new CloudSubnet());
        NetworkDto networkDto = NetworkDto.builder().withNetworkId(NETWORK_ID).withSubnetMetas(subnetMetas).build();
        when(environmentService.findEnvironmentByIdOrThrow(ENV_ID)).thenReturn(environment);
        when(environmentDtoConverter.networkToNetworkDto(environment)).thenReturn(networkDto);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.of(new DescribeFreeIpaResponse()));

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvNetworkCidrsModificationEvent.class, result);
        assertEquals(START_MODIFY_NETWORK_CIDRS_DATALAKE_AND_DATAHUBS_EVENT.name(), result.getSelector());
        verify(freeIpaService).updateNetworkCidrs(ENV_CRN, NETWORK_CIDRS);
        verify(freeIpaPollerService).waitForSaltUpdate(ENV_ID, ENV_CRN);
        ArgumentCaptor<AddDnsZoneForSubnetIdsRequest> requestCaptor = ArgumentCaptor.forClass(AddDnsZoneForSubnetIdsRequest.class);
        verify(dnsV1Endpoint).addDnsZoneForSubnetIds(requestCaptor.capture());
        AddDnsZoneForSubnetIdsRequest sent = requestCaptor.getValue();
        assertEquals(ENV_CRN, sent.getEnvironmentCrn());
        assertEquals(NETWORK_ID, sent.getAddDnsZoneNetwork().getNetworkId());
        assertEquals(subnetMetas.keySet(), sent.getAddDnsZoneNetwork().getSubnetIds());
    }

    @Test
    void testDoAcceptSkipsReverseDnsZoneUpdateWhenFreeIpaNotPresent() {
        Environment environment = new Environment();
        NetworkDto networkDto = NetworkDto.builder().withNetworkId(NETWORK_ID)
                .withSubnetMetas(Map.of("subnet-1", new CloudSubnet())).build();
        when(environmentService.findEnvironmentByIdOrThrow(ENV_ID)).thenReturn(environment);
        when(environmentDtoConverter.networkToNetworkDto(environment)).thenReturn(networkDto);
        when(freeIpaService.describe(ENV_CRN)).thenReturn(Optional.empty());

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvNetworkCidrsModificationEvent.class, result);
        assertEquals(START_MODIFY_NETWORK_CIDRS_DATALAKE_AND_DATAHUBS_EVENT.name(), result.getSelector());
        verify(dnsV1Endpoint, never()).addDnsZoneForSubnetIds(any());
    }

    @Test
    void testDoAcceptFailure() {
        doThrow(new RuntimeException("error")).when(freeIpaService).updateNetworkCidrs(ENV_CRN, NETWORK_CIDRS);

        Selectable result = underTest.doAccept(event);

        assertInstanceOf(EnvNetworkCidrsModificationFailureEvent.class, result);
        assertEquals(FAILED_MODIFY_NETWORK_CIDRS_EVENT.name(), result.getSelector());
        verifyNoMoreInteractions(freeIpaPollerService);
        verify(dnsV1Endpoint, never()).addDnsZoneForSubnetIds(any());
    }
}