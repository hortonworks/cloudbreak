package com.sequenceiq.environment.environment.v1.converter;

import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AWS;
import static com.sequenceiq.cloudbreak.common.mappable.CloudPlatform.AZURE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.tag.request.TagsRequest;
import com.sequenceiq.common.api.telemetry.model.Features;
import com.sequenceiq.common.api.telemetry.request.TelemetryRequest;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.base.CloudStorageValidation;
import com.sequenceiq.environment.api.v1.environment.model.base.IdBrokerMappingSource;
import com.sequenceiq.environment.api.v1.environment.model.request.AttachedFreeIpaRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentAuthenticationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentEditRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.LocationRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.SecurityAccessRequest;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.AwsFreeIpaSpotParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.aws.S3GuardRequestParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureEnvironmentParameters;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.AzureResourceGroup;
import com.sequenceiq.environment.api.v1.environment.model.request.azure.ResourceGroupUsage;
import com.sequenceiq.environment.credential.service.CredentialService;
import com.sequenceiq.environment.credential.v1.converter.TunnelConverter;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.dto.AuthenticationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentCreationDto;
import com.sequenceiq.environment.environment.dto.EnvironmentEditDto;
import com.sequenceiq.environment.environment.dto.FreeIpaCreationDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.dto.SecurityAccessDto;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern;
import com.sequenceiq.environment.parameters.dto.ParametersDto;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;
import com.sequenceiq.environment.telemetry.service.AccountTelemetryService;

@ExtendWith(SpringExtension.class)
public class EnvironmentApiConverterTest {

    private static final String CREDENTIAL_NAME = "my-credential";

    private static final String REGION_DISPLAY_NAME = "US WEST";

    private static final String USER_CRN = "crn:altus:iam:us-west-1:test-aws:user:cloudbreak@hortonworks.com";

    @InjectMocks
    private EnvironmentApiConverter underTest;

    @Mock
    private CredentialService credentialService;

    @Mock
    private TelemetryApiConverter telemetryApiConverter;

    @Mock
    private AccountTelemetryService accountTelemetryService;

    @Mock
    private TunnelConverter tunnelConverter;

    @Mock
    private FreeIpaConverter freeIpaConverter;

    @Mock
    private NetworkRequestToDtoConverter networkRequestToDtoConverter;

    @Mock
    private EntitlementService entitlementService;

    @BeforeAll
    static void before() {
        ThreadBasedUserCrnProvider.setUserCrn(USER_CRN);
    }

