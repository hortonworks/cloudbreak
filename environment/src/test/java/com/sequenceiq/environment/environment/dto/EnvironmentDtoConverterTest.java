package com.sequenceiq.environment.environment.dto;

import static java.util.Map.entry;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.common.model.Architecture;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.environment.api.v1.tags.model.response.AccountTagResponse;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.domain.CredentialView;
import com.sequenceiq.environment.environment.EnvironmentDeletionType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.DefaultComputeCluster;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentAuthentication;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.domain.ExperimentalFeatures;
import com.sequenceiq.environment.environment.domain.ParentEnvironmentView;
import com.sequenceiq.environment.environment.domain.Region;
import com.sequenceiq.environment.environment.dto.credential.CredentialDetailsConverter;
import com.sequenceiq.environment.environment.dto.telemetry.EnvironmentTelemetry;
import com.sequenceiq.environment.environment.service.freeipa.FreeIpaInstanceCountByGroupProvider;
import com.sequenceiq.environment.environment.service.recipe.EnvironmentRecipeService;
import com.sequenceiq.environment.network.dao.domain.AwsNetwork;
import com.sequenceiq.environment.network.dao.domain.BaseNetwork;
import com.sequenceiq.environment.network.dto.NetworkDto;
import com.sequenceiq.environment.network.v1.converter.EnvironmentNetworkConverter;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameters.dao.domain.AwsParameters;
import com.sequenceiq.environment.parameters.dao.domain.AzureParameters;
import com.sequenceiq.environment.parameters.v1.converter.EnvironmentParametersConverter;
import com.sequenceiq.environment.proxy.domain.ProxyConfig;
import com.sequenceiq.environment.proxy.domain.ProxyConfigView;

@ExtendWith(MockitoExtension.class)
class EnvironmentDtoConverterTest {

    private static final Long ID = 123L;

    private static final String RESOURCE_CRN = "resourceCrn";

    private static final String NAME = "name";

    private static final String ORIGINAL_NAME = "originalName";

    private static final String DESCRIPTION = "description";

    private static final String ACCOUNT_ID = "accountId";

    private static final boolean ARCHIVED = true;

    private static final String CLOUD_PLATFORM_AWS = "AWS";

    private static final String CLOUD_PLATFORM_AZURE = "AZURE";

    private static final Long DELETION_TIMESTAMP = 9876543210L;

    private static final String LOCATION = "location";

    private static final String LOCATION_DISPLAY_NAME = "locationDisplayName";

    private static final Double LONGITUDE = 12.34;

    private static final Double LATITUDE = -56.78;

    private static final EnvironmentStatus STATUS = EnvironmentStatus.AVAILABLE;

    private static final String CREATOR = "creator";

    private static final boolean CREATE_FREE_IPA = true;

    private static final Integer FREE_IPA_INSTANCE_COUNT_BY_GROUP = 2;

    private static final String FREE_IPA_INSTANCE_TYPE = "freeIpaInstanceType";

    private static final String FREE_IPA_IMAGE_CATALOG = "freeIpaImageCatalog";

    private static final String FREE_IPA_IMAGE_ID = "freeIpaImageId";

    private static final boolean FREE_IPA_ENABLE_MULTI_AZ = true;

    private static final Integer FREE_IPA_SPOT_PERCENTAGE = 23;

    private static final Double FREE_IPA_SPOT_MAX_PRICE = 567.0;

    private static final Long CREATED = 1234567890L;

    private static final String STATUS_REASON = "statusReason";

    private static final String CIDR = "cidr";

    private static final String SECURITY_GROUP_ID_FOR_KNOX = "securityGroupIdForKnox";

    private static final String DEFAULT_SECURITY_GROUP_ID = "defaultSecurityGroupId";

    private static final String ADMIN_GROUP_NAME = "adminGroupName";

    private static final EnvironmentDeletionType DELETION_TYPE = EnvironmentDeletionType.SIMPLE;

    private static final String ENVIRONMENT_SERVICE_VERSION = "environmentServiceVersion";

    private static final String DOMAIN = "domain";

    private static final String PARENT_RESOURCE_CRN = "parentResourceCrn";

    private static final String PARENT_NAME = "parentName";

    private static final String PARENT_CLOUD_PLATFORM = "GCP";

    private static final String STORAGE_LOCATION = "storageLocation";

    private static final String KUBE_API_AUTHORIZED_IP_RANGES = "0.0.0.0/0";

    private static final String OUTBOUND_TYPE = "udr";

    @Mock
    private AuthenticationDtoConverter authenticationDtoConverter;

    @Mock
    private EnvironmentTagsDtoConverter environmentTagsDtoConverter;

    @Mock
    private EnvironmentRecipeService environmentRecipeService;

    @Mock
    private EnvironmentView environmentView;

    @Mock
    private Environment environment;

    @Mock
    private Environment parentEnvironment;

    @Mock
    private EnvironmentParametersConverter environmentParametersConverter;

    @Mock
    private EnvironmentNetworkConverter environmentNetworkConverter;

    @Mock
    private FreeIpaInstanceCountByGroupProvider ipaInstanceCountByGroupProvider;

    @Mock
    private CredentialDetailsConverter credentialDetailsConverter;

    @Mock
    private EncryptionProfileDtoConverter encryptionProfileDtoConverter;

    @InjectMocks
    private EnvironmentDtoConverter underTest;

