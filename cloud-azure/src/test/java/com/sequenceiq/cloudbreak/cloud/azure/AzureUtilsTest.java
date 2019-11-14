package com.sequenceiq.cloudbreak.cloud.azure;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.resources.Deployment;
import com.microsoft.azure.management.resources.DeploymentOperation;
import com.microsoft.azure.management.resources.DeploymentOperations;
import com.microsoft.azure.management.resources.TargetResource;
import com.sequenceiq.cloudbreak.cloud.azure.validator.AzurePremiumValidatorService;
import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;
import com.sequenceiq.cloudbreak.cloud.model.CloudInstance;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cloud.model.Group;
import com.sequenceiq.cloudbreak.cloud.model.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.InstanceTemplate;

@RunWith(MockitoJUnitRunner.class)
public class AzureUtilsTest {

    private static final String USER_ID = "horton@hortonworks.com";

    private static final Long WORKSPACE_ID = 1L;

    private static final String MAX_RESOURCE_NAME_LENGTH = "50";

    private static final String MICROSOFT_COMPUTE_VIRTUAL_MACHINES = "Microsoft.Compute/virtualMachines";

    private static final String VM_NAME = "vmName";

    private static final String VM_NAME_2 = "vmName2";

    private static final String STACK_NAME = "stackName";

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Mock
    private AzurePremiumValidatorService azurePremiumValidatorService;

    @Spy
    private Deployment deployment;

