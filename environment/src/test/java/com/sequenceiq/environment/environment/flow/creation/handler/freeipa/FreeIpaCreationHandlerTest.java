package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.configuration.SupportedPlatforms;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.v1.TelemetryApiConverter;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;

import reactor.bus.Event;

@ExtendWith(MockitoExtension.class)
public class FreeIpaCreationHandlerTest {

    private static final long ENVIRONMENT_ID = 1L;

    private static final String PARENT_ENVIRONMENT_CRN = "parentEnvCrn";

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String YARN_NETWORK_CIDR = "172.27.0.0/16";

    @Mock
    private EventSender eventSender;

    @Mock
    private EnvironmentService environmentService;

    @Mock
    private FreeIpaService freeIpaService;

    @Mock
    private DnsV1Endpoint dnsV1Endpoint;

    @Mock
    private SupportedPlatforms supportedPlatforms;

    @Mock
    private Map<CloudPlatform, FreeIpaNetworkProvider> freeIpaNetworkProviderMapByCloudPlatform;

    @Mock
    private PollingService<FreeIpaPollerObject> freeIpaPollingService;

    @Mock
    private FreeIpaServerRequestProvider freeIpaServerRequestProvider;

    @Mock
    private TelemetryApiConverter telemetryApiConverter;

    @Mock
    private CloudPlatformConnectors connectors;

    private FreeIpaCreationHandler victim;

    @BeforeEach
    public void initTests() {
        victim = new FreeIpaCreationHandler(
                eventSender,
                environmentService,
                freeIpaService,
                dnsV1Endpoint,
                supportedPlatforms,
                freeIpaNetworkProviderMapByCloudPlatform,
                freeIpaPollingService,
                freeIpaServerRequestProvider,
                telemetryApiConverter,
                connectors,
                Collections.singleton(CloudPlatform.YARN.name()));
    }

    @Test
    public void shouldAttachFreeIpaInCaseOfChildEnvironment() throws Exception {
        DescribeFreeIpaResponse describeFreeIpaResponse = mock(DescribeFreeIpaResponse.class);
        EnvironmentDto environmentDto = aYarnEnvironmentDtoWithParentEnvironment();
        Environment environment = anEnvironmentWithParent();

        ArgumentCaptor<AddDnsZoneForSubnetsRequest> addDnsZoneForSubnetsRequestArgumentCaptor = ArgumentCaptor.forClass(AddDnsZoneForSubnetsRequest.class);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(of(environment));
        when(freeIpaService.describe(PARENT_ENVIRONMENT_CRN)).thenReturn(of(describeFreeIpaResponse));

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<AttachChildEnvironmentRequest> attachChildEnvironmentRequestArgumentCaptor
                = ArgumentCaptor.forClass(AttachChildEnvironmentRequest.class);
        verify(dnsV1Endpoint).addDnsZoneForSubnets(addDnsZoneForSubnetsRequestArgumentCaptor.capture());
        verify(freeIpaService).attachChildEnvironment(attachChildEnvironmentRequestArgumentCaptor.capture());
        verify(eventSender).sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class));

        assertEquals(PARENT_ENVIRONMENT_CRN, addDnsZoneForSubnetsRequestArgumentCaptor.getValue().getEnvironmentCrn());
        assertEquals(Collections.singletonList(YARN_NETWORK_CIDR), addDnsZoneForSubnetsRequestArgumentCaptor.getValue().getSubnets());
        assertEquals(ENVIRONMENT_CRN, attachChildEnvironmentRequestArgumentCaptor.getValue().getChildEnvironmentCrn());
        assertEquals(PARENT_ENVIRONMENT_CRN, attachChildEnvironmentRequestArgumentCaptor.getValue().getParentEnvironmentCrn());
    }

    @Test
    public void shouldNotAttachInCaseOfMissingFreeIpa() throws Exception {
        EnvironmentDto environmentDto = aYarnEnvironmentDtoWithParentEnvironment();
        Environment environment = anEnvironmentWithParent();

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(of(environment));
        when(freeIpaService.describe(PARENT_ENVIRONMENT_CRN)).thenReturn(empty());

        victim.accept(new Event<>(environmentDto));

        verify(dnsV1Endpoint, never()).addDnsZoneForSubnets(any(AddDnsZoneForSubnetsRequest.class));
        verify(freeIpaService, never()).attachChildEnvironment(any(AttachChildEnvironmentRequest.class));
        verify(eventSender).sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class));
    }

    @Test
    public void shouldNotAttachFreeIpaInCaseOfNonSupportedCloudPlatform() throws Exception {
        EnvironmentDto environmentDto = aNonYarnEnvironmentDtoWithParentEnvironment();
        Environment environment = anEnvironmentWithParent();

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(of(environment));

        victim.accept(new Event<>(environmentDto));

        verify(dnsV1Endpoint, never()).addDnsZoneForSubnets(any(AddDnsZoneForSubnetsRequest.class));
        verify(freeIpaService, never()).attachChildEnvironment(any(AttachChildEnvironmentRequest.class));
        verify(eventSender).sendEvent(any(BaseNamedFlowEvent.class), any(Event.Headers.class));
    }

    private EnvironmentDto aYarnEnvironmentDtoWithParentEnvironment() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENVIRONMENT_ID);
        environmentDto.setResourceCrn(ENVIRONMENT_CRN);
        environmentDto.setParentEnvironmentCrn(PARENT_ENVIRONMENT_CRN);
        environmentDto.setCloudPlatform(CloudPlatform.YARN.name());
        environmentDto.setNetwork(NetworkDto.builder()
                .withNetworkCidr(YARN_NETWORK_CIDR)
                .build());

        return environmentDto;
    }

    private EnvironmentDto aNonYarnEnvironmentDtoWithParentEnvironment() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENVIRONMENT_ID);
        environmentDto.setParentEnvironmentCrn(PARENT_ENVIRONMENT_CRN);
        environmentDto.setCloudPlatform(CloudPlatform.AWS.name());

        return environmentDto;
    }

    private Environment anEnvironmentWithParent() {
        Environment environment = new Environment();
        Environment parentEnvironment = new Environment();
        environment.setParentEnvironment(parentEnvironment);

        return environment;
    }
}