    @Test
    void testEnvironmentToEnvironmentDtoFreeIpaCreationWithoutAwsParameters() {
        Environment source = new Environment();
        source.setId(1L);
        source.setFreeIpaInstanceType("large");
        source.setFreeIpaImageId("imageid");
        source.setFreeIpaImageCatalog("imagecatalog");
        source.setFreeIpaInstanceCountByGroup(1);
        source.setFreeIpaEnableMultiAz(true);
        source.setCreateFreeIpa(true);
        source.setCloudPlatform("AWS");
        source.setAuthentication(new EnvironmentAuthentication());

        when(environmentRecipeService.getRecipes(1L)).thenReturn(Set.of("recipe1", "recipe2"));

        EnvironmentDto environmentDto = underTest.environmentToDto(source);

        assertThat(environmentDto).isNotNull();
        FreeIpaCreationDto freeIpaCreation = environmentDto.getFreeIpaCreation();
        assertNotNull(freeIpaCreation);
        assertEquals("large", freeIpaCreation.getInstanceType());
        assertEquals("imageid", freeIpaCreation.getImageId());
        assertEquals("imagecatalog", freeIpaCreation.getImageCatalog());
        assertEquals(1, freeIpaCreation.getInstanceCountByGroup());
        assertTrue(freeIpaCreation.isEnableMultiAz());
        assertTrue(freeIpaCreation.isCreate());
        assertNull(freeIpaCreation.getAws());
        assertThat(freeIpaCreation.getRecipes()).containsExactlyInAnyOrder("recipe1", "recipe2");
    }

    @Test
    void testEnvironmentToEnvironmentDtoWhenDefaultComputeClusterIsNull() {
        Environment source = new Environment();
        source.setCloudPlatform("AWS");
        EnvironmentDto environmentDto = underTest.environmentToDto(source);
        assertThat(environmentDto.isEnableComputeCluster()).isFalse();
    }

    @Test
    void testEnvironmentToEnvironmentDtoWhenDefaultComputeClusterCreateIsFalse() {
        Environment source = new Environment();
        source.setCloudPlatform("AWS");
        source.setDefaultComputeCluster(new DefaultComputeCluster());
        EnvironmentDto environmentDto = underTest.environmentToDto(source);
        assertThat(environmentDto.isEnableComputeCluster()).isFalse();
    }

    @Test
    void testEnvironmentToEnvironmentDtoWhenDefaultComputeClusterCreateIsTrue() {
        Environment source = new Environment();
        source.setCloudPlatform("AWS");
        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        defaultComputeCluster.setCreate(true);
        source.setDefaultComputeCluster(defaultComputeCluster);
        EnvironmentDto environmentDto = underTest.environmentToDto(source);
        assertThat(environmentDto.isEnableComputeCluster()).isTrue();
    }

    @Test
    void environmentViewToViewDtoTestBasic() {
        CredentialView credential = new CredentialView();
        Region region = new Region();
        Set<Region> regionSet = Set.of(region);
        EnvironmentTelemetry environmentTelemetry = new EnvironmentTelemetry();
        EnvironmentBackup environmentBackup = new EnvironmentBackup();
        EnvironmentAuthentication authentication = new EnvironmentAuthentication();
        AuthenticationDto authenticationDto = AuthenticationDto.builder().build();
        Set<String> freeipaRecipes = Set.of("recipe");
        ExperimentalFeatures experimentalFeatures = ExperimentalFeatures.builder().build();
        EnvironmentTags environmentTags = new EnvironmentTags(Map.of(), Map.of());
        ProxyConfigView proxyConfig = new ProxyConfigView();

        when(environmentView.getId()).thenReturn(ID);
        when(environmentView.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(environmentView.getName()).thenReturn(NAME);
        when(environmentView.getOriginalName()).thenReturn(ORIGINAL_NAME);
        when(environmentView.getDescription()).thenReturn(DESCRIPTION);
        when(environmentView.getAccountId()).thenReturn(ACCOUNT_ID);
        when(environmentView.isArchived()).thenReturn(ARCHIVED);
        when(environmentView.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AWS);
        when(environmentView.getCredential()).thenReturn(credential);
        when(environmentView.getDeletionTimestamp()).thenReturn(DELETION_TIMESTAMP);
        when(environmentView.getLocation()).thenReturn(LOCATION);
        when(environmentView.getLocationDisplayName()).thenReturn(LOCATION_DISPLAY_NAME);
        when(environmentView.getLongitude()).thenReturn(LONGITUDE);
        when(environmentView.getLatitude()).thenReturn(LATITUDE);
        when(environmentView.getRegionSet()).thenReturn(regionSet);
        when(environmentView.getTelemetry()).thenReturn(environmentTelemetry);
        when(environmentView.getBackup()).thenReturn(environmentBackup);
        when(environmentView.getStatus()).thenReturn(STATUS);
        when(environmentView.getCreator()).thenReturn(CREATOR);
        when(environmentView.getAuthentication()).thenReturn(authentication);
        when(environmentView.isCreateFreeIpa()).thenReturn(CREATE_FREE_IPA);
        when(environmentView.getFreeIpaInstanceCountByGroup()).thenReturn(FREE_IPA_INSTANCE_COUNT_BY_GROUP);
        when(environmentView.getFreeIpaInstanceType()).thenReturn(FREE_IPA_INSTANCE_TYPE);
        when(environmentView.getFreeIpaImageCatalog()).thenReturn(FREE_IPA_IMAGE_CATALOG);
        when(environmentView.getFreeIpaImageId()).thenReturn(FREE_IPA_IMAGE_ID);
        when(environmentView.isFreeIpaEnableMultiAz()).thenReturn(FREE_IPA_ENABLE_MULTI_AZ);
        when(environmentView.getFreeipaRecipes()).thenReturn(freeipaRecipes);
        when(environmentView.getParameters()).thenReturn(null);
        when(environmentView.getCreated()).thenReturn(CREATED);
        when(environmentView.getStatusReason()).thenReturn(STATUS_REASON);
        when(environmentView.getExperimentalFeaturesJson()).thenReturn(experimentalFeatures);
        when(environmentView.getEnvironmentTags()).thenReturn(environmentTags);
        when(environmentView.getCidr()).thenReturn(CIDR);
        when(environmentView.getSecurityGroupIdForKnox()).thenReturn(SECURITY_GROUP_ID_FOR_KNOX);
        when(environmentView.getDefaultSecurityGroupId()).thenReturn(DEFAULT_SECURITY_GROUP_ID);
        when(environmentView.getAdminGroupName()).thenReturn(ADMIN_GROUP_NAME);
        when(environmentView.getProxyConfig()).thenReturn(proxyConfig);
        when(environmentView.getDeletionType()).thenReturn(DELETION_TYPE);
        when(environmentView.getEnvironmentServiceVersion()).thenReturn(ENVIRONMENT_SERVICE_VERSION);
        when(environmentView.getDomain()).thenReturn(DOMAIN);
        when(environmentView.getNetwork()).thenReturn(null);
        when(environmentView.getParentEnvironment()).thenReturn(null);
        when(environmentView.isEnableSecretEncryption()).thenReturn(true);
        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        defaultComputeCluster.setCreate(true);
        when(environmentView.getDefaultComputeCluster()).thenReturn(defaultComputeCluster);

        when(authenticationDtoConverter.authenticationToDto(authentication)).thenReturn(authenticationDto);

        EnvironmentViewDto result = underTest.environmentViewToViewDto(environmentView);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getResourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.getOriginalName()).isEqualTo(ORIGINAL_NAME);
        assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.isArchived()).isEqualTo(ARCHIVED);
        assertThat(result.getCloudPlatform()).isEqualTo(CLOUD_PLATFORM_AWS);
        assertThat(result.getCredentialView()).isSameAs(credential);
        assertThat(result.getDeletionTimestamp()).isEqualTo(DELETION_TIMESTAMP);
        assertThat(result.getRegions()).isSameAs(regionSet);
        assertThat(result.getTelemetry()).isSameAs(environmentTelemetry);
        assertThat(result.getBackup()).isSameAs(environmentBackup);
        assertThat(result.getStatus()).isEqualTo(STATUS);
        assertThat(result.getCreator()).isEqualTo(CREATOR);
        assertThat(result.getAuthentication()).isSameAs(authenticationDto);
        assertThat(result.getCreated()).isEqualTo(CREATED);
        assertThat(result.getStatusReason()).isEqualTo(STATUS_REASON);
        assertThat(result.getExperimentalFeatures()).isSameAs(experimentalFeatures);
        assertThat(result.getTags()).isSameAs(environmentTags);
        assertThat(result.getAdminGroupName()).isEqualTo(ADMIN_GROUP_NAME);
        assertThat(result.getProxyConfig()).isSameAs(proxyConfig);
        assertThat(result.getDeletionType()).isEqualTo(DELETION_TYPE);
        assertThat(result.getEnvironmentServiceVersion()).isEqualTo(ENVIRONMENT_SERVICE_VERSION);
        assertThat(result.getDomain()).isEqualTo(DOMAIN);
        assertThat(result.isEnableSecretEncryption()).isTrue();
        assertThat(result.isEnableComputeCluster()).isTrue();
        assertThat(result.getEncryptionProfileName()).isNull();

