package com.sequenceiq.environment.environment.encryption;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Platform;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.Variant;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetDeletionRequest;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.service.EnvironmentTagProvider;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;
import com.sequenceiq.environment.resourcepersister.CloudResourceRetrieverService;

@ExtendWith(MockitoExtension.class)
class EnvironmentEncryptionServiceTest {

    private static final String CLOUD_PLATFORM = "AZURE";

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource";

    private static final String ENVIRONMENT_NAME = "envName";

    private static final String REGION = "dummyRegion";

    private static final String USER_NAME = "dummyUser";

    private static final String ACCOUNT_ID = "dummyAccountId";

    private static final String DISK_ENCRYPTION_SET_ID = "dummyDesId";

    private static final String DISK_ENCRYPTION_SET_NAME = "dummyDesName";

    private static final long ENVIRONMENT_ID = 1L;

    private static final String KEY_URL = "dummyKeyUrl";

    private static final String RESOURCE_GROUP_NAME = "dummyRG";

    @Mock
    private CloudConnector<Object> cloudConnector;

    @Mock
    private EncryptionResources encryptionResources;

    @Mock
    private EnvironmentTagProvider environmentTagProvider;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @Mock
    private CloudResourceRetrieverService resourceRetriever;

    @InjectMocks
    private EnvironmentEncryptionService underTest;

    @Captor
    private ArgumentCaptor<DiskEncryptionSetDeletionRequest> diskEncryptionSetDeletionRequestCaptor;

    @Mock
    private Credential credential;

    @Mock
    private CloudCredential cloudCredential;