    @ParameterizedTest
    @EnumSource(value = CloudPlatform.class, names = {"AWS", "AZURE"})
    void testInitCreationDto(CloudPlatform cloudPlatform) {
        EnvironmentRequest request = createEnvironmentRequest(cloudPlatform);
        FreeIpaCreationDto freeIpaCreationDto = mock(FreeIpaCreationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        AccountTelemetry accountTelemetry = mock(AccountTelemetry.class);
        Features features = mock(Features.class);
        NetworkDto networkDto = mock(NetworkDto.class);

        when(credentialService.getCloudPlatformByCredential(anyString(), anyString())).thenReturn(cloudPlatform.name());
        when(freeIpaConverter.convert(request.getFreeIpa())).thenReturn(freeIpaCreationDto);
        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any())).thenReturn(environmentTelemetry);
        when(tunnelConverter.convert(request.getTunnel())).thenReturn(request.getTunnel());
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);

        EnvironmentCreationDto actual = underTest.initCreationDto(request);

        assertEquals("test-aws", actual.getAccountId());
        assertEquals(USER_CRN, actual.getCreator());
        assertEquals(request.getName(), actual.getName());
        assertEquals(request.getDescription(), actual.getDescription());
        assertEquals(request.getCredentialName(), actual.getCredential().getCredentialName());
        assertEquals(request, actual.getCredential());
        assertEquals(freeIpaCreationDto, actual.getFreeIpaCreation());
        assertLocation(request.getLocation(), actual.getLocation());
        assertEquals(environmentTelemetry, actual.getTelemetry());
        assertEquals(request.getRegions(), actual.getRegions());
        assertAuthentication(request.getAuthentication(), actual.getAuthentication());
        assertEquals(request.getAdminGroupName(), actual.getAdminGroupName());
        assertEquals(request.getTags().getAll(), actual.getTags().getAll());
        assertExperimentalFeatures(request, actual.getExperimentalFeatures());
        assertParameters(request, actual.getParameters(), cloudPlatform);
        assertEquals(request.getProxyConfigName(), actual.getProxyConfigName());
        assertEquals(networkDto, actual.getNetwork());
        assertSecurityAccess(request.getSecurityAccess(), actual.getSecurityAccess());

        verify(credentialService).getCloudPlatformByCredential(anyString(), anyString());
        verify(freeIpaConverter).convert(request.getFreeIpa());
        verify(accountTelemetry).getFeatures();
        verify(accountTelemetryService).getOrDefault(any());
        verify(telemetryApiConverter).convert(eq(request.getTelemetry()), any());
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

        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any())).thenReturn(environmentTelemetry);
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);

        EnvironmentEditDto actual = underTest.initEditDto(request);

        assertEquals("test-aws", actual.getAccountId());
        assertEquals(request.getDescription(), actual.getDescription());
        assertEquals(environmentTelemetry, actual.getTelemetry());
        assertAuthentication(request.getAuthentication(), actual.getAuthentication());
        assertEquals(request.getAdminGroupName(), actual.getAdminGroupName());
        assertSecurityAccess(request.getSecurityAccess(), actual.getSecurityAccess());

        verify(accountTelemetry).getFeatures();
        verify(accountTelemetryService).getOrDefault(any());
        verify(telemetryApiConverter).convert(eq(request.getTelemetry()), any());
        verify(networkRequestToDtoConverter).convert(request.getNetwork());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void testAzureSingleRgEnabledWithEmptyAzureRequest(Boolean azureSingleResourceGroupDeploymentEnabled) {
        EnvironmentRequest request = createEnvironmentRequest(AZURE);
        request.setAzure(null);
        FreeIpaCreationDto freeIpaCreationDto = mock(FreeIpaCreationDto.class);
        EnvironmentTelemetry environmentTelemetry = mock(EnvironmentTelemetry.class);
        AccountTelemetry accountTelemetry = mock(AccountTelemetry.class);
        Features features = mock(Features.class);
        NetworkDto networkDto = mock(NetworkDto.class);

        when(credentialService.getCloudPlatformByCredential(anyString(), anyString())).thenReturn(AZURE.name());
        when(freeIpaConverter.convert(request.getFreeIpa())).thenReturn(freeIpaCreationDto);
        when(accountTelemetry.getFeatures()).thenReturn(features);
        when(accountTelemetryService.getOrDefault(any())).thenReturn(accountTelemetry);
        when(telemetryApiConverter.convert(eq(request.getTelemetry()), any())).thenReturn(environmentTelemetry);
        when(tunnelConverter.convert(request.getTunnel())).thenReturn(request.getTunnel());
        when(networkRequestToDtoConverter.convert(request.getNetwork())).thenReturn(networkDto);
        when(entitlementService.azureSingleResourceGroupDeploymentEnabled(anyString(), anyString())).thenReturn(azureSingleResourceGroupDeploymentEnabled);

        EnvironmentCreationDto actual = underTest.initCreationDto(request);
        assertEquals(
                azureSingleResourceGroupDeploymentEnabled
                        ? ResourceGroupUsagePattern.USE_SINGLE
                        : ResourceGroupUsagePattern.USE_MULTIPLE,
                actual.getParameters().getAzureParametersDto().getAzureResourceGroupDto().getResourceGroupUsagePattern());

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
    }

    private void assertParameters(EnvironmentRequest request, ParametersDto actual, CloudPlatform cloudPlatform) {
        if (AWS.equals(cloudPlatform)) {
            assertAwsParameters(request, actual);
        } else {
            assertAzureParameters(request, actual);
        }
    }

    private void assertAzureParameters(EnvironmentRequest request, ParametersDto actual) {
        assertEquals(request.getAzure().getResourceGroup().getName(),
                actual.getAzureParametersDto().getAzureResourceGroupDto().getName());
    }

    private void assertAwsParameters(EnvironmentRequest request, ParametersDto actual) {
        assertEquals(request.getAws().getS3guard().getDynamoDbTableName(), actual.getAwsParametersDto().getS3GuardTableName());
        assertEquals(request.getFreeIpa().getAws().getSpot().getPercentage(), actual.getAwsParametersDto().getFreeIpaSpotPercentage());
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
        request.setRegions(Collections.singleton("us-west"));
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
        request.setTags(new TagsRequest(Map.of("owner", "cloudbreak")));
        request.setParentEnvironmentName("parent-env");
        setParameters(request, cloudPlatform);
        return request;
    }

    private void setParameters(EnvironmentRequest request, CloudPlatform cloudPlatform) {
        if (AWS.equals(cloudPlatform)) {
            request.setAws(createAwsRequest());
        } else if (AZURE.equals(cloudPlatform)) {
            request.setAzure(createAzureRequest());
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
        request.setAws(createAwsRequest());
        return request;
    }

    private AwsEnvironmentParameters createAwsRequest() {
        S3GuardRequestParameters s3GuardRequestParameters = new S3GuardRequestParameters();
        s3GuardRequestParameters.setDynamoDbTableName("my-table");
        AwsEnvironmentParameters awsEnvironmentParameters = new AwsEnvironmentParameters();
        awsEnvironmentParameters.setS3guard(s3GuardRequestParameters);
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
        return azureEnvironmentParameters;
    }

    private AttachedFreeIpaRequest createFreeIpaRequest() {
        AwsFreeIpaSpotParameters awsFreeIpaSpotParameters = new AwsFreeIpaSpotParameters();
        awsFreeIpaSpotParameters.setPercentage(50);
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