    @InjectMocks
    private AzureUtils underTest;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(underTest, "maxResourceNameLength", Integer.parseInt(MAX_RESOURCE_NAME_LENGTH));
    }

    @Test
    public void shouldAdjustResourceNameLengthIfItsTooLong() {
        //GIVEN
        CloudContext context = new CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1",
                USER_ID, WORKSPACE_ID);

        //WHEN
        String testResult = underTest.getStackName(context);

        //THEN
        Assert.assertNotNull("The generated name must not be null!", testResult);
        assertEquals("The resource name is not the excepted one!", "thisisaverylongazureresourcenamewhichneedstobe7899", testResult);
        assertEquals("The resource name length is wrong", testResult.length(), Integer.parseInt(MAX_RESOURCE_NAME_LENGTH));

    }

    @Test
    public void validateStorageTypeForGroupWhenPremiumStorageConfiguredAndFlavorNotPremiumThenShouldThrowCloudConnectorException() {
        thrown.expect(CloudConnectorException.class);

        String flavor = "Standard_A10";
        AzureDiskType azureDiskType = AzureDiskType.PREMIUM_LOCALLY_REDUNDANT;

        when(azurePremiumValidatorService.premiumDiskTypeConfigured(azureDiskType)).thenReturn(true);
        when(azurePremiumValidatorService.validPremiumConfiguration(flavor)).thenReturn(false);

        underTest.validateStorageTypeForGroup(azureDiskType, flavor);

        verify(azurePremiumValidatorService, times(1)).premiumDiskTypeConfigured(azureDiskType);
        verify(azurePremiumValidatorService, times(1)).validPremiumConfiguration(flavor);

    }

    @Test
    public void validateStorageTypeForGroupWhenPremiumStorageNotConfiguredThenShouldNotCallInstanceValidation() {
        String flavor = "Standard_A10";
        AzureDiskType azureDiskType = AzureDiskType.GEO_REDUNDANT;

        when(azurePremiumValidatorService.premiumDiskTypeConfigured(azureDiskType)).thenReturn(false);

        underTest.validateStorageTypeForGroup(azureDiskType, flavor);

        verify(azurePremiumValidatorService, times(1)).premiumDiskTypeConfigured(azureDiskType);
        verify(azurePremiumValidatorService, times(0)).validPremiumConfiguration(flavor);
    }

    @Test
    public void validateStorageTypeForGroupWhenPremiumStorageConfiguredAndFlavorIsPremiumThenShouldEverythinGoesFine() {
        String flavor = "Standard_DS10";
        AzureDiskType azureDiskType = AzureDiskType.PREMIUM_LOCALLY_REDUNDANT;

        when(azurePremiumValidatorService.premiumDiskTypeConfigured(azureDiskType)).thenReturn(true);
        when(azurePremiumValidatorService.validPremiumConfiguration(flavor)).thenReturn(true);

        underTest.validateStorageTypeForGroup(azureDiskType, flavor);

        verify(azurePremiumValidatorService, times(1)).validPremiumConfiguration(flavor);
        verify(azurePremiumValidatorService, times(1)).premiumDiskTypeConfigured(azureDiskType);
    }

    @Test
    public void getInstanceCloudResourcesInstancesFound() {

        TargetResource t = new TargetResource();
        t.withResourceType(MICROSOFT_COMPUTE_VIRTUAL_MACHINES);
        t.withResourceName(VM_NAME);
        CloudContext context = new CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1", USER_ID, WORKSPACE_ID);
        List<Group> groupList = new ArrayList<>();
        Group group = Mockito.mock(Group.class);
        CloudInstance instance = Mockito.mock(CloudInstance.class);
        InstanceTemplate instanceTemplate = Mockito.mock(InstanceTemplate.class);

        when(instance.getTemplate()).thenReturn(instanceTemplate);
        when(instance.getTemplate().getPrivateId()).thenReturn(1L);
        when(instance.getTemplate().getStatus()).thenReturn(InstanceStatus.CREATE_REQUESTED);
        when(group.getInstances()).thenReturn(List.of(instance));
        when(group.getName()).thenReturn("instanceGroupName");
        groupList.add(group);

        PagedList<DeploymentOperation> operationList = Mockito.spy(PagedList.class);
        DeploymentOperations operations = Mockito.mock(DeploymentOperations.class);
        DeploymentOperation operation = Mockito.mock(DeploymentOperation.class);
        operationList.add(operation);

        when(deployment.deploymentOperations()).thenReturn(operations);
        when(deployment.deploymentOperations().list()).thenReturn(operationList);
        when(operation.targetResource()).thenReturn(t);
        AzureUtils utils = new AzureUtils() {
            @Override
            public String getPrivateInstanceId(String stackName, String groupName, String privateId) {
                return VM_NAME;
            }

            @Override
            public String getStackName(CloudContext cloudContext) {
                return STACK_NAME;
            }
        };

        List<CloudResource> resourceList = utils.getInstanceCloudResources(context, deployment, groupList);

        assertEquals(1, resourceList.size());
        assertEquals(VM_NAME, resourceList.get(0).getName());

    }

    @Test
    public void getInstanceCloudResourcesInstancesNotFound() {
        TargetResource t = new TargetResource();
        t.withResourceType(MICROSOFT_COMPUTE_VIRTUAL_MACHINES);
        t.withResourceName(VM_NAME);
        CloudContext context = new CloudContext(7899L, "thisisaverylongazureresourcenamewhichneedstobeshortened", "dummy1", USER_ID, WORKSPACE_ID);
        List<Group> groupList = new ArrayList<>();
        Group group = Mockito.mock(Group.class);
        CloudInstance instance = Mockito.mock(CloudInstance.class);
        InstanceTemplate instanceTemplate = Mockito.mock(InstanceTemplate.class);

        when(instance.getTemplate()).thenReturn(instanceTemplate);
        when(instance.getTemplate().getPrivateId()).thenReturn(1L);
        when(group.getInstances()).thenReturn(List.of(instance));
        when(group.getName()).thenReturn("instanceGroupName");
        groupList.add(group);

        PagedList<DeploymentOperation> operationList = Mockito.spy(PagedList.class);
        DeploymentOperations operations = Mockito.mock(DeploymentOperations.class);
        DeploymentOperation operation = Mockito.mock(DeploymentOperation.class);
        operationList.add(operation);

        when(deployment.deploymentOperations()).thenReturn(operations);
        when(deployment.deploymentOperations().list()).thenReturn(operationList);
        when(operation.targetResource()).thenReturn(t);
        AzureUtils utils = new AzureUtils() {
            @Override
            public String getPrivateInstanceId(String stackName, String groupName, String privateId) {
                return VM_NAME_2;
            }

            @Override
            public String getStackName(CloudContext cloudContext) {
                return STACK_NAME;
            }
        };

        List<CloudResource> resourceList = utils.getInstanceCloudResources(context, deployment, groupList);

        assertEquals(0, resourceList.size());
    }
}
