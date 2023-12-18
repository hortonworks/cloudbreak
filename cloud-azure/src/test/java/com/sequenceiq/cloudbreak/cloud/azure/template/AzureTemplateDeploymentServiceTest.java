package com.sequenceiq.cloudbreak.cloud.azure.template;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.azure.core.management.exception.ManagementError;
import com.azure.resourcemanager.compute.models.ApiError;
import com.azure.resourcemanager.compute.models.ApiErrorException;
import com.sequenceiq.cloudbreak.cloud.azure.AzureImage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStackViewProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureStorage;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTemplateBuilder;
import com.sequenceiq.cloudbreak.cloud.azure.AzureTestUtils;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.image.marketplace.AzureMarketplaceImage;
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

    @Mock
    private AzureStackViewProvider azureStackViewProvider;

    @InjectMocks
    private AzureTemplateDeploymentService underTest;

    private static Stream<Arguments> marketplaceImageFlags() {
        return Stream.of(
                Arguments.of(true, true),
                Arguments.of(true, false),
                Arguments.of(false, true),
                Arguments.of(false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("marketplaceImageFlags")
    public void testGetTemplateDeploymentWhenFailed(boolean hasSourceImagePlan, boolean marketplaceFormat) {

        AzureMarketplaceImage azureMarketplaceImage = new AzureMarketplaceImage("cloudera", "my-offer", "my-plan", "my-version", true);
        CloudStack stack = mock(CloudStack.class);
        Image image = mock(Image.class);
        CloudContext cloudContext = CloudContext.Builder.builder().withId(1L).build();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, new CloudCredential());
        AzureStackView stackView = mock(AzureStackView.class);

        when(stack.getImage()).thenReturn(image);
        when(azureResourceGroupMetadataProvider.getResourceGroupName(any(CloudContext.class), any(CloudStack.class))).thenReturn("rg1");
        when(azureImageFormatValidator.isMarketplaceImageFormat(image)).thenReturn(marketplaceFormat);
        lenient().when(azureImageFormatValidator.hasSourceImagePlan(any(Image.class))).thenReturn(hasSourceImagePlan);
        lenient().when(azureMarketplaceImageProviderService.getSourceImage(any(Image.class))).thenReturn(azureMarketplaceImage);
        lenient().when(azureMarketplaceImageProviderService.get(any(Image.class))).thenReturn(azureMarketplaceImage);
        lenient().when(azureStorage.getCustomImage(any(), any(), any())).thenReturn(new AzureImage("1", "image1", true));
        when(azureTemplateBuilder.build(eq("stack1"), any(), any(), any(), any(), any(), any(), any())).thenReturn("template");
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

        assertEquals("VMs not started in time.", actionFailedException.getMessage());
        if (marketplaceFormat) {
            verify(azureMarketplaceImageProviderService).get(eq(image));
            verify(azureTemplateBuilder).build(eq("stack1"), any(), any(), any(), any(), any(), any(), eq(azureMarketplaceImage));
        } else {
            verify(azureMarketplaceImageProviderService, hasSourceImagePlan ? times(1) : never()).getSourceImage(eq(image));
            verify(azureTemplateBuilder).build(eq("stack1"), any(), any(), any(), any(), any(), any(),
                    hasSourceImagePlan ? eq(azureMarketplaceImage) : eq(null));
        }
    }

    @Test
    void runWhatIfAnalysisShouldReturnManagementErrorWhenClientFails() {
        CloudStack stack = mock(CloudStack.class);
        AzureClient azureClient = mock(AzureClient.class);
        CloudContext cloudContext = CloudContext.Builder.builder().withId(1L).build();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, new CloudCredential());
        AzureStackView stackView = mock(AzureStackView.class);
        String resourceGroupName = "rg-1";
        String deploymentName = "whatif-1";

        when(azureStorage.getCustomImage(any(), any(), any())).thenReturn(new AzureImage("1", "image1", true));
        when(azureUtils.generateResourceGroupNameByNameAndId("whatif", "1")).thenReturn(deploymentName);
        when(azureTemplateBuilder.build(eq(deploymentName), any(), any(), any(), any(), any(), any(), any())).thenReturn("template");
        when(azureResourceGroupMetadataProvider.getResourceGroupName(cloudContext, stack)).thenReturn(resourceGroupName);
        when(azureStackViewProvider.getAzureStack(any(), any(), any(), any())).thenReturn(stackView);

        underTest.runWhatIfAnalysis(azureClient, stack, ac);

        verify(azureClient).runWhatIfAnalysis(resourceGroupName, deploymentName, "template");
    }

}





