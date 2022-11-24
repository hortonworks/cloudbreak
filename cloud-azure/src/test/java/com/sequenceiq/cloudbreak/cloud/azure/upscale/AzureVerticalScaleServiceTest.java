package com.sequenceiq.cloudbreak.cloud.azure.upscale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.microsoft.azure.CloudError;
import com.microsoft.azure.CloudException;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentExportResult;
import com.sequenceiq.cloudbreak.cloud.azure.AzureCloudResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.AzureInstanceTemplateOperation;
import com.sequenceiq.cloudbreak.cloud.azure.AzureResourceGroupMetadataProvider;
import com.sequenceiq.cloudbreak.cloud.azure.AzureUtils;
import com.sequenceiq.cloudbreak.cloud.azure.client.AzureClient;
import com.sequenceiq.cloudbreak.cloud.azure.connector.resource.AzureComputeResourceService;
import com.sequenceiq.cloudbreak.cloud.azure.template.AzureTemplateDeploymentService;
import com.sequenceiq.cloudbreak.cloud.azure.view.AzureStackView;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.exception.QuotaExceededException;
import com.sequenceiq.cloudbreak.cloud.model.AvailabilityZone;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.CloudResourceStatus;
import com.sequenceiq.cloudbreak.cloud.model.CloudStack;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.Location;
import com.sequenceiq.cloudbreak.cloud.model.Region;
import com.sequenceiq.cloudbreak.cloud.model.ResourceStatus;
import com.sequenceiq.cloudbreak.cloud.transform.CloudResourceHelper;
import com.sequenceiq.cloudbreak.common.exception.BadRequestException;
import com.sequenceiq.cloudbreak.service.Retry;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class AzureVerticalScaleServiceTest {

    private static final Map<String, Object> PARAMETERS = Collections.emptyMap();

    private static final String STACK_NAME = "Test Cluster";

    private static final String RESOURCE_GROUP = "resource group";

    private static final List<CloudResource> NETWORK_RESOURCES = List.of(mock(CloudResource.class));

    private static final String TEMPLATE = "template";

    @InjectMocks
    private AzureVerticalScaleService underTest;

    @Mock
    private AzureTemplateDeploymentService azureTemplateDeploymentService;

    @Mock
    private AzureUtils azureUtils;

    @Mock
    private CloudResourceHelper cloudResourceHelper;

    @Mock
    private AzureComputeResourceService azureComputeResourceService;

    @Mock
    private AzureClient client;

    @Mock
    private AzureResourceGroupMetadataProvider azureResourceGroupMetadataProvider;

    @Mock
    private CloudStack stack;

    @Mock
    private AzureStackView azureStackView;

    @Mock
    private Deployment templateDeployment;

    @Mock
    private AzureCloudResourceService azureCloudResourceService;

    @Mock
    private AzureScaleUtilService azureScaleUtilService;

    @Before
    public void before() {
        when(azureUtils.getStackName(any(CloudContext.class))).thenReturn(STACK_NAME);
    }

    @Test
    public void testVerticalScaleThenThereAreNewInstancesRequired() throws QuotaExceededException {
        CloudContext cloudContext = createCloudContext();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, null);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);

        when(azureTemplateDeploymentService.getTemplateDeployment(any(), any(), any(), any(), any()))
                .thenReturn(templateDeployment);
        when(templateDeployment.exportTemplate()).thenReturn(mock(DeploymentExportResult.class));
        when(azureScaleUtilService.getArmTemplate(anyList(), anyString())).thenReturn(template);

        List<CloudResourceStatus> actual = underTest.verticalScale(ac, stack, resources, azureStackView, client);

        assertFalse(actual.isEmpty());
        assertEquals(template, actual.get(0).getCloudResource());
        assertEquals(ResourceStatus.IN_PROGRESS, actual.get(0).getStatus());

        verify(azureTemplateDeploymentService).getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.VERTICAL_SCALE);
        verify(templateDeployment).exportTemplate();
        verify(azureUtils).getStackName(any(CloudContext.class));
    }

    @Test
    public void testVerticalScaleButQuotaIssueHappen() throws QuotaExceededException {
        CloudContext cloudContext = createCloudContext();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, null);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);

        CloudError cloudError = new CloudError();
        CloudError quotaError = new CloudError();
        quotaError.withCode("QuotaExceeded");
        quotaError.withMessage("Operation could not be completed as it results in exceeding approved standardNCPromoFamily Cores quota. " +
                "Additional details - Deployment Model: Resource Manager, Location: westus2, Current Limit: 200, Current Usage: 24, Additional Required: 600," +
                " (Minimum) New Limit Required: 624. Submit a request for Quota increase at https://aka" +
                ".ms/ProdportalCRP/#blade/Microsoft_Azure_Capacity/UsageAndQuota.ReactView/Parameters/%7B%22subscriptionId%22:" +
                "%223ddda1c7-d1f5-4e7b-ac81-0523f483b3b3%22,%22command%22:%22openQuotaApprovalBlade%22,%22quotas%22:[%7B%22location%22:%22westus2%22,%22" +
                "providerId%22:%22Microsoft.Compute%22,%22resourceName%22:%22standardNCPromoFamily%22,%22quotaRequest%22:%7B%22properties%22:%7B%22limit%22:" +
                "624,%22unit%22:%22Count%22,%22name%22:%7B%22value%22:%22standardNCPromoFamily%22%7D%7D%7D%7D]%7D by specifying parameters listed in the " +
                "'Details' section for deployment to succeed. Please read more about quota limits at https://docs.microsoft.com/en-us/azure/" +
                "azure-supportability/per-vm-quota-requests");
        cloudError.details().add(quotaError);
        CloudException cloudException = new CloudException("", null, cloudError);
        when(azureScaleUtilService.getArmTemplate(anyList(), anyString())).thenThrow(cloudException);
        doThrow(new QuotaExceededException(200, 24, 600, "QuotaExceeded", new BadRequestException("")))
                .when(azureScaleUtilService).checkIfQuotaLimitIssued(cloudException);

        QuotaExceededException quotaExceededException = Assertions.assertThrows(QuotaExceededException.class, () -> {
            underTest.verticalScale(ac, stack, resources, azureStackView, client);
        });
        assertEquals(200, quotaExceededException.getCurrentLimit());
        assertEquals(24, quotaExceededException.getCurrentUsage());
        assertEquals(600, quotaExceededException.getAdditionalRequired());
    }

    @Test
    public void testVerticalScaleWhenVmsNotStartedInTime() {
        CloudContext cloudContext = createCloudContext();
        AuthenticatedContext ac = new AuthenticatedContext(cloudContext, null);
        CloudResource template = createCloudResource(TEMPLATE, ResourceType.ARM_TEMPLATE);
        List<CloudResource> resources = List.of(createCloudResource("volumes", ResourceType.AZURE_VOLUMESET), template);

        CloudError cloudError = new CloudError().withCode("code").withMessage("Error happened");
        cloudError.details().add(new CloudError().withCode("code").withMessage("Please check the power state later"));
        when(azureTemplateDeploymentService.getTemplateDeployment(client, stack, ac, azureStackView, AzureInstanceTemplateOperation.VERTICAL_SCALE))
                .thenThrow(new Retry.ActionFailedException("VMs not started in time.", new CloudException("Error", null, cloudError)));
        when(azureUtils.convertToCloudConnectorException(any(CloudException.class), anyString())).thenCallRealMethod();
        when(azureUtils.convertToCloudConnectorException(any(Throwable.class), anyString())).thenCallRealMethod();
        CloudConnectorException cloudConnectorException = assertThrows(CloudConnectorException.class, () ->
                underTest.verticalScale(ac, stack, resources, azureStackView, client)
        );

        assertThat(cloudConnectorException.getMessage())
                .contains("Stack upscale failed, status code code, error message: Error happened, details: Please check the power state later");
    }

    private List<Group> createScaledGroups() {
        Group group = mock(Group.class);
        return Collections.singletonList(group);
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
        return new CloudResource.Builder()
                .withName(name)
                .withStatus(status)
                .withType(resourceType)
                .withInstanceId(instanceId)
                .withParams(PARAMETERS)
                .build();
    }
}
