package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.GCP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.crn.CrnTestUtil;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareCrnGenerator;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.CcmV2TlsType;
import com.sequenceiq.common.api.type.PublicEndpointAccessGateway;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentLoadBalancerUpdateRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsDiskEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaSpotParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.UpdateAzureResourceEncryptionParametersRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.gcp.GcpResourceEncryptionParameters;
import com.sequenceiq.environment.api.v1.proxy.model.request.ProxyRequest;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.TunnelConverter;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentBackup;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.EnvironmentLoadBalancerDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.UpdateAzureResourceEncryptionDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.v1.converter.ProxyRequestToProxyConfigConverter;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;
import com.sequenceiq.environment.telemetry.service.AccountTelemetryService;

@ExtendWith(SpringExtension.class)
public class EnvironmentApiConverterTest {

    private static final String CREDENTIAL_NAME = "my-credential";

    private static final String REGION_DISPLAY_NAME = "US WEST";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:test-aws:user:cloudbreak@hortonworks.com";

    private static final String KEY_URL = "dummy-key-url";

    private static final String KEY_URL_RESOURCE_GROUP = "dummy-key-url";

    private static final String ENCRYPTION_KEY_ARN = "dummy-key-arn";

    @InjectMocks
    private EnvironmentApiConverter underTest;

    @Mock
    private CredentialService credentialService;

    @Mock
    private TelemetryApiConverter telemetryApiConverter;

    @Mock
    private BackupConverter backupConverter;

    @Mock
    private AccountTelemetryService accountTelemetryService;

    @Mock
    private TunnelConverter tunnelConverter;

    @Mock
    private FreeIpaConverter freeIpaConverter;

    @Mock
    private NetworkRequestToDtoConverter networkRequestToDtoConverter;

    @Mock
    private RegionAwareCrnGenerator regionAwareCrnGenerator;

    @Mock
    private ProxyRequestToProxyConfigConverter proxyRequestToProxyConfigConverter;

    @BeforeEach
    void init() {
        CrnTestUtil.mockCrnGenerator(regionAwareCrnGenerator);
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE", "GCP"})
    void testInitCreationDto(CloudPlatform cloudPlatform) {
        EnvironmentRequest request = createEnvironmentRequest(cloudPlatform);
        FreeIpaCreationDto freeIpaCreationDto = mock(FreeIpaCreationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        AccountTelemetry accountTelemetry = mock(AccountTelemetry.class);
        Features features = mock(Features.class);
        NetworkDto networkDto = mock(NetworkDto.class);

        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn(cloudPlatform.name());
        when(freeIpaConverter.convert(request.getFreeIpa(), "test-aws", cloudPlatform.name())).thenReturn(freeIpaCreationDto);
        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any(), anyString())).thenReturn(environmentTelemetry);
        when(tunnelConverter.convert(request.getTunnel())).thenReturn(request.getTunnel());
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);

        EnvironmentCreationDto actual = testInitCreationDto(request);

        assertEquals("test-aws", actual.getAccountId());
        assertEquals(USER_CRN, actual.getCreator());
        assertEquals(request.getName(), actual.getName());
        assertEquals(request.getDescription(), actual.getDescription());
        assertEquals(request.getCredentialName(), actual.getCredential().getCredentialName());
        assertEquals(request, actual.getCredential());
        assertEquals(freeIpaCreationDto, actual.getFreeIpaCreation());
        assertLocation(request.getLocation(), actual.getLocation());
        assertEquals(environmentTelemetry, actual.getTelemetry());
        assertEquals(1, actual.getRegions().size());
        assertAuthentication(request.getAuthentication(), actual.getAuthentication());
        assertEquals(request.getAdminGroupName(), actual.getAdminGroupName());
        assertEquals(request.getTags(), actual.getTags());
        assertExperimentalFeatures(request, actual.getExperimentalFeatures());
        assertParameters(request, actual.getParameters(), cloudPlatform);
        assertEquals(request.getProxyConfigName(), actual.getProxyConfigName());
        assertEquals(networkDto, actual.getNetwork());
        assertSecurityAccess(request.getSecurityAccess(), actual.getSecurityAccess());

