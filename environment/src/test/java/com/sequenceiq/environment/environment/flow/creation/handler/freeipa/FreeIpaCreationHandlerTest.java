package com.sequenceiq.environment.environment.flow.creation.handler.freeipa;

import static com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType.NONE;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.eventbus.Event;
import com.sequenceiq.cloudbreak.eventbus.EventBus;
import com.sequenceiq.cloudbreak.polling.ExtendedPollingResult;
import com.sequenceiq.cloudbreak.polling.PollingResult;
import com.sequenceiq.cloudbreak.polling.PollingService;
import com.sequenceiq.environment.configuration.SupportedPlatforms;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationAwsSpotParametersDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.FreeIpaLoadBalancerType;
import com.sequenceiq.environment.environment.flow.creation.event.EnvCreationEvent;
import com.sequenceiq.environment.environment.service.EnvironmentService;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaService;
import com.sequenceiq.environment.environment.v1.converter.BackupConverter;
import com.sequenceiq.environment.environment.v1.converter.TelemetryApiConverter;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.flow.reactor.api.event.EventSender;
import com.sequenceiq.freeipa.api.v1.dns.DnsV1Endpoint;
import com.sequenceiq.freeipa.api.v1.dns.model.AddDnsZoneForSubnetsRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.attachchildenv.AttachChildEnvironmentRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceTemplateRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.aws.AwsInstanceTemplateParameters;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.create.CreateFreeIpaRequest;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;

@ExtendWith(MockitoExtension.class)
public class FreeIpaCreationHandlerTest {

    private static final long ENVIRONMENT_ID = 1L;

    private static final String PARENT_ENVIRONMENT_CRN = "parentEnvCrn";

    private static final String ENVIRONMENT_CRN = "envCrn";

    private static final String YARN_NETWORK_CIDR = "172.27.0.0/16";

    private static final String IMAGE_CATALOG = "image catalog";

    private static final String IMAGE_ID = "image id";

    private static final String INSTANCE_TYPE = "instance type";

    private static final int FREE_IPA_INSTANCE_COUNT_BY_GROUP = 2;

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
    private MultiAzValidator multiAzValidator;

    @Mock
    private EventBus eventBus;