        LocationDto locationDto = result.getLocation();
        assertThat(locationDto).isNotNull();
        assertThat(locationDto.getName()).isEqualTo(LOCATION);
        assertThat(locationDto.getDisplayName()).isEqualTo(LOCATION_DISPLAY_NAME);
        assertThat(locationDto.getLongitude()).isEqualTo(LONGITUDE);
        assertThat(locationDto.getLatitude()).isEqualTo(LATITUDE);

        FreeIpaCreationDto freeIpaCreationDto = result.getFreeIpaCreation();
        assertThat(freeIpaCreationDto).isNotNull();
        assertThat(freeIpaCreationDto.isCreate()).isEqualTo(CREATE_FREE_IPA);
        assertThat(freeIpaCreationDto.getInstanceCountByGroup()).isEqualTo(FREE_IPA_INSTANCE_COUNT_BY_GROUP);
        assertThat(freeIpaCreationDto.getInstanceType()).isEqualTo(FREE_IPA_INSTANCE_TYPE);
        assertThat(freeIpaCreationDto.isEnableMultiAz()).isEqualTo(FREE_IPA_ENABLE_MULTI_AZ);
        assertThat(freeIpaCreationDto.getRecipes()).isSameAs(freeipaRecipes);
        assertThat(freeIpaCreationDto.getAws()).isNull();

        SecurityAccessDto securityAccessDto = result.getSecurityAccess();
        assertThat(securityAccessDto).isNotNull();
        assertThat(securityAccessDto.getCidr()).isEqualTo(CIDR);
        assertThat(securityAccessDto.getSecurityGroupIdForKnox()).isEqualTo(SECURITY_GROUP_ID_FOR_KNOX);
        assertThat(securityAccessDto.getDefaultSecurityGroupId()).isEqualTo(DEFAULT_SECURITY_GROUP_ID);

        assertThat(result.getParameters()).isNull();
        assertThat(result.getNetwork()).isNull();
        assertThat(result.getParentEnvironmentCrn()).isNull();
        assertThat(result.getParentEnvironmentName()).isNull();
        assertThat(result.getParentEnvironmentCloudPlatform()).isNull();