    @Test
    void testCreateEncryptionResourcesCreationRequestShouldReturnWithANewEncryptionResourcesCreationRequest() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withResourceCrn(ENVIRONMENT_CRN)
                .withId(ENVIRONMENT_ID)
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(credential)
                .withLocationDto(LocationDto.builder().withName(REGION).build())
                .withParameters(ParametersDto.builder()
                        .withAzureParameters(AzureParametersDto.builder()
                                .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                        .withEncryptionKeyUrl(KEY_URL).build())
                                .withResourceGroup(AzureResourceGroupDto.builder()
                                        .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                        .withName(RESOURCE_GROUP_NAME).build())
                                .build())
                        .build())
                .withCreator(USER_NAME)
                .withAccountId(ACCOUNT_ID)
                .build();
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);
        Map<String, String> tags = Map.ofEntries(entry("tag1", "value1"), entry("tag2", "value2"));
        when(environmentTagProvider.getTags(environmentDto, ENVIRONMENT_CRN)).thenReturn(tags);

        DiskEncryptionSetCreationRequest creationRequest = underTest.createEncryptionResourcesCreationRequest(environmentDto);

        assertEquals(creationRequest.getEncryptionKeyUrl(), KEY_URL);
        assertEquals(creationRequest.getResourceGroupName(), RESOURCE_GROUP_NAME);
        assertTrue(creationRequest.isSingleResourceGroup());
        verifyCloudContext(creationRequest.getCloudContext());
        assertThat(creationRequest.getCloudCredential()).isSameAs(cloudCredential);
        assertThat(creationRequest.getId()).isEqualTo("randomGeneratedResource");
        assertThat(creationRequest.getTags()).isSameAs(tags);
    }

    @Test
    void testCreateEncryptionResourcesShouldCallCreateDiskEncryptionSetWhenCloudPlatformAzure() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.encryptionResources()).thenReturn(encryptionResources);
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withResourceCrn(ENVIRONMENT_CRN)
                .withId(ENVIRONMENT_ID)
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(credential)
                .withLocationDto(LocationDto.builder().withName(REGION).build())
                .withParameters(ParametersDto.builder()
                        .withAzureParameters(AzureParametersDto.builder()
                                .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                        .withEncryptionKeyUrl(KEY_URL).build())
                                .withResourceGroup(AzureResourceGroupDto.builder()
                                        .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                        .withName(RESOURCE_GROUP_NAME).build())
                                .build())
                        .build())
                .build();
        CreatedDiskEncryptionSet dummyDes = new CreatedDiskEncryptionSet.Builder()
                .withDiskEncryptionSetId(DISK_ENCRYPTION_SET_ID)
                .build();
        when(encryptionResources.createDiskEncryptionSet(any(DiskEncryptionSetCreationRequest.class))).thenReturn(dummyDes);

        CreatedDiskEncryptionSet createdDes = underTest.createEncryptionResources(environmentDto);

        assertEquals(createdDes.getDiskEncryptionSetId(), DISK_ENCRYPTION_SET_ID);
    }

    @Test
    void testShouldNotThrowExceptionWhenCloudPlatformAzure() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.encryptionResources()).thenReturn(encryptionResources);

        EncryptionResources encryptionResources = underTest.getEncryptionResources(CLOUD_PLATFORM);

        assertNotNull(encryptionResources);
    }

    @Test
    void testShouldThrowExceptionWhenCloudPlatformNotAzure() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);

        EncryptionResourcesNotFoundException exception = assertThrows(EncryptionResourcesNotFoundException.class,
                () -> underTest.getEncryptionResources("NotAzure"));

        assertEquals("No Encryption resources component found for cloud platform: NotAzure", exception.getMessage());
    }

    @Test
    void testCreateEncryptionResourcesDeletionRequestShouldReturnWithANewDeletionRequest() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withResourceCrn(ENVIRONMENT_CRN)
                .withId(ENVIRONMENT_ID)
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(credential)
                .withLocationDto(LocationDto.builder()
                        .withName(REGION)
                        .build())
                .withParameters(ParametersDto.builder()
                        .withAzureParameters(AzureParametersDto.builder()
                                .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                        .withDiskEncryptionSetId(DISK_ENCRYPTION_SET_ID)
                                        .withEncryptionKeyUrl(KEY_URL)
                                        .build())
                                .build())
                        .build())
                .withCreator(USER_NAME)
                .withAccountId(ACCOUNT_ID)
                .build();
        CloudResource desCloudResource = CloudResource.builder()
                .name(DISK_ENCRYPTION_SET_NAME)
                .type(ResourceType.AZURE_DISK_ENCRYPTION_SET)
                .reference(DISK_ENCRYPTION_SET_ID)
                .status(CommonStatus.CREATED)
                .build();
        when(resourceRetriever.findByEnvironmentIdAndType(ENVIRONMENT_ID, ResourceType.AZURE_DISK_ENCRYPTION_SET))
                .thenReturn(Optional.of(desCloudResource));
        when(credentialToCloudCredentialConverter.convert(credential)).thenReturn(cloudCredential);

        DiskEncryptionSetDeletionRequest deletionRequest = underTest.createEncryptionResourcesDeletionRequest(environmentDto);

        Optional<CloudResource> dummyResourceOptional = deletionRequest.getCloudResources().stream()
                .filter(r -> r.getType() == ResourceType.AZURE_DISK_ENCRYPTION_SET)
                .findFirst();
        assertNotNull(dummyResourceOptional);
        assertThat(dummyResourceOptional).isNotEmpty();
        assertEquals(dummyResourceOptional.get().getReference(), DISK_ENCRYPTION_SET_ID);

        verifyCloudContext(deletionRequest.getCloudContext());
        assertThat(deletionRequest.getCloudCredential()).isSameAs(cloudCredential);
    }

    private void verifyCloudContext(CloudContext cloudContext) {
        assertThat(cloudContext).isNotNull();
        assertThat(cloudContext.getId()).isEqualTo(ENVIRONMENT_ID);
        assertThat(cloudContext.getName()).isEqualTo(ENVIRONMENT_NAME);
        assertThat(cloudContext.getCrn()).isEqualTo(ENVIRONMENT_CRN);
        Platform platform = cloudContext.getPlatform();
        assertThat(platform).isNotNull();
        assertThat(platform.value()).isEqualTo(CLOUD_PLATFORM);
        Variant variant = cloudContext.getVariant();
        assertThat(variant).isNotNull();
        assertThat(variant.value()).isEqualTo(CLOUD_PLATFORM);
        Location location = cloudContext.getLocation();
        assertThat(location).isNotNull();
        Region region = location.getRegion();
        assertThat(region).isNotNull();
        assertThat(region.getRegionName()).isEqualTo(REGION);
        assertThat(cloudContext.getUserId()).isEqualTo(USER_NAME);
        assertThat(cloudContext.getUserName()).isEqualTo(USER_NAME);
        assertThat(cloudContext.getAccountId()).isEqualTo(ACCOUNT_ID);
    }

    @Test
    void testDeleteEncryptionResourcesShouldCallDeleteDiskEncryptionSetWhenCloudPlatformAzure() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.encryptionResources()).thenReturn(encryptionResources);
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withResourceCrn(ENVIRONMENT_CRN)
                .withId(ENVIRONMENT_ID)
                .withName(ENVIRONMENT_NAME)
                .withCloudPlatform(CLOUD_PLATFORM)
                .withCredential(credential)
                .withLocationDto(LocationDto.builder()
                        .withName(REGION)
                        .build())
                .withParameters(ParametersDto.builder()
                        .withAzureParameters(AzureParametersDto.builder()
                                .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                        .withDiskEncryptionSetId(DISK_ENCRYPTION_SET_ID)
                                        .withEncryptionKeyUrl(KEY_URL)
                                        .build())
                                .build())
                        .build())
                .withCreator(USER_NAME)
                .withAccountId(ACCOUNT_ID)
                .withCredential(credential)
                .build();
        CloudResource desCloudResource = CloudResource.builder()
                .name(DISK_ENCRYPTION_SET_NAME)
                .type(ResourceType.AZURE_DISK_ENCRYPTION_SET)
                .reference(DISK_ENCRYPTION_SET_ID)
                .status(CommonStatus.CREATED)
                .build();
        when(resourceRetriever.findByEnvironmentIdAndType(ENVIRONMENT_ID, ResourceType.AZURE_DISK_ENCRYPTION_SET))
                .thenReturn(Optional.of(desCloudResource));

        underTest.deleteEncryptionResources(environmentDto);

        verify(encryptionResources).deleteDiskEncryptionSet(diskEncryptionSetDeletionRequestCaptor.capture());
        verifyDiskEncryptionSetDeletionRequest();
    }

    private void verifyDiskEncryptionSetDeletionRequest() {
        DiskEncryptionSetDeletionRequest deletionRequest = diskEncryptionSetDeletionRequestCaptor.getValue();
        CloudContext cloudContext = deletionRequest.getCloudContext();
        List<CloudResource> cloudResources = deletionRequest.getCloudResources();
        Optional<CloudResource> desCloudResourceOptional = cloudResources.stream()
                .filter(r -> r.getType() == ResourceType.AZURE_DISK_ENCRYPTION_SET)
                .findFirst();
        assertThat(desCloudResourceOptional).isNotEmpty();
        CloudResource desCloudResource = desCloudResourceOptional.get();
        assertEquals(desCloudResource.getReference(), DISK_ENCRYPTION_SET_ID);
        assertEquals(desCloudResource.getName(), DISK_ENCRYPTION_SET_NAME);
        assertEquals(desCloudResource.getType(), ResourceType.AZURE_DISK_ENCRYPTION_SET);
        assertEquals(cloudContext.getAccountId(), ACCOUNT_ID);
        assertEquals(cloudContext.getCrn(), ENVIRONMENT_CRN);
    }

}
