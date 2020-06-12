package com.sequenceiq.environment.environment.flow.deletion.handler.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.prerequisite.EnvironmentPrerequisiteDeleteRequest;
import com.sequenceiq.environment.environment.dto.EnvironmentDto;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupCreation;
import com.sequenceiq.environment.parameters.dao.domain.ResourceGroupUsagePattern;
import com.sequenceiq.environment.parameters.dto.AzureParametersDto;
import com.sequenceiq.environment.parameters.dto.AzureResourceGroupDto;
import com.sequenceiq.environment.parameters.dto.ParametersDto;

@ExtendWith(MockitoExtension.class)
class AzureEnvironmentPrerequisiteDeleteRequestParameterSetterTest {

    @InjectMocks
    private AzureEnvironmentPrerequisiteDeleteRequestParameterSetter underTest;

    @Mock
    private EnvironmentPrerequisiteDeleteRequest environmentPrerequisiteDeleteRequestMock;

    @Test
    void testWhenResourceGroupDtoEmptyThenDoesNotSet() {
        EnvironmentDto environmentDto = new EnvironmentDto();

        underTest.setParameters(environmentPrerequisiteDeleteRequestMock, environmentDto);

        verify(environmentPrerequisiteDeleteRequestMock, never()).withAzurePrerequisiteDeleteRequest(any());
    }

    @Test
    void testWhenResourceGroupUseMultipleThenDoesNotSet() {
        EnvironmentDto environmentDto = getEnvironmentDto(ResourceGroupUsagePattern.USE_MULTIPLE, ResourceGroupCreation.CREATE_NEW, "");

        underTest.setParameters(environmentPrerequisiteDeleteRequestMock, environmentDto);

        verify(environmentPrerequisiteDeleteRequestMock, never()).withAzurePrerequisiteDeleteRequest(any());
    }

    @Test
    void testWhenResourceGroupUseExistingThenDoesNotSet() {
        EnvironmentDto environmentDto = getEnvironmentDto(ResourceGroupUsagePattern.USE_SINGLE, ResourceGroupCreation.USE_EXISTING, "");

        underTest.setParameters(environmentPrerequisiteDeleteRequestMock, environmentDto);

        verify(environmentPrerequisiteDeleteRequestMock, never()).withAzurePrerequisiteDeleteRequest(any());
    }

    @Test
    void testWhenResourceGroupCreateNewThenResourceGroupNameIsSet() {
        EnvironmentDto environmentDto = getEnvironmentDto(ResourceGroupUsagePattern.USE_SINGLE, ResourceGroupCreation.CREATE_NEW, "resourceGroupName");
        EnvironmentPrerequisiteDeleteRequest environmentPrerequisiteDeleteRequest = new EnvironmentPrerequisiteDeleteRequest(mock(CloudCredential.class));

        underTest.setParameters(environmentPrerequisiteDeleteRequest, environmentDto);

        assertNotNull(environmentPrerequisiteDeleteRequest.getAzurePrerequisiteDeleteRequest());
        assertTrue(environmentPrerequisiteDeleteRequest.getAzurePrerequisiteDeleteRequest().isPresent());
        assertEquals("resourceGroupName", environmentPrerequisiteDeleteRequest.getAzurePrerequisiteDeleteRequest().get().getResourceGroupName());
    }

    private EnvironmentDto getEnvironmentDto(ResourceGroupUsagePattern resourceGroupUsagePattern, ResourceGroupCreation resourceGroupCreation,
            String resourceGroupName) {
        EnvironmentDto environmentDto = new EnvironmentDto();
        AzureResourceGroupDto azureResourceGroupDto = AzureResourceGroupDto.builder()
                .withResourceGroupUsagePattern(resourceGroupUsagePattern)
                .withResourceGroupCreation(resourceGroupCreation)
                .withName(resourceGroupName)
                .build();
        AzureParametersDto azureParametersDto = AzureParametersDto.builder()
                .withResourceGroup(azureResourceGroupDto)
                .build();
        ParametersDto parametersDto = ParametersDto.builder()
                .withAzureParameters(azureParametersDto)
                .build();
        environmentDto.setParameters(parametersDto);
        return environmentDto;
    }

}