    @Mock
    private EntitlementService entitlementService;

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
                eventBus,
                entitlementService,
                multiAzValidator,
                Collections.singleton(CloudPlatform.YARN.name()));
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
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));

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
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
    }

    @Test
    public void shouldNotAttachFreeIpaInCaseOfNonSupportedCloudPlatform() throws Exception {
        EnvironmentDto environmentDto = aNonYarnEnvironmentDtoWithParentEnvironment();
        Environment environment = anEnvironmentWithParent();

        when(environmentService.findEnvironmentById(ENVIRONMENT_ID)).thenReturn(Optional.of(environment));

        victim.accept(new Event<>(environmentDto));

        verify(dnsV1Endpoint, never()).addDnsZoneForSubnets(any(AddDnsZoneForSubnetsRequest.class));
        verify(freeIpaService, never()).attachChildEnvironment(any(AttachChildEnvironmentRequest.class));
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
    }

    @ParameterizedTest
    @EnumSource(value = PollingResult.class, names = "SUCCESS", mode = Mode.EXCLUDE)
    public void testIfFreeIpaPollingServiceReturnsWithUnsuccessfulResultThenCreationFailedEventShouldBeSent(PollingResult pollingResult) {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.setCredential(new Credential());
        Environment environment = mock(Environment.class);

        when(environment.getCloudPlatform()).thenReturn(environmentDto.getCloudPlatform());
        when(environment.isCreateFreeIpa()).thenReturn(environmentDto.getFreeIpaCreation().isCreate());

        ExtendedPollingResult result = mock(ExtendedPollingResult.class);

        when(result.getPollingResult()).thenReturn(pollingResult);

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

        verify(eventBus, times(1)).notify(anyString(), any(Event.class));

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
        Credential credential = new Credential();
        environment.setCredential(credential);

        int spotPercentage = 100;
        Double spotMaxPrice = 0.9;
        environmentDto.getFreeIpaCreation()
                .setAws(FreeIpaCreationAwsParametersDto.builder()
                        .withSpot(FreeIpaCreationAwsSpotParametersDto.builder()
                                .withMaxPrice(spotMaxPrice)
                                .withPercentage(spotPercentage)
                                .build())
                        .build());
        environmentDto.getFreeIpaCreation().setEnableMultiAz(true);
        environmentDto.setCredential(new Credential());

        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();
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
                .thenReturn(extendedPollingResult);

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        freeIpaRequest.getInstanceGroups().stream()
                .map(InstanceGroupRequest::getInstanceTemplate)
                .map(InstanceTemplateRequest::getAws)
                .map(AwsInstanceTemplateParameters::getSpot)
                .forEach(spotParameters -> {
                    assertEquals(spotMaxPrice, spotParameters.getMaxPrice());
                    assertEquals(spotPercentage, spotParameters.getPercentage());
                });
        assertEquals(freeIpaRequest.getEnableMultiAz(), true);
    }

    @Test
    public void testImageCatalogAndImageIdParemetersArePopulated() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setImageId(IMAGE_ID);
        environmentDto.getFreeIpaCreation().setImageCatalog(IMAGE_CATALOG);
        environmentDto.setCredential(new Credential());
        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);

        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();
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
                .thenReturn(extendedPollingResult);

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertEquals(IMAGE_CATALOG, freeIpaRequest.getImage().getCatalog());
        assertEquals(IMAGE_ID, freeIpaRequest.getImage().getId());
        assertEquals(freeIpaRequest.getEnableMultiAz(), false);
    }

    @Test
    public void testImageCatalogParemetersArePopulated() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setImageCatalog(IMAGE_CATALOG);
        environmentDto.setCredential(new Credential());
        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);

        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();
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
                .thenReturn(extendedPollingResult);

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertNull(freeIpaRequest.getImage().getId());
        assertNull(freeIpaRequest.getImage().getOs());
        assertEquals(IMAGE_CATALOG, freeIpaRequest.getImage().getCatalog());
        assertEquals(freeIpaRequest.getEnableMultiAz(), false);
    }

    @Test
    public void testFreeIpaInstanceTypeIsPopulatedIfProvided() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setInstanceType(INSTANCE_TYPE);
        environmentDto.getFreeIpaCreation().setEnableMultiAz(false);
        environmentDto.setCredential(new Credential());
        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);

        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();
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
                .thenReturn(extendedPollingResult);

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertThat(freeIpaRequest.getInstanceGroups()).extracting(ig -> ig.getInstanceTemplate().getInstanceType()).containsOnly(INSTANCE_TYPE);
        assertEquals(freeIpaRequest.getEnableMultiAz(), false);
    }

    @Test
    public void testFreeIpaRecipesPopulatedIfProvided() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setRecipes(Set.of("recipe1", "recipe2"));
        environmentDto.setCredential(new Credential());
        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);

        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();
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
                .thenReturn(extendedPollingResult);

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertThat(freeIpaRequest.getRecipes()).containsExactlyInAnyOrder("recipe1", "recipe2");
    }

    @Test
    public void testFreeIpaImageIdIsPopulatedInCaseOfMissingImageCatalog() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setImageId(IMAGE_ID);
        environmentDto.getFreeIpaCreation().setImageCatalog(null);
        environmentDto.setCredential(new Credential());
        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();

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
                .thenReturn(extendedPollingResult);

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertNull(freeIpaRequest.getImage().getCatalog());
        assertEquals(IMAGE_ID, freeIpaRequest.getImage().getId());
    }

    @Test
    public void testFreeIpaImageIsNullInCaseOfMissingImageId() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setImageId(null);
        environmentDto.setCredential(new Credential());
        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);
        environment.setFreeIpaPlatformVariant("AWS");
        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();
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
                .thenReturn(extendedPollingResult);

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertEquals("AWS", freeIpaRequest.getVariant());
        assertNull(freeIpaRequest.getImage());
    }

    @Test
    public void testFreeIpaLoadBalancerIsDisabledIfPopulated() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setLoadBalancerType(NONE);
        environmentDto.getFreeIpaCreation().setEnableMultiAz(false);
        environmentDto.setCredential(new Credential());
        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);

        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();
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
                .thenReturn(extendedPollingResult);

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertEquals("NONE", freeIpaRequest.getLoadBalancerType());
        assertEquals(freeIpaRequest.getEnableMultiAz(), false);
    }

    @Test
    public void testFreeIpaLoadBalancerIsEnabledWhenEmpty() {
        EnvironmentDto environmentDto = someEnvironmentWithFreeIpaCreation();
        environmentDto.getFreeIpaCreation().setEnableMultiAz(false);
        environmentDto.setCredential(new Credential());
        Environment environment = new Environment();
        environment.setCreateFreeIpa(true);

        ExtendedPollingResult extendedPollingResult = new ExtendedPollingResult.ExtendedPollingResultBuilder()
                .success()
                .build();
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
                .thenReturn(extendedPollingResult);

        victim.accept(new Event<>(environmentDto));

        ArgumentCaptor<CreateFreeIpaRequest> freeIpaRequestCaptor = ArgumentCaptor.forClass(CreateFreeIpaRequest.class);
        verify(freeIpaService).create(freeIpaRequestCaptor.capture());
        verify(eventSender, times(1)).sendEvent(any(EnvCreationEvent.class), any(Event.Headers.class));
        CreateFreeIpaRequest freeIpaRequest = freeIpaRequestCaptor.getValue();
        assertEquals(FreeIpaLoadBalancerType.getDefault().toString(), freeIpaRequest.getLoadBalancerType());
        assertEquals(freeIpaRequest.getEnableMultiAz(), false);
    }

    private EnvironmentDto aYarnEnvironmentDtoWithParentEnvironment() {
        EnvironmentDto environmentDto = new EnvironmentDto();
        environmentDto.setId(ENVIRONMENT_ID);
        environmentDto.setResourceCrn(ENVIRONMENT_CRN);
        environmentDto.setParentEnvironmentCrn(PARENT_ENVIRONMENT_CRN);
        environmentDto.setCloudPlatform(CloudPlatform.YARN.name());
        environmentDto.setNetwork(NetworkDto.builder()
                .withNetworkCidr(YARN_NETWORK_CIDR)
                .withNetworkCidrs(Set.of(YARN_NETWORK_CIDR))
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
        dto.setFreeIpaCreation(FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP).withCreate(true).build());
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
