package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsCommonDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.common.model.AwsDiskType;

import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeModification;
import software.amazon.awssdk.services.ec2.model.VolumeModificationState;
import software.amazon.awssdk.services.ec2.model.VolumeState;
import software.amazon.awssdk.services.ec2.model.VolumeType;

/**
 * Unit tests for AwsGp2ToGp3PatchService.
 * <p>
 * Note: These tests focus on basic functionality that can be tested without AWS integration.
 * Full integration tests with actual AWS API calls should be done in integration test suites.
 */
@ExtendWith(MockitoExtension.class)
class AwsGp2ToGp3PatchServiceTest {

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:uuid";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:uuid";

    private static final Long STACK_ID = 1L;

    private static final String STACK_NAME = "test-stack";

    private static final String AWS_PLATFORM = "AWS";

    private static final String AZURE_PLATFORM = "AZURE";

    private static final String GCP_PLATFORM = "GCP";

    private static final String REGION = "us-west-1";

    private static final String INSTANCE_ID = "i-instance123";

    @Mock
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private ResourceService resourceService;

    @Mock
    private CloudConnectorHelper cloudConnectorHelper;

    @Mock
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @Mock
    private AmazonEc2Client mockAmazonEc2Client;

    @Mock
    private AwsVolumeIopsCalculator volumeIopsCalculator;

    @Mock
    private StackStatusService stackStatusService;

    @Mock
    private StackService stackService;

    @Mock
    private EntitlementService entitlementService;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private StackPatchUsageReporterService stackPatchUsageReporterService;

    @InjectMocks
    private AwsGp2ToGp3PatchService underTest;

