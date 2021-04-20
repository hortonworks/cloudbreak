package com.sequenceiq.environment.environment.encryption;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.EncryptionResources;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.encryption.CreatedDiskEncryptionSet;
import com.sequenceiq.cloudbreak.cloud.model.encryption.DiskEncryptionSetCreationRequest;
import com.sequenceiq.environment.credential.domain.Credential;
import com.sequenceiq.environment.credential.v1.converter.CredentialToCloudCredentialConverter;
import com.sequenceiq.environment.environment.domain.Environment;
import com.sequenceiq.environment.environment.domain.EnvironmentTags;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.environment.dto.LocationDto;
import com.sequenceiq.environment.environment.service.EnvironmentTagProvider;
import com.sequenceiq.environment.parameter.dto.AzureParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceEncryptionParametersDto;
import com.sequenceiq.environment.parameter.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameter.dto.ParametersDto;
import com.sequenceiq.environment.parameter.dto.ResourceGroupUsagePattern;

@ExtendWith(MockitoExtension.class)
class EnvironmentEncryptionServiceTest {

    private static final String CLOUD_PLATFORM = "AZURE";

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

    @InjectMocks
    private EnvironmentEncryptionService underTest;

    @Test
    void testCreateEncryptionResourcesCreationRequestShouldReturnWithANewEncryptionResourcesCreationRequest() {
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withResourceCrn("crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource")
                .withId(1L)
                .withName("envName")
                .withCloudPlatform("AZURE")
                .withCredential(new Credential())
                .withLocationDto(LocationDto.builder().withName("DummyRegion").build())
                .withParameters(ParametersDto.builder()
                        .withAzureParameters(AzureParametersDto.builder()
                                .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                        .withEncryptionKeyUrl("dummyKeyUrl").build())
                                .withResourceGroup(AzureResourceGroupDto.builder()
                                        .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                        .withName("dummyRG").build())
                                .build())
                        .build())
                .withTags(new EnvironmentTags(new HashMap<>(), new HashMap<>()))
                .build();
        DiskEncryptionSetCreationRequest createdDes = underTest.createEncryptionResourcesCreationRequest(environmentDto);

        assertEquals(createdDes.getEncryptionKeyUrl(), "dummyKeyUrl");
        assertEquals(createdDes.getEnvironmentId(), 1L);
        assertEquals(createdDes.getRegion(), Region.region("DummyRegion"));
        assertEquals(createdDes.getResourceGroup(), "dummyRG");
        assertEquals(createdDes.isSingleResourceGroup(), true);
    }

    @Test
    void testCreateEncryptionResourcesShouldCallCreateDiskEncryptionSetWhenCloudPlatformAzure() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.encryptionResources()).thenReturn(encryptionResources);
        EnvironmentDto environmentDto = EnvironmentDto.builder()
                .withResourceCrn("crn:cdp:environments:us-west-1:dummyUser:environment:randomGeneratedResource")
                .withId(1L)
                .withName("envName")
                .withCloudPlatform("AZURE")
                .withCredential(new Credential())
                .withLocationDto(LocationDto.builder().withName("DummyRegion").build())
                .withParameters(ParametersDto.builder()
                        .withAzureParameters(AzureParametersDto.builder()
                                .withEncryptionParameters(AzureResourceEncryptionParametersDto.builder()
                                        .withEncryptionKeyUrl("dummyKeyUrl").build())
                                .withResourceGroup(AzureResourceGroupDto.builder()
                                        .withResourceGroupUsagePattern(ResourceGroupUsagePattern.USE_SINGLE)
                                        .withName("dummyRG").build())
                                .build())
                        .build())
                .withTags(new EnvironmentTags(new HashMap<>(), new HashMap<>()))
                .build();
        Environment env = new Environment();
        env.setCloudPlatform("AZURE");
        CreatedDiskEncryptionSet dummyDes = new CreatedDiskEncryptionSet.Builder()
                .withDiskEncryptionSetId("dummyDesId")
                .build();
        when(encryptionResources.createDiskEncryptionSet(any(DiskEncryptionSetCreationRequest.class))).thenReturn(dummyDes);
        CreatedDiskEncryptionSet createdDes = underTest.createEncryptionResources(environmentDto, env);
        assertEquals(createdDes.getDiskEncryptionSetId(), "dummyDesId");
    }

    @Test
    void testShouldNotThrowExceptionWhenCloudPlatformAzure() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        when(cloudConnector.encryptionResources()).thenReturn(encryptionResources);
        EncryptionResources encryptionResources = underTest.getEncryptionResources("AZURE");
        assertNotNull(encryptionResources);
    }

    @Test
    void testShouldThrowExceptionWhenCloudPlatformNotAzure() {
        when(cloudPlatformConnectors.get(any(CloudPlatformVariant.class))).thenReturn(cloudConnector);
        EncryptionResourcesNotFoundException exception = assertThrows(EncryptionResourcesNotFoundException.class,
                () -> underTest.getEncryptionResources("NotAzure"));
        assertEquals("No Encryption resources component found for cloud platform: NotAzure", exception.getMessage());
    }
}
