package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.sequenceiq.environment.environment.v1.converter.BackupConverter;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.configuration.SupportedPlatforms;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.v1.converter.TelemetryApiConverter;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.event.BaseNamedFlowEvent;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

import reactor.bus.Event;
import reactor.bus.Event.Headers;
import reactor.bus.EventBus;

@ExtendWith(MockitoExtension.class)
public class FreeIpaCreationHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaCreationHandlerTest.class);

    private static final long ENVIRONMENT_ID = 1L;

    private static final String PARENT_ENVIRONMENT_CRN = "parentEnvCrn";

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String YARN_NETWORK_CIDR = "172.27.0.0/16";

    private static final Set<PollingResult> UNSUCCESSFUL_POLLING_RESULTS = Set.of(PollingResult.FAILURE, PollingResult.EXIT, PollingResult.TIMEOUT);

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String IMAGE_ID = "image id";

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
    private BackupConverter backupConverter;

    @Mock
    private CloudPlatformConnectors connectors;

    @Mock
    private EventBus eventBus;

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
                backupConverter,
                connectors,
                eventBus, Collections.singleton(CloudPlatform.YARN.name()));
    }

    @Test
    public void shouldAttachFreeIpaInCaseOfChildEnvironment() throws Exception {
        DescribeFreeIpaResponse describeFreeIpaResponse = mock(DescribeFreeIpaResponse.class);
        EnvironmentDto environmentDto = aYarnEnvironmentDtoWithParentEnvironment();
        Environment environment = anEnvironmentWithParent();

        ArgumentCaptor<AddDnsZoneForSubnetsRequest> addDnsZoneForSubnetsRequestArgumentCaptor = ArgumentCaptor.forClass(AddDnsZoneForSubnetsRequest.class);
        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(freeIpaService.describe(PARENT_ENVIRONMENT_CRN)).thenReturn(Optional.of(describeFreeIpaResponse));

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<AttachChildEnvironmentRequest> attachChildEnvironmentRequestArgumentCaptor
                = ArgumentCaptor.forClass(AttachChildEnvironmentRequest.class);
        verify(dnsV1Endpoint).addDnsZoneForSubnets(addDnsZoneForSubnetsRequestArgumentCaptor.capture());
        verify(freeIpaService).attachChildEnvironment(attachChildEnvironmentRequestArgumentCaptor.capture());
        verify(eventSender).sendEvent(any(BaseNamedFlowEvent.class), any(Headers.class));

        assertEquals(PARENT_ENVIRONMENT_CRN, addDnsZoneForSubnetsRequestArgumentCaptor.getValue().getEnvironmentCrn());
        assertEquals(Collections.singletonList(YARN_NETWORK_CIDR), addDnsZoneForSubnetsRequestArgumentCaptor.getValue().getSubnets());
        assertEquals(ENVIRONMENT_CRN, attachChildEnvironmentRequestArgumentCaptor.getValue().getChildEnvironmentCrn());
        assertEquals(PARENT_ENVIRONMENT_CRN, attachChildEnvironmentRequestArgumentCaptor.getValue().getParentEnvironmentCrn());
    }

    @Test
    public void shouldNotAttachInCaseOfMissingFreeIpa() throws Exception {
        EnvironmentDto environmentDto = aYarnEnvironmentDtoWithParentEnvironment();
        Environment environment = anEnvironmentWithParent();

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(freeIpaService.describe(PARENT_ENVIRONMENT_CRN)).thenReturn(Optional.empty());

        victim.accept(new Event<>(environmentDto));

        verify(dnsV1Endpoint, never()).addDnsZoneForSubnets(any(AddDnsZoneForSubnetsRequest.class));
        verify(freeIpaService, never()).attachChildEnvironment(any(AttachChildEnvironmentRequest.class));
        verify(eventSender).sendEvent(any(BaseNamedFlowEvent.class), any(Headers.class));
    }

    @Test
    public void shouldNotAttachFreeIpaInCaseOfNonSupportedCloudPlatform() throws Exception {
        EnvironmentDto environmentDto = aNonYarnEnvironmentDtoWithParentEnvironment();
        Environment environment = anEnvironmentWithParent();

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));

        victim.accept(new Event<>(environmentDto));

        verify(dnsV1Endpoint, never()).addDnsZoneForSubnets(any(AddDnsZoneForSubnetsRequest.class));
        verify(freeIpaService, never()).attachChildEnvironment(any(AttachChildEnvironmentRequest.class));
        verify(eventSender).sendEvent(any(BaseNamedFlowEvent.class), any(Headers.class));
    }

    @ParameterizedTest
    @EnumSource(value = PollingResult.class, names = "SUCCESS", mode = Mode.EXCLUDE)
    public void testIfFreeIpaPollingServiceReturnsWithUnsuccessfulResultThenCreationFailedEventShouldBeSent(PollingResult pollingResult) {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        Environment environment = mock(Environment.class);

        when(environment.getCloudPlatform()).thenReturn(environmentDto.getCloudPlatform());
        when(environment.isCreateFreeIpa()).thenReturn(environmentDto.getFreeIpaCreation().getCreate());

        Pair<PollingResult, Exception> result = mock(Pair.class);

        when(result.getKey()).thenReturn(pollingResult);

        when(environmentService.findEnvironmentById(environmentDto.getId())).thenReturn(Optional.of(environment));
        when(supportedPlatforms.supportedPlatformForFreeIpa(environmentDto.getCloudPlatform())).thenReturn(true);
        when(connectors.getDefault(any())).thenReturn(mock(CloudConnector.class));
        when(freeIpaPollingService.pollWithTimeout(
                any(FreeIpaCreationRetrievalTask.class),
                any(FreeIpaPollerObject.class),
                anyLong(),
                anyInt(),
                anyInt()))
                .thenReturn(result);

        victim.accept(new Event<>(environmentDto));

        verify(eventBus).notify(anyString(), any(Event.class));

        verify(environmentService, times(1)).findEnvironmentById(anyLong());
        verify(environmentService, times(1)).findEnvironmentById(environmentDto.getId());
        verify(supportedPlatforms, times(1)).supportedPlatformForFreeIpa(anyString());
        verify(supportedPlatforms, times(1)).supportedPlatformForFreeIpa(environmentDto.getCloudPlatform());
        verify(freeIpaPollingService, times(1)).pollWithTimeout(
                any(FreeIpaCreationRetrievalTask.class),
                any(FreeIpaPollerObject.class),
                anyLong(),
                anyInt(),
                anyInt());
    }

    @Test
    public void testSpotParameters() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);

        int spotPercentage = 100;
        Double spotMaxPrice = 0.9;
        environmentDto.getFreeIpaCreation().setAws(FreeIpaCreationAwsParametersDto.builder()
                .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                        .withMaxPrice(spotMaxPrice)
                        .withPercentage(spotPercentage)
                        .build())
                .build());

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(supportedPlatforms.supportedPlatformForFreeIpa(environment.getCloudPlatform())).thenReturn(true);
        when(freeIpaService.describe(ENVIRONMENT_CRN)).thenReturn(Optional.empty());
        when(connectors.getDefault(any())).thenReturn(mock(CloudConnector.class));
        when(freeIpaPollingService.pollWithTimeout(
                any(FreeIpaCreationRetrievalTask.class),
                any(FreeIpaPollerObject.class),
                anyLong(),
                anyInt(),
                anyInt()))
                .thenReturn(Pair.of(PollingResult.SUCCESS, null));

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        freeIpaRequest.getInstanceGroups().stream()
                .map(InstanceGroupRequest::getInstanceTemplate)
                .map(InstanceTemplateRequest::getAws)
                .map(AwsInstanceTemplateParameters::getSpot)
                .forEach(spotParameters -> {
                    assertEquals(spotMaxPrice, spotParameters.getMaxPrice());
                    assertEquals(spotPercentage, spotParameters.getPercentage());
                });
    }

    @Test
    public void testImageCatalogAndImageIdParemetersArePopulated() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setImageId(IMAGE_ID);
        environmentDto.getFreeIpaCreation().setImageCatalog(IMAGE_CATALOG);

        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(supportedPlatforms.supportedPlatformForFreeIpa(environment.getCloudPlatform())).thenReturn(true);
        when(freeIpaService.describe(ENVIRONMENT_CRN)).thenReturn(Optional.empty());
        when(connectors.getDefault(any())).thenReturn(mock(CloudConnector.class));
        when(freeIpaPollingService.pollWithTimeout(
                any(FreeIpaCreationRetrievalTask.class),
                any(FreeIpaPollerObject.class),
                anyLong(),
                anyInt(),
                anyInt()))
                .thenReturn(Pair.of(PollingResult.SUCCESS, null));

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertEquals(IMAGE_CATALOG, freeIpaRequest.getImage().getCatalog());
        assertEquals(IMAGE_ID, freeIpaRequest.getImage().getId());
    }

    @Test
    public void testFreeIpaImageIsNullInCaseOfMissingImageCatalog() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setImageId(IMAGE_ID);
        environmentDto.getFreeIpaCreation().setImageCatalog(null);

        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(supportedPlatforms.supportedPlatformForFreeIpa(environment.getCloudPlatform())).thenReturn(true);
        when(freeIpaService.describe(ENVIRONMENT_CRN)).thenReturn(Optional.empty());
        when(connectors.getDefault(any())).thenReturn(mock(CloudConnector.class));
        when(freeIpaPollingService.pollWithTimeout(
                any(FreeIpaCreationRetrievalTask.class),
                any(FreeIpaPollerObject.class),
                anyLong(),
                anyInt(),
                anyInt()))
                .thenReturn(Pair.of(PollingResult.SUCCESS, null));

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertNull(freeIpaRequest.getImage());
        assertNull(freeIpaRequest.getImage());
    }

    @Test
    public void testFreeIpaImageIsNullInCaseOfMissingImageId() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setImageId(null);
        environmentDto.getFreeIpaCreation().setImageCatalog(IMAGE_CATALOG);

        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));
        when(supportedPlatforms.supportedPlatformForFreeIpa(environment.getCloudPlatform())).thenReturn(true);
        when(freeIpaService.describe(ENVIRONMENT_CRN)).thenReturn(Optional.empty());
        when(connectors.getDefault(any())).thenReturn(mock(CloudConnector.class));
        when(freeIpaPollingService.pollWithTimeout(
                any(FreeIpaCreationRetrievalTask.class),
                any(FreeIpaPollerObject.class),
                anyLong(),
                anyInt(),
                anyInt()))
                .thenReturn(Pair.of(PollingResult.SUCCESS, null));

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertNull(freeIpaRequest.getImage());
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

    private EnvironmentDto someEnvironmentWithFreeIpaCreation() {
        EnvironmentDto dto = new EnvironmentDto();

        dto.setId(ENVIRONMENT_ID);
        dto.setResourceCrn(ENVIRONMENT_CRN);
        dto.setCloudPlatform(CloudPlatform.AWS.name());
        dto.setTags(new EnvironmentTags(emptyMap(), emptyMap()));
        dto.setAuthentication(AuthenticationDto.builder().build());
        dto.setRegions(Set.of(createRegion("someWhereOverTheRainbow")));
        dto.setFreeIpaCreation(FreeIpaCreationDto.builder().withCreate(true).build());
        dto.setNetwork(NetworkDto.builder().withNetworkCidr(YARN_NETWORK_CIDR).build());

        return dto;
    }

    private EnvironmentDto aNonYarnEnvironmentDtoWithParentEnvironment() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENVIRONMENT_ID);
        environmentDto.setParentEnvironmentCrn(PARENT_ENVIRONMENT_CRN);
        environmentDto.setCloudPlatform(CloudPlatform.AWS.name());

        return environmentDto;
    }

    private Region createRegion(String name) {
        Region region = new Region();
        region.setName(name);
        region.setDisplayName(name);
        return region;
    }

    private Environment anEnvironmentWithParent() {
        Environment environment = new Environment();
        Environment parentEnvironment = new Environment();
        environment.setParentEnvironment(parentEnvironment);

        return environment;
    }
}