    private Stack stack;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setName(STACK_NAME);
        stack.setResourceCrn(STACK_CRN);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setCloudPlatform(AWS_PLATFORM);
        stack.setPlatformVariant(AWS_PLATFORM);
        stack.setRegion(REGION);
        stack.setInstanceGroups(createInstanceGroupsWithMetadata());

    }

    private Set<InstanceGroup> createInstanceGroupsWithMetadata() {
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setId(1L);
        instanceGroup.setGroupName("master");
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);

        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setId(1L);
        instanceMetaData.setInstanceId(INSTANCE_ID);
        instanceMetaData.setInstanceStatus(com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus.CREATED);
        instanceMetaData.setInstanceGroup(instanceGroup);

        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));

        Set<InstanceGroup> instanceGroups = new HashSet<>();
        instanceGroups.add(instanceGroup);
        return instanceGroups;
    }

    @Test
    void testGetStackPatchType() {
        assertEquals(StackPatchType.AWS_GP2_TO_GP3_MIGRATION, underTest.getStackPatchType());
    }

    @Test
    void testIsAffectedAzureStack() {
        stack.setCloudPlatform(AZURE_PLATFORM);
        boolean result = underTest.isAffected(stack);
        assertFalse(result);
    }

    @Test
    void testIsAffectedGcpStack() {
        stack.setCloudPlatform(GCP_PLATFORM);
        boolean result = underTest.isAffected(stack);
        assertFalse(result);
    }

    @Test
    void testIsAffectedAwsStackWithNoInstances() {
        // Stack with no instances should return false
        boolean result = underTest.isAffected(stack);
        assertFalse(result);
    }

    @Test
    void testShouldCheckForFailedRetryableFlow() {
        // Volume modifications don't start flows, so this should return false
        // to allow the patch to run even if there are failed flows
        assertFalse(underTest.shouldCheckForFailedRetryableFlow());
    }

    @Test
    void testIsAffectedAwsStackWithGp2Volumes() {
        Resource volumeSetResource = new Resource();
        volumeSetResource.setResourceType(ResourceType.AWS_VOLUMESET);

        Resource rootDiskResource = new Resource();
        rootDiskResource.setResourceType(ResourceType.AWS_ROOT_DISK);

        List<Resource> resources = new ArrayList<>();
        resources.add(volumeSetResource);
        resources.add(rootDiskResource);

        when(resourceService.findAllByStackIdAndResourceTypeIn(anyLong(), any())).thenReturn(resources);
        when(stackUtil.getGp2VolumesFromResources(any())).thenReturn(createVolumes(2));
        when(entitlementService.isGp2toGp3MigrationEnabled(any())).thenReturn(true);

        boolean result = underTest.isAffected(stack);
        assertTrue(result, "Stack with GP2 volumes should be affected");
    }

    @Test
    void testIsAffectedAwsStackWithNoEntitlement() {
        when(entitlementService.isGp2toGp3MigrationEnabled(any())).thenReturn(false);

        boolean result = underTest.isAffected(stack);
        assertFalse(result, "Stack with GP2 volumes but no entitlement should not be affected");
    }

    @Test
    void testIsAffectedAwsStackWithGp3Volumes() {
        Resource rootDiskResource = new Resource();
        rootDiskResource.setResourceType(ResourceType.AWS_ROOT_DISK);

        List<Resource> resources = new ArrayList<>();
        resources.add(rootDiskResource);

        // Create VolumeSetAttributes with GP2 volumes
        VolumeSetAttributes.Volume rootDiskVolumes = new VolumeSetAttributes.Volume(
                "vol-456", "/dev/sdg", 200, "gp3", null);

        VolumeSetAttributes rootDiskAttributeSet = new VolumeSetAttributes(
                "us-west-1b", true, null, List.of(rootDiskVolumes), 200, "gp3");

        when(resourceService.findAllByStackIdAndResourceTypeIn(anyLong(), any())).thenReturn(resources);
        when(entitlementService.isGp2toGp3MigrationEnabled(any())).thenReturn(true);

        boolean result = underTest.isAffected(stack);
        assertFalse(result, "Stack with GP2 volumes should be affected");
    }

    @Test
    void testApplyThreePassesWhenConversionTakesLongerThanJobInterval() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        // ------
        // Setup for Pass 1
        // ------

        // Setup: GP2 volume in resources
        List<VolumeSetAttributes.Volume> volumes = createVolumes(1);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                "us-west-1a", true, "", volumes, 100, "gp2");
        Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
        resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));

        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any()))
                .thenReturn(List.of(resource));
        when(resourceAttributeUtil.getTypedAttributes(any(Resource.class), eq(VolumeSetAttributes.class)))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(resourceService.getAllByStackId(STACK_ID)).thenReturn(Set.of(resource));
        when(stackUtil.getGp2VolumesFromResources(any())).thenReturn(volumes);

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        // AWS describeVolumes: return GP2 volume in-use (for both getGp2Volumes and getVolumeInfo)
        Volume awsVolume = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        DescribeVolumesResponse describeVolumesResponse = DescribeVolumesResponse.builder()
                .volumes(awsVolume)
                .build();
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(describeVolumesResponse);

        // Stub the IOPS Calculator.
        when(volumeIopsCalculator.getEquivalentGp3IopsforGp2Volume(anyInt())).thenReturn(3000);

        // Stub the service which calls AWS to perform the migration.
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), any(), any(), any())).thenReturn(null);

        // Migration not started yet
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);

        // Store the stack status objects so we can verify them.
        when(stackService.save(any(Stack.class))).thenAnswer(inv -> {
            Stack saved = inv.getArgument(0);
            stacksStatuses.add(saved.getStackStatus());
            return saved;
        });

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(2, stacksStatuses.size());
        StackStatus savedAfterPass1 = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_REQUESTED, savedAfterPass1.getStatus());
        assertEquals("GP2 migration started", savedAfterPass1.getStatusReason(), "Status reason should contain started message.");

        // Check second status.
        StackStatus saved2AfterPass1 = stacksStatuses.get(1);
        assertEquals(Status.UPDATE_IN_PROGRESS, saved2AfterPass1.getStatus());
        assertEquals(("started:vol-0|failed:|completed:"), saved2AfterPass1.getStatusReason(), "Status reason should contain started volume ID");

        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications: return MODIFYING (conversion still in progress)
        VolumeModification volumeModification = VolumeModification.builder()
                .volumeId("vol-0")
                .modificationState(VolumeModificationState.MODIFYING)
                .build();
        when(mockAmazonEc2Client.describeVolumeModifications(any(DescribeVolumesModificationsRequest.class)))
                .thenReturn(DescribeVolumesModificationsResponse.builder()
                        .volumesModifications(volumeModification)
                        .build());

        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertFalse(pass2Result, "Pass 2 should return false when volumes are still MODIFYING (conversion takes longer than job interval)");

        // Check the 3rd status.
        assertEquals(3, stacksStatuses.size());
        StackStatus savedAfterPass2 = stacksStatuses.get(2);
        assertEquals(Status.UPDATE_IN_PROGRESS, savedAfterPass2.getStatus());
        assertEquals(("started:vol-0|failed:|completed:"), savedAfterPass2.getStatusReason(), "Status reason should contain started volume ID");

        // ------
        // Setup for Pass 3
        // ------

        // AWS describeVolumes: return GP# volume in-use (for both getGp2Volumes and getVolumeInfo)
        awsVolume = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP3)
                .state(VolumeState.IN_USE)
                .build();
        describeVolumesResponse = DescribeVolumesResponse.builder()
                .volumes(awsVolume)
                .build();
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(describeVolumesResponse);

        // AWS describeVolumeModifications: return COMPLETED (conversion done)
        volumeModification = VolumeModification.builder()
                .volumeId("vol-0")
                .modificationState(VolumeModificationState.COMPLETED)
                .build();
        when(mockAmazonEc2Client.describeVolumeModifications(any(DescribeVolumesModificationsRequest.class)))
                .thenReturn(DescribeVolumesModificationsResponse.builder()
                        .volumesModifications(volumeModification)
                        .build());

        // Store the resource save call.
        Resource[] updatedResource = new Resource[1];
        when(resourceService.save(any(Resource.class))).thenAnswer(inv -> {
            Resource saved = inv.getArgument(0);
            updatedResource[0] = saved;
            return saved;
        });

        // Pass 3: The conversion is finished so the patch process should finish.
        boolean pass3Result = underTestLocal.doApply(stack);

        assertTrue(pass3Result, "Pass 3 should return true");

        // Check forth status.
        assertEquals(4, stacksStatuses.size());
        StackStatus savedAfterPass3 = stacksStatuses.get(3);
        assertEquals(Status.AVAILABLE, savedAfterPass3.getStatus());
        assertEquals("started:|failed:|completed:vol-0", savedAfterPass3.getStatusReason(), "Status reason for complete did not match.");

        // Check to make sure resource as updated.
        assertNotNull(updatedResource[0]);
        Optional<VolumeSetAttributes> volumeSetOptional = (new ResourceAttributeUtil()).getTypedAttributes(updatedResource[0], VolumeSetAttributes.class);
        assertTrue(volumeSetOptional.isPresent());
        VolumeSetAttributes volumeSet = volumeSetOptional.get();
        List<VolumeSetAttributes.Volume> existingVolumes = volumeSet.getVolumes();
        assertEquals(1, existingVolumes.size());
        assertEquals("vol-0", existingVolumes.getFirst().getId());
        assertEquals(VolumeType.GP3.toString(), existingVolumes.getFirst().getType());
    }

    @Test
    void testApplyWithBatchingPassesFor100Volumes() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        // ------
        // Setup for Pass 1
        // ------

        // Setup: GP2 volume in resources
        List<VolumeSetAttributes.Volume> volumes = createVolumes(100);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                "us-west-1a", true, "", volumes, 100, "gp2");
        Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
        resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));

        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any()))
                .thenReturn(List.of(resource));
        when(resourceAttributeUtil.getTypedAttributes(any(Resource.class), eq(VolumeSetAttributes.class)))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(resourceService.getAllByStackId(STACK_ID)).thenReturn(Set.of(resource));
        when(stackUtil.getGp2VolumesFromResources(any())).thenAnswer(invocation -> {
                return volumes.stream()
                    .filter(volume -> AwsDiskType.Gp2.toString().equalsIgnoreCase(volume.getType()))
                    .collect(Collectors.toList());
            }
        );

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        // AWS describeVolumes: return GP2 volume in-use (for both getGp2Volumes and getVolumeInfo)
        List<Volume> awsVolumes = createAwsVolumeResponse(100, VolumeType.GP2);
        DescribeVolumesResponse describeVolumesResponse = DescribeVolumesResponse.builder()
                .volumes(awsVolumes)
                .build();
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(describeVolumesResponse);

        // Stub the IOPS Calculator.
        when(volumeIopsCalculator.getEquivalentGp3IopsforGp2Volume(anyInt())).thenReturn(3000);

        // Stub the service which calls AWS to perform the migration.
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), any(), any(), any())).thenReturn(null);

        // Migration not started yet
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);

        // Store the stack status objects so we can verify them.
        when(stackService.save(any(Stack.class))).thenAnswer(inv -> {
            Stack saved = inv.getArgument(0);
            stacksStatuses.add(saved.getStackStatus());
            return saved;
        });

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(2, stacksStatuses.size());
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_REQUESTED, firstStatus.getStatus());
        assertEquals("GP2 migration started", firstStatus.getStatusReason(), "Status reason should contain started message.");

        // Check second status.
        StackStatus secondStatus = stacksStatuses.get(1);
        assertEquals(Status.UPDATE_IN_PROGRESS, secondStatus.getStatus());
        List<String>[] lists = parseMigrationStateMessage(secondStatus.getStatusReason());
        assertEquals(50, lists[0].size(), "There should be 50 volumes created.");
        assertEquals(0, lists[1].size(), "There should be 50 volumes created.");
        assertEquals(0, lists[2].size(), "There should be 50 volumes created.");

        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications: return COMPLETED so the code will move to the next batch.
        VolumeModification volumeModification = VolumeModification.builder()
                .volumeId("vol-")
                .modificationState(VolumeModificationState.COMPLETED)
                .build();
        when(mockAmazonEc2Client.describeVolumeModifications(any(DescribeVolumesModificationsRequest.class)))
                .thenReturn(DescribeVolumesModificationsResponse.builder()
                        .volumesModifications(volumeModification)
                        .build());

        // Log all the calls to save a resource.
        List<Json> resourceUpdates = new ArrayList<>();
        when(resourceService.save(any(Resource.class))).thenAnswer(inv -> {
            Resource saved = inv.getArgument(0);
            resourceUpdates.add(saved.getAttributes());
            return saved;
        });

        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertFalse(pass2Result, "Pass 2 should return false when there are more volumes to process.");
        assertEquals(5, stacksStatuses.size());
        assertEquals(50, resourceUpdates.size(), "There should have been 50 resource save calls");
        assertEquals(50, getGp3VolumeCount(volumes), "There should be 50 gp3 volumes listed.");

        // Check the 3rd status, which is the completion of the first batch.
        StackStatus thirdStatus = stacksStatuses.get(2);
        assertEquals(Status.AVAILABLE, thirdStatus.getStatus());
        lists = parseMigrationStateMessage(thirdStatus.getStatusReason());
        assertEquals(0, lists[0].size(), "Started count should be 0");
        assertEquals(0, lists[1].size(), "Failed count should be 0");
        assertEquals(50, lists[2].size(), "Completed count should be 50");

        // Check 4th status.
        StackStatus forthStatus = stacksStatuses.get(3);
        assertEquals(Status.UPDATE_REQUESTED, forthStatus.getStatus());
        assertEquals("GP2 migration started", forthStatus.getStatusReason(), "Status reason should contain started message.");

        // Check the 5th status, which is the start of the second batch.
        StackStatus fifthStatus = stacksStatuses.get(4);
        assertEquals(Status.UPDATE_IN_PROGRESS, fifthStatus.getStatus());
        lists = parseMigrationStateMessage(fifthStatus.getStatusReason());
        assertEquals(50, lists[0].size(), "Started count should be 50");
        assertEquals(0, lists[1].size(), "Failed count should be 0");
        assertEquals(0, lists[2].size(), "Completed count should be 0");

        // ------
        // Setup for Pass 3
        // ------

        // Pass 3: The conversion is finished so the patch process should finish.
        boolean pass3Result = underTestLocal.doApply(stack);

        assertTrue(pass3Result, "Pass 3 should return true");
        assertEquals(6, stacksStatuses.size());
        assertEquals(100, resourceUpdates.size(), "There should have been 100 resource save calls");
        assertEquals(100, getGp3VolumeCount(volumes), "There should be 100 gp3 volumes listed.");

        // Check 6th status.
        StackStatus sixthStatus = stacksStatuses.get(5);
        assertEquals(Status.AVAILABLE, sixthStatus.getStatus());
        lists = parseMigrationStateMessage(sixthStatus.getStatusReason());
        assertEquals(0, lists[0].size(), "Started count should be 0");
        assertEquals(0, lists[1].size(), "Failed count should be 0");
        assertEquals(50, lists[2].size(), "Completed count should be 50");
    }

    @Test
    void testApplyAllVolumesThrowAnError() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        // ------
        // Setup for Pass 1
        // ------

        // Setup: GP2 volume in resources
        List<VolumeSetAttributes.Volume> volumes = createVolumes(1);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                "us-west-1a", true, "", volumes, 100, "gp2");
        Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
        resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));

        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any()))
                .thenReturn(List.of(resource));
        when(stackUtil.getGp2VolumesFromResources(any())).thenReturn(volumes);

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        // AWS describeVolumes: return GP2 volume in-use (for both getGp2Volumes and getVolumeInfo)
        Volume awsVolume = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        DescribeVolumesResponse describeVolumesResponse = DescribeVolumesResponse.builder()
                .volumes(awsVolume)
                .build();
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(describeVolumesResponse);

        // Stub the IOPS Calculator.
        when(volumeIopsCalculator.getEquivalentGp3IopsforGp2Volume(anyInt())).thenReturn(3000);

        // Stub the service which calls AWS to perform the migration.
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), any(), any(), any())).thenThrow(new CloudbreakException("Could not modify volume"));

        // Migration not started yet
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);

        // Store the stack status objects so we can verify them.
        when(stackService.save(any(Stack.class))).thenAnswer(inv -> {
            Stack saved = inv.getArgument(0);
            stacksStatuses.add(saved.getStackStatus());
            return saved;
        });

        // Execute pass 1
        assertThrows(ExistingStackPatchApplyException.class, () -> underTestLocal.doApply(stack));
    }

    @Test
    void testApplyWithCBMismatch() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        // ------
        // Setup for Pass 1
        // ------

        // Setup: GP2 volume in resources
        List<VolumeSetAttributes.Volume> volumes = createVolumes(1);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                "us-west-1a", true, "", volumes, 100, "gp2");
        Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
        resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));

        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any()))
                .thenReturn(List.of(resource));
        when(stackUtil.getGp2VolumesFromResources(any())).thenReturn(volumes);

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        // AWS describeVolumes: return GP3 volume in-use so that AWS does not match CB.
        Volume awsVolume = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP3)
                .state(VolumeState.IN_USE)
                .build();
        DescribeVolumesResponse describeVolumesResponse = DescribeVolumesResponse.builder()
                .volumes(awsVolume)
                .build();
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(describeVolumesResponse);

        // Migration not started yet
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);

        // Store the stack status objects so we can verify them.
        when(stackService.save(any(Stack.class))).thenAnswer(inv -> {
            Stack saved = inv.getArgument(0);
            stacksStatuses.add(saved.getStackStatus());
            return saved;
        });

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        assertTrue(pass1Result, "Pass 1 should return true");
        assertEquals(2, stacksStatuses.size(), "There should be 2 stack status updates");

        // Check 1st status.
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_REQUESTED, firstStatus.getStatus());
        assertEquals(DetailedStackStatus.VOLUME_MIGRATION_STARTED, firstStatus.getDetailedStackStatus());
        assertEquals("GP2 migration started", firstStatus.getStatusReason(), "Status reason should be GP2 migration started");

        // Check 2nd status.
        StackStatus secondStatus = stacksStatuses.get(1);
        assertEquals(Status.UPDATE_FAILED, secondStatus.getStatus());
        assertEquals(DetailedStackStatus.VOLUME_MIGRATION_FAILED, secondStatus.getDetailedStackStatus());
        assertEquals("No valid GP2 volumes found", secondStatus.getStatusReason(), "Status reason should be GP2 migration started");
    }

    @Test
    void testApplyWithPartialInitialFailure() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        // ------
        // Setup for Pass 1
        // ------

        // Setup: GP2 volume in resources
        List<VolumeSetAttributes.Volume> volumes = createVolumes(3);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                "us-west-1a", true, "", volumes, 100, "gp2");
        Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
        resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));

        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any()))
                .thenReturn(List.of(resource));
        when(resourceAttributeUtil.getTypedAttributes(any(Resource.class), eq(VolumeSetAttributes.class)))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(resourceService.getAllByStackId(STACK_ID)).thenReturn(Set.of(resource));
        when(stackUtil.getGp2VolumesFromResources(any())).thenReturn(volumes);

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        // AWS describeVolumes: return GP2 volume in-use (for both getGp2Volumes and getVolumeInfo)
        Volume awsVolumeVol0 = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        Volume awsVolumeVol1 = Volume.builder()
                .volumeId("vol-1")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        Volume awsVolumeVol2 = Volume.builder()
                .volumeId("vol-2")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        doReturn(DescribeVolumesResponse.builder()
                .volumes(List.of(awsVolumeVol0, awsVolumeVol1, awsVolumeVol2))
                .build()).when(mockAmazonEc2Client)
                .describeVolumes(any(DescribeVolumesRequest.class));

        // Stub the IOPS Calculator.
        when(volumeIopsCalculator.getEquivalentGp3IopsforGp2Volume(anyInt())).thenReturn(3000);

        // Stub the service which calls AWS to perform the migration. Two drives should succeed and one should fail.
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), eq("vol-0"), any(), any())).thenReturn(null);
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), eq("vol-1"), any(), any())).thenReturn(null);
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), eq("vol-2"), any(), any())).thenThrow(new CloudbreakException("Could not modify volume"));

        // Migration not started yet
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);

        // Store the stack status objects so we can verify them.
        when(stackService.save(any(Stack.class))).thenAnswer(inv -> {
            Stack saved = inv.getArgument(0);
            stacksStatuses.add(saved.getStackStatus());
            return saved;
        });

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(2, stacksStatuses.size());
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_REQUESTED, firstStatus.getStatus());
        assertEquals("GP2 migration started", firstStatus.getStatusReason(), "Status reason should contain started message.");

        // Check second status.
        StackStatus secondStatus = stacksStatuses.get(1);
        assertEquals(Status.UPDATE_IN_PROGRESS, secondStatus.getStatus());
        assertEquals(("started:vol-0,vol-1|failed:vol-2|completed:"), secondStatus.getStatusReason(),
                "Status reason should contain started and failed volume ID");

        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications: return MODIFYING (conversion still in progress)
        VolumeModification volumeModification = VolumeModification.builder()
                .volumeId("vol-0")
                .modificationState(VolumeModificationState.MODIFYING)
                .build();
        doReturn(DescribeVolumesModificationsResponse.builder()
                .volumesModifications(volumeModification)
                .build()).when(mockAmazonEc2Client)
                .describeVolumeModifications(argThat(req -> req.volumeIds().getFirst().equals("vol-0")));
        VolumeModification volumeModification2 = VolumeModification.builder()
                .volumeId("vol-1")
                .modificationState(VolumeModificationState.FAILED)
                .build();
        doReturn(DescribeVolumesModificationsResponse.builder()
                .volumesModifications(volumeModification2)
                .build()).when(mockAmazonEc2Client)
                .describeVolumeModifications(argThat(req -> req.volumeIds().getFirst().equals("vol-1")));

        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertFalse(pass2Result, "Pass 2 should return false when volumes are still MODIFYING (conversion takes longer than job interval)");

        // Check the 3rd status.
        assertEquals(3, stacksStatuses.size());
        StackStatus thirdStatus = stacksStatuses.get(2);
        assertEquals(Status.UPDATE_IN_PROGRESS, thirdStatus.getStatus());
        assertEquals(("started:vol-0|failed:vol-2,vol-1|completed:"), thirdStatus.getStatusReason(),
                "Status reason should contain started and failed volume ID");

        // ------
        // Setup for Pass 3
        // ------

        Volume awsVolume = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP3)
                .state(VolumeState.IN_USE)
                .build();
        doReturn(DescribeVolumesResponse.builder()
                .volumes(List.of(awsVolume))
                .build()).when(mockAmazonEc2Client)
                .describeVolumes(any(DescribeVolumesRequest.class));

        // AWS describeVolumeModifications: return COMPLETED (conversion done)
        volumeModification = VolumeModification.builder()
                .volumeId("vol-0")
                .modificationState(VolumeModificationState.COMPLETED)
                .build();
        doReturn(DescribeVolumesModificationsResponse.builder()
                .volumesModifications(volumeModification)
                .build()).when(mockAmazonEc2Client).describeVolumeModifications(any(DescribeVolumesModificationsRequest.class));


        // Store the resource save call.
        Resource[] updatedResource = new Resource[1];
        when(resourceService.save(any(Resource.class))).thenAnswer(inv -> {
            Resource saved = inv.getArgument(0);
            updatedResource[0] = saved;
            return saved;
        });

        // Pass 3: The conversion is finished so the patch process should finish.
        boolean pass3Result = underTestLocal.doApply(stack);

        assertTrue(pass3Result, "Pass 3 should return true");

        // Check forth status.
        assertEquals(4, stacksStatuses.size());
        StackStatus forthStatus = stacksStatuses.get(3);
        assertEquals(Status.AVAILABLE, forthStatus.getStatus());
        assertEquals("started:|failed:vol-2,vol-1|completed:vol-0", forthStatus.getStatusReason(), "Status reason for complete did not match.");

        // Check to make sure resource as updated.
        assertNotNull(updatedResource[0]);
    }

    @Test
    void testApplyWithMissingVolumeModification() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        List<StackStatus> stacksStatuses = setupTwoVolumeInitialPass(underTestLocal);

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(2, stacksStatuses.size());
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_REQUESTED, firstStatus.getStatus());
        assertEquals("GP2 migration started", firstStatus.getStatusReason(), "Status reason should contain started message.");

        // Check second status.
        StackStatus secondStatus = stacksStatuses.get(1);
        assertEquals(Status.UPDATE_IN_PROGRESS, secondStatus.getStatus());
        assertEquals(("started:vol-0,vol-1|failed:|completed:"), secondStatus.getStatusReason(),
                "Status reason should contain started and failed volume ID");

        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications: return MODIFYING (conversion still in progress)
        VolumeModification volumeModification = VolumeModification.builder()
                .volumeId("vol-0")
                .modificationState(VolumeModificationState.COMPLETED)
                .build();
        doReturn(DescribeVolumesModificationsResponse.builder()
                .volumesModifications(volumeModification)
                .build()).when(mockAmazonEc2Client)
                .describeVolumeModifications(argThat(req -> req.volumeIds().getFirst().equals("vol-0")));
        // The request for vol-1 should return an empty list
        doReturn(DescribeVolumesModificationsResponse.builder()
                .build()).when(mockAmazonEc2Client)
                .describeVolumeModifications(argThat(req -> req.volumeIds().getFirst().equals("vol-1")));

        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertTrue(pass2Result, "Pass 2 should return true");

        // Check the 3rd status.
        assertEquals(3, stacksStatuses.size());
        StackStatus thirdStatus = stacksStatuses.get(2);
        assertEquals(Status.AVAILABLE, thirdStatus.getStatus());
        assertEquals(("started:|failed:vol-1|completed:vol-0"), thirdStatus.getStatusReason(),
                "Status reason should contain started and failed volume ID");
    }

    @Test
    void testApplySuccessUnknownVolumeModification() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        List<StackStatus> stacksStatuses = setupTwoVolumeInitialPass(underTestLocal);

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(2, stacksStatuses.size());
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_REQUESTED, firstStatus.getStatus());
        assertEquals("GP2 migration started", firstStatus.getStatusReason(), "Status reason should contain started message.");

        // Check second status.
        StackStatus secondStatus = stacksStatuses.get(1);
        assertEquals(Status.UPDATE_IN_PROGRESS, secondStatus.getStatus());
        assertEquals(("started:vol-0,vol-1|failed:|completed:"), secondStatus.getStatusReason(),
                "Status reason should contain started and failed volume ID");

        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications.
        VolumeModification volumeModification = VolumeModification.builder()
                .volumeId("vol-0")
                .modificationState(VolumeModificationState.COMPLETED)
                .build();
        doReturn(DescribeVolumesModificationsResponse.builder()
                .volumesModifications(volumeModification)
                .build()).when(mockAmazonEc2Client)
                .describeVolumeModifications(argThat(req -> req.volumeIds().getFirst().equals("vol-0")));
        VolumeModification volumeModification2 = VolumeModification.builder()
                .volumeId("vol-1")
                .modificationState(VolumeModificationState.UNKNOWN_TO_SDK_VERSION)
                .build();
        doReturn(DescribeVolumesModificationsResponse.builder()
                .volumesModifications(volumeModification2)
                .build()).when(mockAmazonEc2Client)
                .describeVolumeModifications(argThat(req -> req.volumeIds().getFirst().equals("vol-1")));

        // AWS describeVolumes: return GP3 volume in-use
        Volume awsVolumeVol0 = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP3)
                .state(VolumeState.IN_USE)
                .build();
        doReturn(DescribeVolumesResponse.builder()
                .volumes(List.of(awsVolumeVol0))
                .build()).when(mockAmazonEc2Client)
                .describeVolumes(argThat(req -> req.volumeIds().getFirst().equals("vol-0")));
        Volume awsVolumeVol1 = Volume.builder()
                .volumeId("vol-1")
                .size(100)
                .volumeType(VolumeType.GP3)
                .state(VolumeState.IN_USE)
                .build();
        doReturn(DescribeVolumesResponse.builder()
                .volumes(List.of(awsVolumeVol1))
                .build()).when(mockAmazonEc2Client)
                .describeVolumes(argThat(req -> req.volumeIds().getFirst().equals("vol-1")));

        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertTrue(pass2Result, "Pass 2 should return true");

        // Check the 3rd status.
        assertEquals(3, stacksStatuses.size());
        StackStatus thirdStatus = stacksStatuses.get(2);
        assertEquals(Status.AVAILABLE, thirdStatus.getStatus());
        assertEquals(("started:|failed:|completed:vol-0,vol-1"), thirdStatus.getStatusReason(),
                "Status reason should contain started and failed volume ID");
    }

    @Test
    void testApplyFailureUnknownVolumeModification() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        List<StackStatus> stacksStatuses = setupTwoVolumeInitialPass(underTestLocal);

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(2, stacksStatuses.size());
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_REQUESTED, firstStatus.getStatus());
        assertEquals("GP2 migration started", firstStatus.getStatusReason(), "Status reason should contain started message.");

        // Check second status.
        StackStatus secondStatus = stacksStatuses.get(1);
        assertEquals(Status.UPDATE_IN_PROGRESS, secondStatus.getStatus());
        assertEquals(("started:vol-0,vol-1|failed:|completed:"), secondStatus.getStatusReason(),
                "Status reason should contain started and failed volume ID");

        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications.
        VolumeModification volumeModification = VolumeModification.builder()
                .volumeId("vol-0")
                .modificationState(VolumeModificationState.COMPLETED)
                .build();
        doReturn(DescribeVolumesModificationsResponse.builder()
                .volumesModifications(volumeModification)
                .build()).when(mockAmazonEc2Client)
                .describeVolumeModifications(argThat(req -> req.volumeIds().getFirst().equals("vol-0")));
        VolumeModification volumeModification2 = VolumeModification.builder()
                .volumeId("vol-1")
                .modificationState(VolumeModificationState.UNKNOWN_TO_SDK_VERSION)
                .build();
        doReturn(DescribeVolumesModificationsResponse.builder()
                .volumesModifications(volumeModification2)
                .build()).when(mockAmazonEc2Client)
                .describeVolumeModifications(argThat(req -> req.volumeIds().getFirst().equals("vol-1")));

        // AWS describeVolumes: return GP3 volume in-use
        Volume awsVolumeVol0 = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP3)
                .state(VolumeState.IN_USE)
                .build();
        doReturn(DescribeVolumesResponse.builder()
                .volumes(List.of(awsVolumeVol0))
                .build()).when(mockAmazonEc2Client)
                .describeVolumes(argThat(req -> req.volumeIds().getFirst().equals("vol-0")));
        Volume awsVolumeVol1 = Volume.builder()
                .volumeId("vol-1")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        doReturn(DescribeVolumesResponse.builder()
                .volumes(List.of(awsVolumeVol1))
                .build()).when(mockAmazonEc2Client)
                .describeVolumes(argThat(req -> req.volumeIds().getFirst().equals("vol-1")));


        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertFalse(pass2Result, "Pass 2 should return true");

        // Check the 3rd status.
        assertEquals(3, stacksStatuses.size());
        StackStatus thirdStatus = stacksStatuses.get(2);
        assertEquals(Status.UPDATE_IN_PROGRESS, thirdStatus.getStatus());
        assertEquals(("started:vol-1|failed:|completed:vol-0"), thirdStatus.getStatusReason(),
                "Status reason should contain started and failed volume ID");
    }

    private List<StackStatus> setupTwoVolumeInitialPass(AwsGp2ToGp3PatchService underTestLocal) throws CloudbreakException {
        // Setup: GP2 volume in resources
        List<VolumeSetAttributes.Volume> volumes = createVolumes(2);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                "us-west-1a", true, "", volumes, 100, "gp2");
        Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
        resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));

        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any()))
                .thenReturn(List.of(resource));
        when(resourceAttributeUtil.getTypedAttributes(any(Resource.class), eq(VolumeSetAttributes.class)))
                .thenReturn(Optional.of(volumeSetAttributes));
        when(resourceService.getAllByStackId(STACK_ID)).thenReturn(Set.of(resource));
        when(stackUtil.getGp2VolumesFromResources(any())).thenReturn(volumes);

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        // AWS describeVolumes: return GP2 volume in-use
        Volume awsVolumeVol0 = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        Volume awsVolumeVol1 = Volume.builder()
                .volumeId("vol-1")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        doReturn(DescribeVolumesResponse.builder()
                .volumes(List.of(awsVolumeVol0, awsVolumeVol1))
                .build()).when(mockAmazonEc2Client)
                .describeVolumes(any(DescribeVolumesRequest.class));

        // Stub the IOPS Calculator.
        when(volumeIopsCalculator.getEquivalentGp3IopsforGp2Volume(anyInt())).thenReturn(3000);

        // Stub the service which calls AWS to perform the migration.
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), eq("vol-0"), any(), any())).thenReturn(null);
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), eq("vol-1"), any(), any())).thenReturn(null);

        // Migration not started yet
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);

        // Store the stack status objects so we can verify them.
        when(stackService.save(any(Stack.class))).thenAnswer(inv -> {
            Stack saved = inv.getArgument(0);
            stacksStatuses.add(saved.getStackStatus());
            return saved;
        });
        return stacksStatuses;
    }

    private int getGp3VolumeCount(List<VolumeSetAttributes.Volume> volumes) {
        int  count = 0;
        for (VolumeSetAttributes.Volume volume : volumes) {
            if (volume.getType().equals(VolumeType.GP3.toString())) {
                count++;
            }
        }
        return count;
    }

    private List<VolumeSetAttributes.Volume> createVolumes(int numVolumes) {
        List<VolumeSetAttributes.Volume> retval =  new ArrayList<>();
        for (int i = 0; i < numVolumes; i++) {
            retval.add(new VolumeSetAttributes.Volume(
                    "vol-" + i, "/dev/xvd" + i, 100, "gp2",
                    com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType.GENERAL));
        }
        return retval;
    }

    private List<Volume> createAwsVolumeResponse(int numVolumes, VolumeType volumeType) {
        List<Volume> retval =  new ArrayList<>();
        for (int i = 0; i < numVolumes; i++) {
            retval.add(Volume.builder()
                    .volumeId("vol-" + i)
                    .size(100)
                    .volumeType(volumeType)
                    .state(VolumeState.IN_USE)
                    .build());
        }
        return retval;
    }

    private List<String>[] parseMigrationStateMessage(String statusReason) {
        List<String> started = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        List<String> completed = new ArrayList<>();

        if (statusReason.contains("|")) {
            String[] parts = statusReason.split("\\|", 3);
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.startsWith("started:")) {
                    started = parseVolumeIds(trimmed, "started:");
                } else if (trimmed.startsWith("failed:")) {
                    failed = parseVolumeIds(trimmed, "failed:");
                } else if (trimmed.startsWith("completed:")) {
                    completed = parseVolumeIds(trimmed, "completed:");
                }
            }
        }

        return new List[]{started, failed, completed};
    }

    private List<String> parseVolumeIds(String input, String prefix) {
        String ids = input.substring(prefix.length()).trim();
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.stream(ids.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

}
