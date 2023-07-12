package com.sequenceiq.cloudbreak.cloud.azure.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.management.exception.ManagementError;
import com.azure.resourcemanager.compute.models.ApiError;
import com.azure.resourcemanager.compute.models.ApiErrorException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureImage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTestUtils;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImageProviderService;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzureImageFormatValidator;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.cloudbreak.service.RetryService;

@ExtendWith(MockitoExtension.class)
class AzureTemplateDeploymentServiceTest {

    @Mock
    private AzureStorage azureStorage;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private AzureMarketplaceImageProviderService azureMarketplaceImageProviderService;

    @Mock
    private AzureImageFormatValidator azureImageFormatValidator;

    @Mock
    private AzureTemplateBuilder azureTemplateBuilder;

    @Mock
    private RetryService retry;

    @InjectMocks
    private AzureTemplateDeploymentService underTest;

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    public void testGetTemplateDeploymentWhenFailed(boolean hasSourceImagePlan) {
        CloudStack stack = mock(CloudStack.class);
        Image image = mock(Image.class);
        CloudContext cloudContext = CloudContext.Builder.builder().withId(1L).build();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, new CloudCredential());
        AzureStackView stackView = mock(AzureStackView.class);

        when(stack.getImage()).thenReturn(image);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(CloudContext.class), any(CloudStack.class))).thenReturn("rg1");
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(false);
        when(azureImageFormatValidator.hasSourceImagePlan(any(Image.class))).thenReturn(hasSourceImagePlan);
        when(azureStorage.getCustomImage(any(), any(), any())).thenReturn(new AzureImage("1", "image1", true));
        when(azureTemplateBuilder.build(eq("stack1"), eq("1"), any(), any(), any(), any(), any(), any())).thenReturn("template");
        when(retry.testWith1SecDelayMax5Times(any(Supplier.class))).thenAnswer(invocation -> invocation.getArgument(0, Supplier.class).get());
        when(azureUtils.getStackName(any())).thenReturn("stack1");
        AzureClient client = mock(AzureClient.class);
        ApiError cloudError = AzureTestUtils.apiError(null, "Error happened");
        List<ManagementError> details = new ArrayList<>();
        AzureTestUtils.setDetails(cloudError, details);
        ManagementError managementError = AzureTestUtils.managementError(null, "Please check the power state later");
        details.add(managementError);
        when(client.createTemplateDeployment(eq("rg1"), eq("stack1"), eq("template"), any()))
                .thenThrow(new ApiErrorException("Error", null, cloudError));

        Retry.ActionFailedException actionFailedException = assertThrows(Retry.ActionFailedException.class,
                () -> underTest.getTemplateDeployment(client, stack, ac, stackView, AzureInstanceTemplateOperation.PROVISION));

        verify(azureMarketplaceImageProviderService, hasSourceImagePlan ? times(1) : never()).getSourceImage(eq(image));
        assertEquals("VMs not started in time.", actionFailedException.getMessage());
    }

}