        verify(environmentRecipeService, never()).getRecipes(anyLong());
    }

    @Test
    void environmentViewToViewDtoTestAwsParameters() {
        AwsParameters awsParameters = new AwsParameters();
        awsParameters.setFreeIpaSpotPercentage(FREE_IPA_SPOT_PERCENTAGE);
        awsParameters.setFreeIpaSpotMaxPrice(FREE_IPA_SPOT_MAX_PRICE);

        ReflectionTestUtils.setField(underTest, "environmentParamsConverterMap", Map.ofEntries(entry(CloudPlatform.AWS, environmentParametersConverter)));
        ParametersDto parametersDto = ParametersDto.builder().build();
        when(environmentParametersConverter.convertToDto(awsParameters)).thenReturn(parametersDto);

        when(environmentView.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AWS);
        when(environmentView.getParameters()).thenReturn(awsParameters);

        EnvironmentViewDto result = underTest.environmentViewToViewDto(environmentView);

        assertThat(result).isNotNull();
        assertThat(result.getParameters()).isSameAs(parametersDto);

        FreeIpaCreationDto freeIpaCreationDto = result.getFreeIpaCreation();
        assertThat(freeIpaCreationDto).isNotNull();

        FreeIpaCreationAwsParametersDto freeIpaCreationAwsParametersDto = freeIpaCreationDto.getAws();
        assertThat(freeIpaCreationAwsParametersDto).isNotNull();

        FreeIpaCreationAwsSpotParametersDto freeIpaCreationAwsSpotParametersDto = freeIpaCreationAwsParametersDto.getSpot();
        assertThat(freeIpaCreationAwsSpotParametersDto.getPercentage()).isEqualTo(FREE_IPA_SPOT_PERCENTAGE);
        assertThat(freeIpaCreationAwsSpotParametersDto.getMaxPrice()).isEqualTo(FREE_IPA_SPOT_MAX_PRICE);
    }

    @Test
    void environmentViewToViewDtoTestAzureParameters() {
        AzureParameters azureParameters = new AzureParameters();

        ReflectionTestUtils.setField(underTest, "environmentParamsConverterMap", Map.ofEntries(entry(CloudPlatform.AZURE, environmentParametersConverter)));
        ParametersDto parametersDto = ParametersDto.builder().build();
        when(environmentParametersConverter.convertToDto(azureParameters)).thenReturn(parametersDto);

        when(environmentView.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AZURE);
        when(environmentView.getParameters()).thenReturn(azureParameters);

        EnvironmentViewDto result = underTest.environmentViewToViewDto(environmentView);

        assertThat(result).isNotNull();
        assertThat(result.getParameters()).isSameAs(parametersDto);

        FreeIpaCreationDto freeIpaCreationDto = result.getFreeIpaCreation();
        assertThat(freeIpaCreationDto).isNotNull();
        assertThat(freeIpaCreationDto.getAws()).isNull();
    }

    @Test
    void environmentViewToViewDtoTestNetwork() {
        AwsNetwork awsNetwork = new AwsNetwork();

        ReflectionTestUtils.setField(underTest, "environmentNetworkConverterMap", Map.ofEntries(entry(CloudPlatform.AWS, environmentNetworkConverter)));
        NetworkDto networkDto = NetworkDto.builder().build();
        when(environmentNetworkConverter.convertToDto(awsNetwork)).thenReturn(networkDto);

        when(environmentView.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AWS);
        when(environmentView.getParameters()).thenReturn(null);
        when(environmentView.getNetwork()).thenReturn(awsNetwork);

        EnvironmentViewDto result = underTest.environmentViewToViewDto(environmentView);

        assertThat(result).isNotNull();
        assertThat(result.getNetwork()).isSameAs(networkDto);

        FreeIpaCreationDto freeIpaCreationDto = result.getFreeIpaCreation();
        assertThat(freeIpaCreationDto).isNotNull();
        assertThat(freeIpaCreationDto.getAws()).isNull();

        assertThat(result.getParameters()).isNull();
    }

    @Test
    void environmentViewToViewDtoTestParentEnvironment() {
        ParentEnvironmentView parentEnvironment = new ParentEnvironmentView();
        parentEnvironment.setResourceCrn(PARENT_RESOURCE_CRN);
        parentEnvironment.setName(PARENT_NAME);
        parentEnvironment.setCloudPlatform(PARENT_CLOUD_PLATFORM);

        when(environmentView.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AWS);
        when(environmentView.getParentEnvironment()).thenReturn(parentEnvironment);

        EnvironmentViewDto result = underTest.environmentViewToViewDto(environmentView);

        assertThat(result).isNotNull();
        assertThat(result.getParentEnvironmentCrn()).isEqualTo(PARENT_RESOURCE_CRN);
        assertThat(result.getParentEnvironmentName()).isEqualTo(PARENT_NAME);
        assertThat(result.getParentEnvironmentCloudPlatform()).isEqualTo(PARENT_CLOUD_PLATFORM);
    }

    @Test
    void environmentViewToViewDtoTestWithoutFreeIpaInstanceCountByGroup() {
        int expectedFreeIpaInstanceCountByGroup = 3;
        when(ipaInstanceCountByGroupProvider.getDefaultInstanceCount()).thenReturn(expectedFreeIpaInstanceCountByGroup);
        when(environmentView.getFreeIpaInstanceCountByGroup()).thenReturn(null);
        when(environmentView.getFreeIpaInstanceType()).thenReturn(FREE_IPA_INSTANCE_TYPE);
        when(environmentView.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AWS);

        EnvironmentViewDto result = underTest.environmentViewToViewDto(environmentView);

        assertThat(result).isNotNull();
        assertThat(result.getFreeIpaCreation().getInstanceCountByGroup()).isEqualTo(expectedFreeIpaInstanceCountByGroup);
        assertThat(result.getFreeIpaCreation().getInstanceType()).isEqualTo(FREE_IPA_INSTANCE_TYPE);
    }

    @Test
    void environmentToDtoTestBasic() {
        Credential credential = new Credential();
        Region region = new Region();
        Set<Region> regionSet = Set.of(region);
        EnvironmentTelemetry environmentTelemetry = new EnvironmentTelemetry();
        EnvironmentBackup environmentBackup = new EnvironmentBackup();
        EnvironmentAuthentication authentication = new EnvironmentAuthentication();
        AuthenticationDto authenticationDto = AuthenticationDto.builder().build();
        Set<String> freeipaRecipes = Set.of("recipe");
        ExperimentalFeatures experimentalFeatures = ExperimentalFeatures.builder().build();
        EnvironmentTags environmentTags = new EnvironmentTags(Map.of(), Map.of());
        ProxyConfig proxyConfig = new ProxyConfig();
        String encryptionProfileName = "epName";

        when(environment.getId()).thenReturn(ID);
        when(environment.getResourceCrn()).thenReturn(RESOURCE_CRN);
        when(environment.getName()).thenReturn(NAME);
        when(environment.getOriginalName()).thenReturn(ORIGINAL_NAME);
        when(environment.getDescription()).thenReturn(DESCRIPTION);
        when(environment.getAccountId()).thenReturn(ACCOUNT_ID);
        when(environment.isArchived()).thenReturn(ARCHIVED);
        when(environment.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AWS);
        when(environment.getCredential()).thenReturn(credential);
        when(environment.getDeletionTimestamp()).thenReturn(DELETION_TIMESTAMP);
        when(environment.getLocation()).thenReturn(LOCATION);
        when(environment.getLocationDisplayName()).thenReturn(LOCATION_DISPLAY_NAME);
        when(environment.getLongitude()).thenReturn(LONGITUDE);
        when(environment.getLatitude()).thenReturn(LATITUDE);
        when(environment.getRegionSet()).thenReturn(regionSet);
        when(environment.getTelemetry()).thenReturn(environmentTelemetry);
        when(environment.getBackup()).thenReturn(environmentBackup);
        when(environment.getStatus()).thenReturn(STATUS);
        when(environment.getCreator()).thenReturn(CREATOR);
        when(environment.getAuthentication()).thenReturn(authentication);
        when(environment.isCreateFreeIpa()).thenReturn(CREATE_FREE_IPA);
        when(environment.getFreeIpaInstanceCountByGroup()).thenReturn(FREE_IPA_INSTANCE_COUNT_BY_GROUP);
        when(environment.getFreeIpaInstanceType()).thenReturn(FREE_IPA_INSTANCE_TYPE);
        when(environment.getFreeIpaImageCatalog()).thenReturn(FREE_IPA_IMAGE_CATALOG);
        when(environment.getFreeIpaImageId()).thenReturn(FREE_IPA_IMAGE_ID);
        when(environment.isFreeIpaEnableMultiAz()).thenReturn(FREE_IPA_ENABLE_MULTI_AZ);
        when(environment.getParameters()).thenReturn(null);
        when(environment.getCreated()).thenReturn(CREATED);
        when(environment.getStatusReason()).thenReturn(STATUS_REASON);
        when(environment.getExperimentalFeaturesJson()).thenReturn(experimentalFeatures);
        when(environment.getEnvironmentTags()).thenReturn(environmentTags);
        when(environment.getCidr()).thenReturn(CIDR);
        when(environment.getSecurityGroupIdForKnox()).thenReturn(SECURITY_GROUP_ID_FOR_KNOX);
        when(environment.getDefaultSecurityGroupId()).thenReturn(DEFAULT_SECURITY_GROUP_ID);
        when(environment.getAdminGroupName()).thenReturn(ADMIN_GROUP_NAME);
        when(environment.getProxyConfig()).thenReturn(proxyConfig);
        when(environment.getDeletionType()).thenReturn(DELETION_TYPE);
        when(environment.getEnvironmentServiceVersion()).thenReturn(ENVIRONMENT_SERVICE_VERSION);
        when(environment.getDomain()).thenReturn(DOMAIN);
        when(environment.getNetwork()).thenReturn(null);
        when(environment.getParentEnvironment()).thenReturn(null);
        when(environment.isEnableSecretEncryption()).thenReturn(true);
        when(environment.getEncryptionProfileName()).thenReturn(encryptionProfileName);

        DefaultComputeCluster defaultComputeCluster = new DefaultComputeCluster();
        defaultComputeCluster.setCreate(true);
        when(environment.getDefaultComputeCluster()).thenReturn(defaultComputeCluster);
        when(authenticationDtoConverter.authenticationToDto(authentication)).thenReturn(authenticationDto);
        when(environmentRecipeService.getRecipes(ID)).thenReturn(freeipaRecipes);

        EnvironmentDto result = underTest.environmentToDto(environment);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ID);
        assertThat(result.getResourceCrn()).isEqualTo(RESOURCE_CRN);
        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.getOriginalName()).isEqualTo(ORIGINAL_NAME);
        assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.isArchived()).isEqualTo(ARCHIVED);
        assertThat(result.getCloudPlatform()).isEqualTo(CLOUD_PLATFORM_AWS);
        assertThat(result.getCredential()).isSameAs(credential);
        assertThat(result.getDeletionTimestamp()).isEqualTo(DELETION_TIMESTAMP);
        assertThat(result.getRegions()).isSameAs(regionSet);
        assertThat(result.getTelemetry()).isSameAs(environmentTelemetry);
        assertThat(result.getBackup()).isSameAs(environmentBackup);
        assertThat(result.getStatus()).isEqualTo(STATUS);
        assertThat(result.getCreator()).isEqualTo(CREATOR);
        assertThat(result.getAuthentication()).isSameAs(authenticationDto);
        assertThat(result.getCreated()).isEqualTo(CREATED);
        assertThat(result.getStatusReason()).isEqualTo(STATUS_REASON);
        assertThat(result.getExperimentalFeatures()).isSameAs(experimentalFeatures);
        assertThat(result.getTags()).isSameAs(environmentTags);
        assertThat(result.getAdminGroupName()).isEqualTo(ADMIN_GROUP_NAME);
        assertThat(result.getProxyConfig()).isSameAs(proxyConfig);
        assertThat(result.getDeletionType()).isEqualTo(DELETION_TYPE);
        assertThat(result.getEnvironmentServiceVersion()).isEqualTo(ENVIRONMENT_SERVICE_VERSION);
        assertThat(result.getDomain()).isEqualTo(DOMAIN);
        assertThat(result.isEnableSecretEncryption()).isTrue();
        assertThat(result.isEnableComputeCluster()).isTrue();

        LocationDto locationDto = result.getLocation();
        assertThat(locationDto).isNotNull();
        assertThat(locationDto.getName()).isEqualTo(LOCATION);
        assertThat(locationDto.getDisplayName()).isEqualTo(LOCATION_DISPLAY_NAME);
        assertThat(locationDto.getLongitude()).isEqualTo(LONGITUDE);
        assertThat(locationDto.getLatitude()).isEqualTo(LATITUDE);

        FreeIpaCreationDto freeIpaCreationDto = result.getFreeIpaCreation();
        assertThat(freeIpaCreationDto).isNotNull();
        assertThat(freeIpaCreationDto.isCreate()).isEqualTo(CREATE_FREE_IPA);
        assertThat(freeIpaCreationDto.getInstanceCountByGroup()).isEqualTo(FREE_IPA_INSTANCE_COUNT_BY_GROUP);
        assertThat(freeIpaCreationDto.getInstanceType()).isEqualTo(FREE_IPA_INSTANCE_TYPE);
        assertThat(freeIpaCreationDto.isEnableMultiAz()).isEqualTo(FREE_IPA_ENABLE_MULTI_AZ);
        assertThat(freeIpaCreationDto.getRecipes()).isSameAs(freeipaRecipes);
        assertThat(freeIpaCreationDto.getAws()).isNull();

        SecurityAccessDto securityAccessDto = result.getSecurityAccess();
        assertThat(securityAccessDto).isNotNull();
        assertThat(securityAccessDto.getCidr()).isEqualTo(CIDR);
        assertThat(securityAccessDto.getSecurityGroupIdForKnox()).isEqualTo(SECURITY_GROUP_ID_FOR_KNOX);
        assertThat(securityAccessDto.getDefaultSecurityGroupId()).isEqualTo(DEFAULT_SECURITY_GROUP_ID);

        assertThat(result.getParameters()).isNull();
        assertThat(result.getNetwork()).isNull();
        assertThat(result.getParentEnvironmentCrn()).isNull();
        assertThat(result.getParentEnvironmentName()).isNull();
        assertThat(result.getParentEnvironmentCloudPlatform()).isNull();

        assertThat(result.getEncryptionProfileName()).isEqualTo(encryptionProfileName);
    }

    @Test
    void environmentToDtoTestAwsParameters() {
        AwsParameters awsParameters = new AwsParameters();
        awsParameters.setFreeIpaSpotPercentage(FREE_IPA_SPOT_PERCENTAGE);
        awsParameters.setFreeIpaSpotMaxPrice(FREE_IPA_SPOT_MAX_PRICE);

        ReflectionTestUtils.setField(underTest, "environmentParamsConverterMap", Map.ofEntries(entry(CloudPlatform.AWS, environmentParametersConverter)));
        ParametersDto parametersDto = ParametersDto.builder().build();
        when(environmentParametersConverter.convertToDto(awsParameters)).thenReturn(parametersDto);

        when(environment.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AWS);
        when(environment.getParameters()).thenReturn(awsParameters);

        EnvironmentDto result = underTest.environmentToDto(environment);

        assertThat(result).isNotNull();
        assertThat(result.getParameters()).isSameAs(parametersDto);

        FreeIpaCreationDto freeIpaCreationDto = result.getFreeIpaCreation();
        assertThat(freeIpaCreationDto).isNotNull();

        FreeIpaCreationAwsParametersDto freeIpaCreationAwsParametersDto = freeIpaCreationDto.getAws();
        assertThat(freeIpaCreationAwsParametersDto).isNotNull();

        FreeIpaCreationAwsSpotParametersDto freeIpaCreationAwsSpotParametersDto = freeIpaCreationAwsParametersDto.getSpot();
        assertThat(freeIpaCreationAwsSpotParametersDto.getPercentage()).isEqualTo(FREE_IPA_SPOT_PERCENTAGE);
        assertThat(freeIpaCreationAwsSpotParametersDto.getMaxPrice()).isEqualTo(FREE_IPA_SPOT_MAX_PRICE);
    }

    @Test
    void environmentToDtoTestAzureParameters() {
        AzureParameters azureParameters = new AzureParameters();

        ReflectionTestUtils.setField(underTest, "environmentParamsConverterMap", Map.ofEntries(entry(CloudPlatform.AZURE, environmentParametersConverter)));
        ParametersDto parametersDto = ParametersDto.builder().build();
        when(environmentParametersConverter.convertToDto(azureParameters)).thenReturn(parametersDto);

        when(environment.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AZURE);
        when(environment.getParameters()).thenReturn(azureParameters);

        EnvironmentDto result = underTest.environmentToDto(environment);

        assertThat(result).isNotNull();
        assertThat(result.getParameters()).isSameAs(parametersDto);

        FreeIpaCreationDto freeIpaCreationDto = result.getFreeIpaCreation();
        assertThat(freeIpaCreationDto).isNotNull();
        assertThat(freeIpaCreationDto.getAws()).isNull();
    }

    @Test
    void environmentToDtoTestNetwork() {
        AwsNetwork awsNetwork = new AwsNetwork();

        ReflectionTestUtils.setField(underTest, "environmentNetworkConverterMap", Map.ofEntries(entry(CloudPlatform.AWS, environmentNetworkConverter)));
        NetworkDto networkDto = NetworkDto.builder().build();
        when(environmentNetworkConverter.convertToDto(awsNetwork)).thenReturn(networkDto);

        when(environment.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AWS);
        when(environment.getParameters()).thenReturn(null);
        when(environment.getNetwork()).thenReturn(awsNetwork);

        EnvironmentDto result = underTest.environmentToDto(environment);

        assertThat(result).isNotNull();
        assertThat(result.getNetwork()).isSameAs(networkDto);

        FreeIpaCreationDto freeIpaCreationDto = result.getFreeIpaCreation();
        assertThat(freeIpaCreationDto).isNotNull();
        assertThat(freeIpaCreationDto.getAws()).isNull();

        assertThat(result.getParameters()).isNull();
    }

    @Test
    void environmentToDtoTestParentEnvironment() {
        when(parentEnvironment.getResourceCrn()).thenReturn(PARENT_RESOURCE_CRN);
        when(parentEnvironment.getName()).thenReturn(PARENT_NAME);
        when(parentEnvironment.getCloudPlatform()).thenReturn(PARENT_CLOUD_PLATFORM);

        when(environment.getCloudPlatform()).thenReturn(CLOUD_PLATFORM_AWS);
        when(environment.getParentEnvironment()).thenReturn(parentEnvironment);

        EnvironmentDto result = underTest.environmentToDto(environment);

        assertThat(result).isNotNull();
        assertThat(result.getParentEnvironmentCrn()).isEqualTo(PARENT_RESOURCE_CRN);
        assertThat(result.getParentEnvironmentName()).isEqualTo(PARENT_NAME);
        assertThat(result.getParentEnvironmentCloudPlatform()).isEqualTo(PARENT_CLOUD_PLATFORM);
    }

    @Test
    void environmentToLocationDtoTest() {
        when(environment.getLocation()).thenReturn(LOCATION);
        when(environment.getLocationDisplayName()).thenReturn(LOCATION_DISPLAY_NAME);
        when(environment.getLongitude()).thenReturn(LONGITUDE);
        when(environment.getLatitude()).thenReturn(LATITUDE);

        LocationDto result = underTest.environmentToLocationDto(environment);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo(LOCATION);
        assertThat(result.getDisplayName()).isEqualTo(LOCATION_DISPLAY_NAME);
        assertThat(result.getLongitude()).isEqualTo(LONGITUDE);
        assertThat(result.getLatitude()).isEqualTo(LATITUDE);
    }

    @Test
    void creationDtoToEnvironmentTestWhenSuccessAndUserDefinedTagsArePresent() {
        LocationDto location = LocationDto.builder()
                .withLatitude(LATITUDE)
                .withLongitude(LONGITUDE)
                .withName(LOCATION)
                .withDisplayName(LOCATION_DISPLAY_NAME)
                .build();
        Map<String, Object> fluentAttributes = Map.ofEntries(entry("fluentKey1", "fluentValue1"), entry("fluentKey2", "fluentValue2"));
        EnvironmentTelemetry environmentTelemetry = new EnvironmentTelemetry();
        environmentTelemetry.setFluentAttributes(fluentAttributes);
        EnvironmentBackup environmentBackup = new EnvironmentBackup();
        environmentBackup.setStorageLocation(STORAGE_LOCATION);
        FreeIpaCreationDto freeIpaCreation = FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP)
                .withCreate(CREATE_FREE_IPA)
                .withInstanceType(FREE_IPA_INSTANCE_TYPE)
                .withImageCatalog(FREE_IPA_IMAGE_CATALOG)
                .withEnableMultiAz(FREE_IPA_ENABLE_MULTI_AZ)
                .withImageId(FREE_IPA_IMAGE_ID)
                .withSeLinux(SeLinux.ENFORCING)
                .withArchitecture(Architecture.X86_64)
                .build();
        ExperimentalFeatures experimentalFeatures = ExperimentalFeatures.builder()
                .withTunnel(Tunnel.CCMV2_JUMPGATE)
                .build();
        Set<String> regions = Set.of("region1", "region2");

        Map<String, String> userDefinedTags = Map.ofEntries(entry("userKey1", "userValue1"), entry("userKey2", "userValue2"));
        AccountTagResponse accountTagResponse = new AccountTagResponse();
        accountTagResponse.setKey("accountKey");
        accountTagResponse.setValue("accountValue");
        Map<String, String> defaultTags = Map.ofEntries(entry("defaultKey1", "defaultValue1"), entry("defaultKey2", "defaultValue2"));

        Set<String> workerNodeSubnets = Set.of("subnet1", "subnet2");
        EnvironmentCreationDto creationDto = EnvironmentCreationDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withCreator(CREATOR)
                .withName(NAME)
                .withCloudPlatform(CLOUD_PLATFORM_AWS)
                .withDescription(DESCRIPTION)
                .withLocation(location)
                .withTelemetry(environmentTelemetry)
                .withBackup(environmentBackup)
                .withFreeIpaCreation(freeIpaCreation)
                .withAdminGroupName(ADMIN_GROUP_NAME)
                .withExperimentalFeatures(experimentalFeatures)
                .withRegions(regions)
                .withTags(userDefinedTags)
                .withCrn(RESOURCE_CRN)
                .withExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                        .withCreate(true)
                        .withPrivateCluster(true)
                        .withOutboundType(OUTBOUND_TYPE)
                        .withKubeApiAuthorizedIpRanges(Set.of(KUBE_API_AUTHORIZED_IP_RANGES))
                        .withWorkerNodeSubnetIds(workerNodeSubnets)
                        .build())
                .build();

        when(environmentTagsDtoConverter.getTags(any(EnvironmentCreationDto.class)))
                .thenReturn(new Json(new EnvironmentTags(userDefinedTags, defaultTags)));

        Environment result = underTest.creationDtoToEnvironment(creationDto);

        assertThat(result).isNotNull();
        assertThat(result.getAccountId()).isEqualTo(ACCOUNT_ID);
        assertThat(result.getCreator()).isEqualTo(CREATOR);
        assertThat(result.getName()).isEqualTo(NAME);
        assertThat(result.getOriginalName()).isEqualTo(NAME);
        assertThat(result.isArchived()).isFalse();
        assertThat(result.getCloudPlatform()).isEqualTo(CLOUD_PLATFORM_AWS);
        assertThat(result.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(result.getLatitude()).isEqualTo(LATITUDE);
        assertThat(result.getLongitude()).isEqualTo(LONGITUDE);
        assertThat(result.getLocation()).isEqualTo(LOCATION);
        assertThat(result.getLocationDisplayName()).isEqualTo(LOCATION_DISPLAY_NAME);
        assertThat(result.getStatus()).isEqualTo(EnvironmentStatus.CREATION_INITIATED);
        assertThat(result.getStatusReason()).isNull();
        assertThat(result.getFreeIpaInstanceCountByGroup()).isEqualTo(FREE_IPA_INSTANCE_COUNT_BY_GROUP);
        assertThat(result.getFreeIpaInstanceType()).isEqualTo(FREE_IPA_INSTANCE_TYPE);
        assertThat(result.getFreeIpaImageCatalog()).isEqualTo(FREE_IPA_IMAGE_CATALOG);
        assertThat(result.isFreeIpaEnableMultiAz()).isEqualTo(FREE_IPA_ENABLE_MULTI_AZ);
        assertThat(result.getDeletionType()).isEqualTo(EnvironmentDeletionType.NONE);
        assertThat(result.getFreeIpaImageId()).isEqualTo(FREE_IPA_IMAGE_ID);
        assertThat(result.getAdminGroupName()).isEqualTo(ADMIN_GROUP_NAME);
        assertThat(result.getSeLinux()).isEqualTo(SeLinux.ENFORCING);
        assertThat(result.getCreated()).isPositive();
        DefaultComputeCluster defaultComputeCluster = result.getDefaultComputeCluster();
        assertNotNull(defaultComputeCluster);
        assertThat(defaultComputeCluster.isCreate()).isTrue();
        assertThat(defaultComputeCluster.isPrivateCluster()).isTrue();
        assertThat(defaultComputeCluster.getOutboundType()).isEqualTo(OUTBOUND_TYPE);
        assertThat(defaultComputeCluster.getKubeApiAuthorizedIpRanges()).isEqualTo(Set.of(KUBE_API_AUTHORIZED_IP_RANGES));
        assertThat(defaultComputeCluster.getWorkerNodeSubnetIds()).isEqualTo(workerNodeSubnets);

        EnvironmentTelemetry resultTelemetry = result.getTelemetry();
        assertThat(resultTelemetry).isNotNull();
        assertThat(resultTelemetry.getFluentAttributes()).isEqualTo(fluentAttributes);

        EnvironmentBackup resultBackup = result.getBackup();
        assertThat(resultBackup).isNotNull();
        assertThat(resultBackup.getStorageLocation()).isEqualTo(STORAGE_LOCATION);

        ExperimentalFeatures resultExperimentalFeaturesJson = result.getExperimentalFeaturesJson();
        assertThat(resultExperimentalFeaturesJson).isNotNull();
        assertThat(resultExperimentalFeaturesJson.getTunnel()).isEqualTo(Tunnel.CCMV2_JUMPGATE);

        Json tags = result.getTags();
        assertThat(tags).isNotNull();
        assertThat(tags.getValue()).isNotNull();

        EnvironmentTags environmentTags = result.getEnvironmentTags();
        assertThat(environmentTags).isNotNull();
        assertThat(environmentTags.getUserDefinedTags()).isEqualTo(userDefinedTags);
        assertThat(environmentTags.getDefaultTags()).isEqualTo(defaultTags);

        Set<Region> regionSet = result.getRegionSet();
        assertThat(regionSet).isNotNull();
        assertThat(regionSet.stream()
                .map(Region::getName)
                .collect(toSet())).isEqualTo(regions);
    }

    @Test
    void creationDtoToEnvironmentTestWhenSuccessAndUserDefinedTagsAreAbsent() {
        LocationDto location = LocationDto.builder()
                .withLatitude(LATITUDE)
                .withLongitude(LONGITUDE)
                .withName(LOCATION)
                .withDisplayName(LOCATION_DISPLAY_NAME)
                .build();
        FreeIpaCreationDto freeIpaCreation = FreeIpaCreationDto.builder(FREE_IPA_INSTANCE_COUNT_BY_GROUP).withCreate(CREATE_FREE_IPA)
                .withArchitecture(Architecture.X86_64)
                .withSeLinux(null)
                .build();
        Map<String, String> defaultTags = Map.ofEntries(entry("defaultKey1", "defaultValue1"), entry("defaultKey2", "defaultValue2"));
        EnvironmentCreationDto creationDto = EnvironmentCreationDto.builder()
                .withAccountId(ACCOUNT_ID)
                .withCreator(CREATOR)
                .withCloudPlatform(CLOUD_PLATFORM_AWS)
                .withLocation(location)
                .withFreeIpaCreation(freeIpaCreation)
                .withTags(null)
                .withCrn(RESOURCE_CRN)
                .withExternalizedComputeCluster(ExternalizedComputeClusterDto.builder()
                        .withCreate(false)
                        .build())
                .build();

        when(environmentTagsDtoConverter.getTags(any(EnvironmentCreationDto.class)))
                .thenReturn(new Json(new EnvironmentTags(new HashMap<>(), defaultTags)));

        Environment result = underTest.creationDtoToEnvironment(creationDto);

        assertThat(result).isNotNull();

        Json tags = result.getTags();
        assertThat(tags).isNotNull();
        assertThat(tags.getValue()).isNotNull();

        EnvironmentTags environmentTags = result.getEnvironmentTags();
        assertThat(environmentTags).isNotNull();
        assertThat(environmentTags.getUserDefinedTags()).isNotNull();
        assertThat(environmentTags.getUserDefinedTags()).isEmpty();
        assertThat(environmentTags.getDefaultTags()).isEqualTo(defaultTags);
        assertThat(result.getSeLinux()).isEqualTo(SeLinux.PERMISSIVE);
        DefaultComputeCluster defaultComputeCluster = result.getDefaultComputeCluster();
        assertThat(defaultComputeCluster.isCreate()).isFalse();
        assertThat(defaultComputeCluster.isPrivateCluster()).isFalse();
        assertThat(defaultComputeCluster.getOutboundType()).isNull();
        assertThat(defaultComputeCluster.getKubeApiAuthorizedIpRanges()).isEmpty();
        assertThat(defaultComputeCluster.getWorkerNodeSubnetIds()).isEmpty();
    }

    @Test
    public void testNetworkConversionWhenNetworkNotNull() {
        Environment source = new Environment();
        source.setId(1L);
        source.setFreeIpaInstanceType("large");
        source.setFreeIpaImageId("imageid");
        source.setFreeIpaImageCatalog("imagecatalog");
        source.setFreeIpaInstanceCountByGroup(1);
        source.setFreeIpaEnableMultiAz(true);
        source.setCreateFreeIpa(true);
        source.setNetwork(new AwsNetwork());
        source.setCloudPlatform("AWS");
        source.setAuthentication(new EnvironmentAuthentication());

        ReflectionTestUtils.setField(underTest, "environmentNetworkConverterMap", Map.ofEntries(entry(CloudPlatform.AWS, environmentNetworkConverter)));
        when(environmentNetworkConverter.convertToDto(any(BaseNetwork.class))).thenReturn(NetworkDto.builder().build());

        NetworkDto networkDto = underTest.networkToNetworkDto(source);

        assertThat(networkDto).isNotNull();
    }

}
