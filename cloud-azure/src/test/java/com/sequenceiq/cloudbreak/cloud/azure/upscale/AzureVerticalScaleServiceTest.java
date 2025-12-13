package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.management.exception.ManagementError;
import com.azure.resourcemanager.compute.models.ApiError;
import com.azure.resourcemanager.compute.models.ApiErrorException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTestUtils;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.AzureVirtualMachineService;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class AzureVerticalScaleServiceTest {

    private static final Map<String, Object> PARAMETERS = Collections.emptyMap();

    private static final String STACK_NAME = "Test Cluster";

    private static final String RESOURCE_GROUP = "resource group";

    private static final List<CloudResource> NETWORK_RESOURCES = List.of(mock(CloudResource.class));

    private static final String TEMPLATE = "template";

    @InjectMocks
    private AzureVerticalScaleService underTest;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureClient client;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private CloudStack stack;

    @Mock
    private AzureStackView azureStackView;

    @Mock
    private AzureVirtualMachineService azureVirtualMachineService;

    @BeforeEach
    void before() {
        when(azureUtils.getStackName(any(CloudContext.class))).thenReturn(STACK_NAME);
    }

    @Test
    void testVerticalScaleWhenAzureAnswersTheVerticalScaleShouldHappen() throws QuotaExceededException {
        CloudContext cloudContext = createCloudContext();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, null);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);

        when(azureUtils.getInstanceList(any(CloudStack.class)))
                .thenReturn(new ArrayList<>());
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(CloudContext.class), any(CloudStack.class)))
                .thenReturn("group");
        when(azureVirtualMachineService.getVirtualMachinesByName(any(), any(), any())).thenReturn(new HashMap<>());

        List<CloudResourceStatus> actual = underTest.verticalScale(ac, stack, resources, azureStackView, client, Optional.empty());

        assertEquals(0, actual.size());
    }

    @Test
    void testVerticalScaleWhenDropExceptionThenVerticalScaleDoesNotHappen() {
        CloudContext cloudContext = createCloudContext();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, null);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);

        ApiError cloudError = AzureTestUtils.apiError("code", "Error happened");
        List<ManagementError> details = new ArrayList<>();
        AzureTestUtils.setDetails(cloudError, details);
        ManagementError managementError = AzureTestUtils.managementError("code", "Please check the power state later");
        details.add(managementError);
        when(azureUtils.getInstanceList(any(CloudStack.class)))
                .thenThrow(new Retry.ActionFailedException("VMs not started in time.", new ApiErrorException("Error", null, cloudError)));
        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class, () ->
                underTest.verticalScale(ac, stack, resources, azureStackView, client, Optional.empty())
        );

        assertThat(cloudConnectorException.getMessage())
                .contains("Could not upscale Azure infrastructure: Test Cluster, VMs not started in time.");
    }

    private CloudContext createCloudContext() {
        Location location = Location.location(Region.region("us-west-1"), AvailabilityZone.availabilityZone("us-west-1"));
        return CloudContext.Builder.builder()
                .withName(STACK_NAME)
                .withLocation(location)
                .build();
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType) {
        return createCloudResource(name, resourceType, CommonStatus.CREATED, null);
    }

    private CloudResource createCloudResource(String name, ResourceType resourceType, CommonStatus status, String instanceId) {
        return CloudResource.builder()
                .withName(name)
                .withStatus(status)
                .withType(resourceType)
                .withInstanceId(instanceId)
                .withParameters(PARAMETERS)
                .build();
    }
}