        verify(credentialService).getCloudPlatformByCredential(anyString(), anyString(), any());
        verify(freeIpaConverter).convert(request.getFreeIpa(), "test-aws", cloudPlatform.name());
        verify(accountTelemetry).getFeatures();
        verify(accountTelemetryService).getOrDefault(any());
        verify(telemetryApiConverter).convert(eq(request.getTelemetry()), any(), anyString());
        verify(tunnelConverter).convert(request.getTunnel());
        verify(networkRequestToDtoConverter).convert(request.getNetwork());
    }

    @Test
    void testInitEditDto() {
        EnvironmentEditRequest request = createEditEnvironmentRequest();
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        AccountTelemetry accountTelemetry = mock(AccountTelemetry.class);
        Features features = mock(Features.class);
        NetworkDto networkDto = mock(NetworkDto.class);
        ProxyConfig proxyConfig = mock(ProxyConfig.class);

        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any(), anyString())).thenReturn(environmentTelemetry);
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);
        when(proxyRequestToProxyConfigConverter.convert(request.getProxy())).thenReturn(proxyConfig);

        EnvironmentEditDto actual = ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.initEditDto(request));

        assertEquals("test-aws", actual.getAccountId());
        assertEquals(request.getDescription(), actual.getDescription());
        assertEquals(environmentTelemetry, actual.getTelemetry());
        assertAuthentication(request.getAuthentication(), actual.getAuthentication());
        assertEquals(request.getAdminGroupName(), actual.getAdminGroupName());
        assertSecurityAccess(request.getSecurityAccess(), actual.getSecurityAccess());
        assertEquals(proxyConfig, actual.getProxyConfig());

        verify(accountTelemetry).getFeatures();
        verify(accountTelemetryService).getOrDefault(any());
        verify(telemetryApiConverter).convert(eq(request.getTelemetry()), any(), anyString());
        verify(networkRequestToDtoConverter).convert(request.getNetwork());
    }

    @Test
    void testAzureSingleRgEnabledAndEmptyAzureRequest() {
        EnvironmentRequest request = createEnvironmentRequest(AZURE);
        request.setAzure(null);
        FreeIpaCreationDto freeIpaCreationDto = mock(FreeIpaCreationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        AccountTelemetry accountTelemetry = mock(AccountTelemetry.class);
        Features features = mock(Features.class);
        NetworkDto networkDto = mock(NetworkDto.class);
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn(AZURE.name());
        when(freeIpaConverter.convert(request.getFreeIpa(), "id", CloudConstants.AWS)).thenReturn(freeIpaCreationDto);
        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any(), anyString())).thenReturn(environmentTelemetry);
        when(tunnelConverter.convert(request.getTunnel())).thenReturn(request.getTunnel());
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);

        EnvironmentCreationDto actual = testInitCreationDto(request);

        assertEquals(ResourceGroupUsagePattern.USE_MULTIPLE,
                actual.getParameters().getAzureParametersDto().getAzureResourceGroupDto().getResourceGroupUsagePattern());
    }

    @Test
    void testAzureSingleRgEnabledAndAzureRequestWithSingleUsageAndName() {
        EnvironmentRequest request = createEnvironmentRequest(AZURE);
        request.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(
                        AzureResourceGroup.builder()
                                .withName("myResourceGroup")
                                .withResourceGroupUsage(ResourceGroupUsage.SINGLE)
                                .build())
                .build());
        FreeIpaCreationDto freeIpaCreationDto = mock(FreeIpaCreationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        AccountTelemetry accountTelemetry = mock(AccountTelemetry.class);
        Features features = mock(Features.class);
        NetworkDto networkDto = mock(NetworkDto.class);
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn(AZURE.name());
        when(freeIpaConverter.convert(request.getFreeIpa(), "id", CloudConstants.AWS)).thenReturn(freeIpaCreationDto);
        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any(), anyString())).thenReturn(environmentTelemetry);
        when(tunnelConverter.convert(request.getTunnel())).thenReturn(request.getTunnel());
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);

        EnvironmentCreationDto actual = testInitCreationDto(request);

        assertEquals(ResourceGroupUsagePattern.USE_SINGLE,
                actual.getParameters().getAzureParametersDto().getAzureResourceGroupDto().getResourceGroupUsagePattern());
        assertEquals("myResourceGroup",
                actual.getParameters().getAzureParametersDto().getAzureResourceGroupDto().getName());
    }

    @Test
    void testAzureSingleRgEnabledAndAzureRequestWithoutUsageAndWithName() {
        EnvironmentRequest request = createEnvironmentRequest(AZURE);
        request.setAzure(AzureEnvironmentParameters.builder()
                .withAzureResourceGroup(
                        AzureResourceGroup.builder()
                                .withName("myResourceGroup")
                                .build())
                .build());
        FreeIpaCreationDto freeIpaCreationDto = mock(FreeIpaCreationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        EnvironmentBackup environmentBackup = mock(EnvironmentBackup.class);
        AccountTelemetry accountTelemetry = mock(AccountTelemetry.class);
        Features features = mock(Features.class);
        NetworkDto networkDto = mock(NetworkDto.class);
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn(AZURE.name());
        when(freeIpaConverter.convert(request.getFreeIpa(), "id", CloudConstants.AWS)).thenReturn(freeIpaCreationDto);
        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any(), anyString())).thenReturn(environmentTelemetry);
        when(backupConverter.convert(eq(request.getBackup()))).thenReturn(environmentBackup);
        when(tunnelConverter.convert(request.getTunnel())).thenReturn(request.getTunnel());
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);

        EnvironmentCreationDto actual = testInitCreationDto(request);

        assertNull(actual.getParameters().getAzureParametersDto().getAzureResourceGroupDto().getResourceGroupUsagePattern());
        assertEquals("myResourceGroup",
                actual.getParameters().getAzureParametersDto().getAzureResourceGroupDto().getName());
    }

    @Test
    void testInitLoadBalancerDto() {
        Set<String> subnetIds = Set.of("id1, id2");
        EnvironmentLoadBalancerUpdateRequest request = new EnvironmentLoadBalancerUpdateRequest();
        request.setPublicEndpointAccessGateway(PublicEndpointAccessGateway.ENABLED);
        request.setSubnetIds(subnetIds);

        EnvironmentLoadBalancerDto environmentLbDto = underTest.initLoadBalancerDto(request);

        assertEquals(PublicEndpointAccessGateway.ENABLED, environmentLbDto.getEndpointAccessGateway());
        assertEquals(subnetIds, environmentLbDto.getEndpointGatewaySubnetIds());
    }

    @Test
    void testAwsDiskEncryptionParametersAndAwsRequest() {
        EnvironmentRequest request = createEnvironmentRequest(AWS);
        FreeIpaCreationDto freeIpaCreationDto = mock(FreeIpaCreationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        EnvironmentBackup environmentBackup = mock(EnvironmentBackup.class);
        AccountTelemetry accountTelemetry = mock(AccountTelemetry.class);
        Features features = mock(Features.class);
        NetworkDto networkDto = mock(NetworkDto.class);
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn(AWS.name());
        when(freeIpaConverter.convert(request.getFreeIpa(), "id", CloudConstants.AWS)).thenReturn(freeIpaCreationDto);
        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any(), anyString())).thenReturn(environmentTelemetry);
        when(backupConverter.convert(eq(request.getBackup()))).thenReturn(environmentBackup);
        when(tunnelConverter.convert(request.getTunnel())).thenReturn(request.getTunnel());
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);

        EnvironmentCreationDto actual = testInitCreationDto(request);
        assertEquals(ENCRYPTION_KEY_ARN,
                actual.getParameters().getAwsParametersDto().getAwsDiskEncryptionParametersDto().getEncryptionKeyArn());
    }

    @Test
    void testAzureResourceEncryptionParametersAndAzureRequest() {
        EnvironmentRequest request = createEnvironmentRequest(AZURE);
        request.setAzure(AzureEnvironmentParameters.builder()
                .withResourceEncryptionParameters(
                        AzureResourceEncryptionParameters.builder()
                                .withEncryptionKeyUrl(KEY_URL)
                                .withEncryptionKeyResourceGroupName(KEY_URL_RESOURCE_GROUP)
                                .build())
                .build());
        FreeIpaCreationDto freeIpaCreationDto = mock(FreeIpaCreationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        EnvironmentBackup environmentBackup = mock(EnvironmentBackup.class);
        AccountTelemetry accountTelemetry = mock(AccountTelemetry.class);
        Features features = mock(Features.class);
        NetworkDto networkDto = mock(NetworkDto.class);
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn(AZURE.name());
        when(freeIpaConverter.convert(request.getFreeIpa(), "id", CloudConstants.AWS)).thenReturn(freeIpaCreationDto);
        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any(), anyString())).thenReturn(environmentTelemetry);
        when(backupConverter.convert(eq(request.getBackup()))).thenReturn(environmentBackup);
        when(tunnelConverter.convert(request.getTunnel())).thenReturn(request.getTunnel());
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);

        EnvironmentCreationDto actual = testInitCreationDto(request);

        assertEquals(KEY_URL,
                actual.getParameters().getAzureParametersDto().getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl());
        assertEquals(KEY_URL_RESOURCE_GROUP,
                actual.getParameters().getAzureParametersDto().getAzureResourceEncryptionParametersDto().getEncryptionKeyResourceGroupName());
    }

    @Test
    void testGcpResourceEncryptionParametersAndGcpRequest() {
        EnvironmentRequest request = createEnvironmentRequest(GCP);
        request.setGcp(GcpEnvironmentParameters.builder()
                .withResourceEncryptionParameters(
                        GcpResourceEncryptionParameters.builder()
                                .withEncryptionKey("dummy-encryption-key")
                                .build())
                .build());
        FreeIpaCreationDto freeIpaCreationDto = mock(FreeIpaCreationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        EnvironmentBackup environmentBackup = mock(EnvironmentBackup.class);
        AccountTelemetry accountTelemetry = mock(AccountTelemetry.class);
        Features features = mock(Features.class);
        NetworkDto networkDto = mock(NetworkDto.class);
        when(credentialService.getCloudPlatformByCredential(anyString(), anyString(), any())).thenReturn(GCP.name());
        when(freeIpaConverter.convert(request.getFreeIpa(), "id", CloudConstants.AWS)).thenReturn(freeIpaCreationDto);
        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any(), anyString())).thenReturn(environmentTelemetry);
        when(backupConverter.convert(eq(request.getBackup()))).thenReturn(environmentBackup);
        when(tunnelConverter.convert(request.getTunnel())).thenReturn(request.getTunnel());
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);

        EnvironmentCreationDto actual = testInitCreationDto(request);

        assertEquals("dummy-encryption-key",
                actual.getParameters().getGcpParametersDto().getGcpResourceEncryptionParametersDto().getEncryptionKey());
    }

    private EnvironmentCreationDto testInitCreationDto(EnvironmentRequest request) {
        return ThreadBasedUserCrnProvider.doAs(USER_CRN, () -> underTest.initCreationDto(request));
    }

    @Test
    void testConvertUpdateAzureResourceEncryptionDto() {
        UpdateAzureResourceEncryptionParametersRequest request = UpdateAzureResourceEncryptionParametersRequest.builder()
                .withAzureResourceEncryptionParameters(AzureResourceEncryptionParameters.builder()
                        .withEncryptionKeyUrl(KEY_URL)
                        .withEncryptionKeyResourceGroupName(KEY_URL_RESOURCE_GROUP)
                        .build())
                .build();

        UpdateAzureResourceEncryptionDto actual = underTest.convertUpdateAzureResourceEncryptionDto(request);

        assertEquals(KEY_URL, actual.getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl());
        assertEquals(KEY_URL_RESOURCE_GROUP, actual.getAzureResourceEncryptionParametersDto().getEncryptionKeyResourceGroupName());
    }

    private void assertLocation(LocationRequest request, LocationDto actual) {
        assertEquals(request.getName(), actual.getName());
        assertEquals(request.getLatitude(), actual.getLatitude());
        assertEquals(request.getLongitude(), actual.getLongitude());
        assertEquals(REGION_DISPLAY_NAME, actual.getDisplayName());
    }

    private void assertAuthentication(EnvironmentAuthenticationRequest request, AuthenticationDto actual) {
        assertEquals("public-key", actual.getPublicKey());
        assertEquals(request.getPublicKeyId(), actual.getPublicKeyId());
        assertEquals(request.getLoginUserName(), actual.getLoginUserName());
    }

    private void assertExperimentalFeatures(EnvironmentRequest request, ExperimentalFeatures actual) {
        assertEquals(request.getIdBrokerMappingSource(), actual.getIdBrokerMappingSource());
        assertEquals(request.getCloudStorageValidation(), actual.getCloudStorageValidation());
        assertEquals(request.getTunnel(), actual.getTunnel());
        assertEquals(request.getCcmV2TlsType(), actual.getCcmV2TlsType());
    }

    private void assertParameters(EnvironmentRequest request, ParametersDto actual, CloudPlatform cloudPlatform) {
        if (AWS.equals(cloudPlatform)) {
            assertAwsParameters(request, actual);
        } else if (AZURE.equals(cloudPlatform)) {
            assertAzureParameters(request, actual);
        } else if (GCP.equals(cloudPlatform)) {
            assertGcpParameters(request, actual);
        }
    }

    private void assertAzureParameters(EnvironmentRequest request, ParametersDto actual) {
        assertEquals(request.getAzure().getResourceGroup().getName(),
                actual.getAzureParametersDto().getAzureResourceGroupDto().getName());
        assertEquals(request.getAzure().getResourceEncryptionParameters().getEncryptionKeyUrl(),
                actual.getAzureParametersDto().getAzureResourceEncryptionParametersDto().getEncryptionKeyUrl());
        assertEquals(request.getAzure().getResourceEncryptionParameters().getEncryptionKeyResourceGroupName(),
                actual.getAzureParametersDto().getAzureResourceEncryptionParametersDto().getEncryptionKeyResourceGroupName());
    }

    private void assertAwsParameters(EnvironmentRequest request, ParametersDto actual) {
        assertEquals(request.getAws().getS3guard().getDynamoDbTableName(), actual.getAwsParametersDto().getS3GuardTableName());
        assertEquals(request.getFreeIpa().getAws().getSpot().getPercentage(), actual.getAwsParametersDto().getFreeIpaSpotPercentage());
        assertEquals(request.getFreeIpa().getAws().getSpot().getMaxPrice(), actual.getAwsParametersDto().getFreeIpaSpotMaxPrice());
        assertEquals(request.getAws().getAwsDiskEncryptionParameters().getEncryptionKeyArn(),
                actual.getAwsParametersDto().getAwsDiskEncryptionParametersDto().getEncryptionKeyArn());
    }

    private void assertGcpParameters(EnvironmentRequest request, ParametersDto actual) {
        assertEquals(request.getGcp().getGcpResourceEncryptionParameters().getEncryptionKey(),
                actual.getGcpParametersDto().getGcpResourceEncryptionParametersDto().getEncryptionKey());
    }

    private void assertSecurityAccess(SecurityAccessRequest request, SecurityAccessDto actual) {
        assertEquals(request.getCidr(), actual.getCidr());
        assertEquals(request.getDefaultSecurityGroupId(), actual.getDefaultSecurityGroupId());
        assertEquals(request.getSecurityGroupIdForKnox(), actual.getSecurityGroupIdForKnox());
    }

    private EnvironmentRequest createEnvironmentRequest(CloudPlatform cloudPlatform) {
        EnvironmentRequest request = new EnvironmentRequest();
        request.setName("test-cluster");
        request.setDescription("Test description.");
        request.setCredentialName(CREDENTIAL_NAME);
        request.setLocation(createLocationRequest());
        request.setNetwork(new EnvironmentNetworkRequest());
        request.setTelemetry(new TelemetryRequest());
        request.setAuthentication(createAuthenticationRequest());
        request.setFreeIpa(createFreeIpaRequest());
        request.setSecurityAccess(createSecurityAccessRequest());
        request.setTunnel(Tunnel.CCM);
        request.setIdBrokerMappingSource(IdBrokerMappingSource.IDBMMS);
        request.setCloudStorageValidation(CloudStorageValidation.DISABLED);
        request.setAdminGroupName("cb-admin");
        request.setProxyConfigName("my-proxy");
        request.setTags(Map.of("owner", "cloudbreak"));
        request.setParentEnvironmentName("parent-env");
        request.setCcmV2TlsType(CcmV2TlsType.ONE_WAY_TLS);
        setParameters(request, cloudPlatform);
        return request;
    }

    private void setParameters(EnvironmentRequest request, CloudPlatform cloudPlatform) {
        if (AWS.equals(cloudPlatform)) {
            request.setAws(createAwsRequest());
        } else if (AZURE.equals(cloudPlatform)) {
            request.setAzure(createAzureRequest());
        } else if (GCP.equals(cloudPlatform)) {
            request.setGcp(createGcpRequest());
        } else {
            throw new RuntimeException("Unexpected cloudplatform: " + cloudPlatform);
        }
    }

    private EnvironmentEditRequest createEditEnvironmentRequest() {
        EnvironmentEditRequest request = new EnvironmentEditRequest();
        request.setDescription("Test description.");
        request.setNetwork(new EnvironmentNetworkRequest());
        request.setAuthentication(createAuthenticationRequest());
        request.setTelemetry(new TelemetryRequest());
        request.setSecurityAccess(createSecurityAccessRequest());
        request.setIdBrokerMappingSource(IdBrokerMappingSource.IDBMMS);
        request.setCloudStorageValidation(CloudStorageValidation.DISABLED);
        request.setAdminGroupName("cb-admin");
        request.setProxy(new ProxyRequest());
        request.setAws(createAwsRequest());
        return request;
    }

    private AwsEnvironmentParameters createAwsRequest() {
        S3GuardRequestParameters s3GuardRequestParameters = new S3GuardRequestParameters();
        s3GuardRequestParameters.setDynamoDbTableName("my-table");
        AwsEnvironmentParameters awsEnvironmentParameters = new AwsEnvironmentParameters();
        awsEnvironmentParameters.setS3guard(s3GuardRequestParameters);
        awsEnvironmentParameters.setAwsDiskEncryptionParameters(
                AwsDiskEncryptionParameters.builder()
                        .withEncryptionKeyArn("dummy-key-arn")
                        .build()
        );
        return awsEnvironmentParameters;
    }

    private AzureEnvironmentParameters createAzureRequest() {
        AzureEnvironmentParameters azureEnvironmentParameters = new AzureEnvironmentParameters();
        azureEnvironmentParameters.setResourceGroup(
                AzureResourceGroup.builder()
                        .withName("mySingleResourceGroupName")
                        .withResourceGroupUsage(ResourceGroupUsage.MULTIPLE)
                        .build()
        );
        azureEnvironmentParameters.setResourceEncryptionParameters(
                AzureResourceEncryptionParameters.builder()
                        .withEncryptionKeyUrl(KEY_URL)
                        .withEncryptionKeyResourceGroupName(KEY_URL_RESOURCE_GROUP)
                        .build()
        );
        return azureEnvironmentParameters;
    }

    private GcpEnvironmentParameters createGcpRequest() {
        GcpEnvironmentParameters gcpEnvironmentParameters = new GcpEnvironmentParameters();

        gcpEnvironmentParameters.setGcpResourceEncryptionParameters(
                GcpResourceEncryptionParameters.builder()
                        .withEncryptionKey("dummy-encryption-key")
                        .build()
        );
        return gcpEnvironmentParameters;
    }

    private AttachedFreeIpaRequest createFreeIpaRequest() {
        AwsFreeIpaSpotParameters awsFreeIpaSpotParameters = new AwsFreeIpaSpotParameters();
        awsFreeIpaSpotParameters.setPercentage(50);
        awsFreeIpaSpotParameters.setMaxPrice(0.9);
        AwsFreeIpaParameters awsFreeIpaParameters = new AwsFreeIpaParameters();
        awsFreeIpaParameters.setSpot(awsFreeIpaSpotParameters);
        AttachedFreeIpaRequest attachedFreeIpaRequest = new AttachedFreeIpaRequest();
        attachedFreeIpaRequest.setAws(awsFreeIpaParameters);
        return attachedFreeIpaRequest;
    }

    private SecurityAccessRequest createSecurityAccessRequest() {
        SecurityAccessRequest securityAccessRequest = new SecurityAccessRequest();
        securityAccessRequest.setCidr("1.1.1.1/16");
        securityAccessRequest.setDefaultSecurityGroupId("default-security-group");
        securityAccessRequest.setSecurityGroupIdForKnox("knox-security-group");
        return securityAccessRequest;
    }

    private EnvironmentAuthenticationRequest createAuthenticationRequest() {
        EnvironmentAuthenticationRequest authenticationRequest = new EnvironmentAuthenticationRequest();
        authenticationRequest.setPublicKey(" public-\nkey ");
        authenticationRequest.setPublicKeyId("my-public-key-id");
        authenticationRequest.setLoginUserName("cloudbreak");
        return authenticationRequest;
    }

    private LocationRequest createLocationRequest() {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setName(REGION_DISPLAY_NAME);
        locationRequest.setLatitude(123.4);
        locationRequest.setLongitude(567.8);
        return locationRequest;
    }
}
