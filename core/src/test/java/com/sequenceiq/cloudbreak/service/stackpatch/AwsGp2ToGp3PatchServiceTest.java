package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsCommonDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeModificationState;
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
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;

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

    @Spy
    private final ResourceAttributeUtil resourceAttributeUtil = new ResourceAttributeUtil();

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
        // Create VolumeSetAttributes with GP2 volumes
        VolumeSetAttributes.Volume rootDiskVolume = new VolumeSetAttributes.Volume(
                "vol-456", "/dev/sdg", 200, "gp2", null);
        VolumeSetAttributes rootDiskAttributeSet = new VolumeSetAttributes(
                "us-west-1b", true, null, List.of(rootDiskVolume), 200, "gp2");
        Resource resource = new Resource(ResourceType.AWS_ROOT_DISK, "resource-1", stack, "us-west-1a");
        resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(rootDiskAttributeSet));

        // Create VolumeSetAttributes with GP2 volumes
        VolumeSetAttributes.Volume volumeSetDiskVolume = new VolumeSetAttributes.Volume(
                "vol-789", "/dev/sdh", 200, "gp2", null);
        VolumeSetAttributes volumeSetDiskAttributeSet = new VolumeSetAttributes(
                "us-west-1b", true, null, List.of(volumeSetDiskVolume), 200, "gp2");
        Resource volumeSetResource = new Resource(ResourceType.AWS_VOLUMESET, "resource-2", stack, "us-west-1a");
        volumeSetResource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetDiskAttributeSet));

        List<Resource> resources = new ArrayList<>();
        resources.add(resource);
        resources.add(volumeSetResource);

        when(resourceService.findAllByStackIdAndResourceTypeIn(anyLong(), any())).thenReturn(resources);

        boolean result = underTest.isAffected(stack);
        assertTrue(result, "Stack with GP2 volumes should be affected");
    }

    @Test
    void testIsAffectedAwsStackWithNoEntitlement() throws Exception {
        when(entitlementService.isGp2toGp3MigrationEnabled(any())).thenReturn(false);

        boolean result = underTest.doApply(stack);
        assertFalse(result, "Stack with GP2 volumes but no entitlement should not be affected");
    }

    @Test
    void testIsAffectedAwsStackWithGp3Volumes() {
        // Create VolumeSetAttributes with GP3 volumes
        VolumeSetAttributes.Volume rootDiskVolume = new VolumeSetAttributes.Volume(
                "vol-456", "/dev/sdg", 200, "gp3", null);
        VolumeSetAttributes rootDiskAttributeSet = new VolumeSetAttributes(
                "us-west-1b", true, null, List.of(rootDiskVolume), 200, "gp3");
        Resource resource = new Resource(ResourceType.AWS_ROOT_DISK, "resource-1", stack, "us-west-1a");
        resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(rootDiskAttributeSet));

        List<Resource> resources = new ArrayList<>();
        resources.add(resource);

        when(resourceService.findAllByStackIdAndResourceTypeIn(anyLong(), any())).thenReturn(resources);

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
        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any())).thenAnswer(inv -> {
            VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                    "us-west-1a", true, "", "", volumes, "");
            Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
            resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));
            return List.of(resource);
        });

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        doReturn(true).when(entitlementService).isGp2toGp3MigrationEnabled(any());

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
        when(volumeIopsCalculator.getEquivalentGp3IopsForGp2Volume(anyInt())).thenReturn(3000);

        // Stub the service which calls AWS to perform the migration.
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), any(), any(), any())).thenReturn(null);

        // Mock the stackStatusService and also record any saved statuses for verification.
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);
        when(stackService.save(any(Stack.class))).thenAnswer(inv -> {
            Stack saved = inv.getArgument(0);
            stacksStatuses.add(saved.getStackStatus());
            return saved;
        });

        // Log all the calls to save a resource.
        List<Json> resourceUpdates = new ArrayList<>();
        when(resourceService.save(any(Resource.class))).thenAnswer(inv -> {
            Resource saved = inv.getArgument(0);
            resourceUpdates.add(saved.getAttributes());
            volumes.clear();
            volumes.addAll((new ResourceAttributeUtil()).getTypedAttributes(saved, VolumeSetAttributes.class).get().getVolumes());
            return saved;
        });

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(1, stacksStatuses.size());
        StackStatus savedAfterPass1 = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_IN_PROGRESS, savedAfterPass1.getStatus());
        assertEquals("GP2 migration started for 1 GP2 volumes", savedAfterPass1.getStatusReason(), "Status reason should contain started message.");

        assertEquals(1, resourceUpdates.size(), "There should have been 1 resource save calls");
        assertEquals(0, getGp3VolumeCount(volumes), "There should be 0 gp3 volumes listed.");
        assertEquals(1, getInProgressVolumeCount(volumes), "There should be 1 in-progress volume listed.");

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

        // Check the first status.
        assertEquals(1, stacksStatuses.size());
        StackStatus savedAfterPass2 = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_IN_PROGRESS, savedAfterPass2.getStatus());

        // There should be no changes to the following counts from the first pass.
        assertEquals(1, resourceUpdates.size(), "There should have been 1 resource save calls");
        assertEquals(0, getGp3VolumeCount(volumes), "There should be 0 gp3 volumes listed.");
        assertEquals(1, getInProgressVolumeCount(volumes), "There should be 1 in-progress volume listed.");

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

        // Pass 3: The conversion is finished so the patch process should finish.
        boolean pass3Result = underTestLocal.doApply(stack);

        assertTrue(pass3Result, "Pass 3 should return true");

        // Check forth status.
        assertEquals(2, stacksStatuses.size());
        StackStatus savedAfterPass3 = stacksStatuses.get(1);
        assertEquals(Status.AVAILABLE, savedAfterPass3.getStatus());
        assertEquals("GP2 migration finished. Failed: 0 Succeeded: 1", savedAfterPass3.getStatusReason(), "Status reason should contain AVAILABLE message.");

        // Check to make sure resource as updated.
        assertEquals(2, resourceUpdates.size(), "There should have been 2 resource save calls");
        assertEquals(1, getGp3VolumeCount(volumes), "There should be 1 gp3 volumes listed.");
        assertEquals(0, getInProgressVolumeCount(volumes), "There should be 0 in-progress volume listed.");
    }

    @Test
    void testApplyWithBatchingPassesFor100Volumes() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        // ------
        // Setup for Pass 1
        // ------

        // Setup: Create an initial resource with 100 volumes.
        List<VolumeSetAttributes.Volume> volumes = createVolumes(100);
        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any())).thenAnswer(inv -> {
            VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                    "us-west-1a", true, "", "", volumes, "");
            Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
            resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));
            return List.of(resource);
        });

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        doReturn(true).when(entitlementService).isGp2toGp3MigrationEnabled(any());

        // AWS describeVolumes: return GP2 volume in-use (for both getGp2Volumes and getVolumeInfo)
        List<Volume> awsVolumes = createAwsVolumeResponse(100, VolumeType.GP2);
        DescribeVolumesResponse describeVolumesResponse = DescribeVolumesResponse.builder()
                .volumes(awsVolumes)
                .build();
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(describeVolumesResponse);

        // Stub the IOPS Calculator.
        when(volumeIopsCalculator.getEquivalentGp3IopsForGp2Volume(anyInt())).thenReturn(3000);

        // Stub the service which calls AWS to perform the migration.
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), any(), any(), any())).thenReturn(null);

        // Mock the stackStatusService and also record any saved statuses for verification.
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);
        when(stackService.save(any(Stack.class))).thenAnswer(inv -> {
            Stack saved = inv.getArgument(0);
            stacksStatuses.add(saved.getStackStatus());
            return saved;
        });

        // Log all the calls to save a resource.
        List<Json> resourceUpdates = new ArrayList<>();
        when(resourceService.save(any(Resource.class))).thenAnswer(inv -> {
            Resource saved = inv.getArgument(0);
            resourceUpdates.add(saved.getAttributes());
            volumes.clear();
            volumes.addAll((new ResourceAttributeUtil()).getTypedAttributes(saved, VolumeSetAttributes.class).get().getVolumes());
            return saved;
        });

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(1, stacksStatuses.size());
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_IN_PROGRESS, firstStatus.getStatus());
        assertEquals("GP2 migration started for 100 GP2 volumes", firstStatus.getStatusReason(), "Status reason should contain started message.");

        assertEquals(1, resourceUpdates.size(), "There should have been 1 resource save calls");
        assertEquals(0, getGp3VolumeCount(volumes), "There should be 50 gp3 volumes listed.");
        assertEquals(50, getInProgressVolumeCount(volumes), "There should be 50 in-progress volumes listed.");


        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications: return COMPLETED so the code will move to the next batch.
        when(mockAmazonEc2Client.describeVolumeModifications(any(DescribeVolumesModificationsRequest.class))).thenAnswer(inv -> {
            DescribeVolumesModificationsRequest saved = inv.getArgument(0);
            List<VolumeModification> newVolumeModList = new ArrayList<>();
            for (String vid : saved.volumeIds()) {
                newVolumeModList.add(VolumeModification.builder()
                        .volumeId(vid)
                        .modificationState(VolumeModificationState.COMPLETED)
                        .build());
            }
            return DescribeVolumesModificationsResponse.builder()
                    .volumesModifications(newVolumeModList)
                    .build();
        });

        // AWS describeVolumes: The first batch of 50 should be GP3, but the second batch of 50 should be GP2.
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class))).thenAnswer(inv -> {
            DescribeVolumesRequest saved = inv.getArgument(0);
            List<Volume> newVolumeList = new ArrayList<>();
            for (String vid : saved.volumeIds()) {
                int idx = Integer.parseInt(vid.split("-")[1]);
                VolumeType type =  VolumeType.GP3;
                if (idx >= 50) {
                    type =  VolumeType.GP2;
                }
                newVolumeList.add(Volume.builder()
                        .volumeId(vid)
                        .size(100)
                        .volumeType(type)
                        .state(VolumeState.IN_USE)
                        .build());
            }
            return DescribeVolumesResponse.builder()
                    .volumes(newVolumeList)
                    .build();
        });

        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertFalse(pass2Result, "Pass 2 should return false when there are more volumes to process.");
        assertEquals(1, stacksStatuses.size(), "There should only be one stack status.");
        // 1 update from above + 1 to mark the batch as finished + 1 to mark the next batch as in-progress
        assertEquals(3, resourceUpdates.size(), "There should have been 3 resource save calls");
        // 50 volumes should have completed migration.
        assertEquals(50, getGp3VolumeCount(volumes), "There should be 50 gp3 volumes listed.");
        // 50 new volumes should have started migration
        assertEquals(50, getInProgressVolumeCount(volumes), "There should be 50 in-progress volumes listed.");

        // ------
        // Setup for Pass 3
        // ------

        // AWS describeVolumes: Everything should be GP3 now.
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class))).thenAnswer(inv -> {
            DescribeVolumesRequest saved = inv.getArgument(0);
            List<Volume> newVolumeList = new ArrayList<>();
            for (String vid : saved.volumeIds()) {
                newVolumeList.add(Volume.builder()
                        .volumeId(vid)
                        .size(100)
                        .volumeType(VolumeType.GP3)
                        .state(VolumeState.IN_USE)
                        .build());
            }
            return DescribeVolumesResponse.builder()
                    .volumes(newVolumeList)
                    .build();
        });

        // Pass 3: The conversion is finished so the patch process should finish.
        boolean pass3Result = underTestLocal.doApply(stack);

        assertTrue(pass3Result, "Pass 3 should return true");
        assertEquals(2, stacksStatuses.size());
        // 3 updates from above + 1 to mark the batch as finished
        assertEquals(4, resourceUpdates.size(), "There should have been 4 resource save calls");
        assertEquals(100, getGp3VolumeCount(volumes), "There should be 100 gp3 volumes listed.");
        assertEquals(0, getInProgressVolumeCount(volumes), "There should be 50 in-progress volumes listed.");

        // Check 2nd status.
        StackStatus secondStatus = stacksStatuses.get(1);
        assertEquals(Status.AVAILABLE, secondStatus.getStatus());
        assertEquals("GP2 migration finished. Failed: 0 Succeeded: 100", secondStatus.getStatusReason(), "Status reason should contain AVAILABLE message.");
    }

    @Test
    void testApplyAllVolumesThrowAnError() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        // ------
        // Setup for Pass 1
        // ------

        // Setup: GP2 volume in resources
        List<VolumeSetAttributes.Volume> volumes = createVolumes(1);
        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any())).thenAnswer(inv -> {
            VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                    "us-west-1a", true, "", "", volumes, "");
            Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
            resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));
            return List.of(resource);
        });

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        doReturn(true).when(entitlementService).isGp2toGp3MigrationEnabled(any());

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
        when(volumeIopsCalculator.getEquivalentGp3IopsForGp2Volume(anyInt())).thenReturn(3000);

        // Stub the service which calls AWS to perform the migration.
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), any(), any(), any())).thenThrow(new CloudbreakException("Could not modify volume"));

        // Mock the stackStatusService and also record any saved statuses for verification.
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);
        when(stackService.save(any(Stack.class))).thenAnswer(inv -> {
            Stack saved = inv.getArgument(0);
            stacksStatuses.add(saved.getStackStatus());
            return saved;
        });

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        assertTrue(pass1Result, "Pass 1 should return true");
        // There should be no valid volumes to process so the stack patcher will just return true with no status updates.
        assertEquals(2, stacksStatuses.size(), "There should be 2 stack status updates");
    }

    @Test
    void testApplyWithCBMismatch() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        // ------
        // Setup for Pass 1
        // ------

        // Setup: GP2 volume in resources
        List<VolumeSetAttributes.Volume> volumes = createVolumes(1);
        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any())).thenAnswer(inv -> {
            VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                    "us-west-1a", true, "", "", volumes, "");
            Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
            resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));
            return List.of(resource);
        });

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        doReturn(true).when(entitlementService).isGp2toGp3MigrationEnabled(any());

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

        // Mock the stackStatusService and also record any saved statuses for verification.
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        assertTrue(pass1Result, "Pass 1 should return true");
        // There should be no valid volumes to process so the stack patcher will just return true with no status updates.
        assertEquals(0, stacksStatuses.size(), "There should be 0 stack status updates");
    }

    @Test
    void testApplyWithPartialInitialFailure() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        // ------
        // Setup for Pass 1
        // ------

        // Setup: GP2 volume in resources
        List<VolumeSetAttributes.Volume> volumes = createVolumes(3);
        when(resourceService.findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any())).thenAnswer(inv -> {
            VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                    "us-west-1a", true, "", "", volumes, "");
            Resource resource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
            resource.setAttributes(new com.sequenceiq.cloudbreak.common.json.Json(volumeSetAttributes));
            return List.of(resource);
        });

        // Setup: configure mock EC2 client
        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        doReturn(true).when(entitlementService).isGp2toGp3MigrationEnabled(any());

        // AWS describeVolumes: return GP2 volume in-use (for both getGp2Volumes and getVolumeInfo)
        List<Volume> awsVolumes = createAwsVolumeResponse(3, VolumeType.GP2);
        DescribeVolumesResponse describeVolumesResponse = DescribeVolumesResponse.builder()
                .volumes(awsVolumes)
                .build();
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(describeVolumesResponse);

        // Stub the IOPS Calculator.
        when(volumeIopsCalculator.getEquivalentGp3IopsForGp2Volume(anyInt())).thenReturn(3000);

        // Stub the service which calls AWS to perform the migration. Two drives should succeed and one should fail.
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), eq("vol-0"), any(), any())).thenReturn(null);
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), eq("vol-1"), any(), any())).thenReturn(null);
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), eq("vol-2"), any(), any())).thenThrow(new CloudbreakException("Could not modify volume"));

        // Mock the stackStatusService and also record any saved statuses for verification.
        List<StackStatus> stacksStatuses = new ArrayList<>();
        when(stackStatusService.findAllStackStatusesById(STACK_ID))
                .thenReturn(stacksStatuses);
        when(stackService.save(any(Stack.class))).thenAnswer(inv -> {
            Stack saved = inv.getArgument(0);
            stacksStatuses.add(saved.getStackStatus());
            return saved;
        });

        // Log all the calls to save a resource.
        List<Json> resourceUpdates = new ArrayList<>();
        when(resourceService.save(any(Resource.class))).thenAnswer(inv -> {
            Resource saved = inv.getArgument(0);
            resourceUpdates.add(saved.getAttributes());
            volumes.clear();
            volumes.addAll((new ResourceAttributeUtil()).getTypedAttributes(saved, VolumeSetAttributes.class).get().getVolumes());
            return saved;
        });

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(1, stacksStatuses.size());
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_IN_PROGRESS, firstStatus.getStatus());
        assertEquals("GP2 migration started for 3 GP2 volumes", firstStatus.getStatusReason(), "Status reason should contain started message.");

        assertEquals(1, resourceUpdates.size(), "There should have been 3 resource save calls");
        assertEquals(0, getGp3VolumeCount(volumes), "There should be 0 gp3 volumes listed.");
        assertEquals(2, getInProgressVolumeCount(volumes), "There should be 2 in-progress volumes listed.");
        assertEquals(1, getInFailedVolumeCount(volumes), "There should be 1 failed volumes listed.");

        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications: return MODIFYING (conversion still in progress)
        when(mockAmazonEc2Client.describeVolumeModifications(any(DescribeVolumesModificationsRequest.class))).thenAnswer(inv -> {
            DescribeVolumesModificationsRequest saved = inv.getArgument(0);
            List<VolumeModification> newVolumeModList = new ArrayList<>();
            for (String vid : saved.volumeIds()) {
                if ("vol-0".equals(vid)) {
                    newVolumeModList.add(VolumeModification.builder()
                            .volumeId("vol-0")
                            .modificationState(VolumeModificationState.MODIFYING)
                            .build());
                } else {
                    newVolumeModList.add(VolumeModification.builder()
                            .volumeId("vol-1")
                            .modificationState(VolumeModificationState.FAILED)
                            .build());
                }
            }
            return DescribeVolumesModificationsResponse.builder()
                    .volumesModifications(newVolumeModList)
                    .build();
        });

        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertFalse(pass2Result, "Pass 2 should return false when volumes are still MODIFYING (conversion takes longer than job interval)");
        assertEquals(1, stacksStatuses.size());

        assertEquals(2, resourceUpdates.size(), "There should have been 2 resource save calls");
        assertEquals(0, getGp3VolumeCount(volumes), "There should be 0 gp3 volumes listed.");
        assertEquals(1, getInProgressVolumeCount(volumes), "There should be 1 in-progress volumes listed.");
        assertEquals(2, getInFailedVolumeCount(volumes), "There should be 2 failed volumes listed.");

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
        when(mockAmazonEc2Client.describeVolumeModifications(any(DescribeVolumesModificationsRequest.class))).thenAnswer(inv -> {
            DescribeVolumesModificationsRequest saved = inv.getArgument(0);
            List<VolumeModification> newVolumeModList = new ArrayList<>();
            for (String vid : saved.volumeIds()) {
                if ("vol-0".equals(vid)) {
                    newVolumeModList.add(VolumeModification.builder()
                            .volumeId("vol-0")
                            .modificationState(VolumeModificationState.COMPLETED)
                            .build());
                }
            }
            return DescribeVolumesModificationsResponse.builder()
                    .volumesModifications(newVolumeModList)
                    .build();
        });

        // Pass 3: The conversion is finished so the patch process should finish.
        boolean pass3Result = underTestLocal.doApply(stack);
        assertTrue(pass3Result, "Pass 3 should return true");

        // Check second status.
        assertEquals(2, stacksStatuses.size());
        StackStatus forthStatus = stacksStatuses.get(1);
        assertEquals(Status.AVAILABLE, forthStatus.getStatus());

        // Check to make sure resource as updated.
        assertEquals(3, resourceUpdates.size(), "There should have been 3 resource save calls");
        assertEquals(1, getGp3VolumeCount(volumes), "There should be 1 gp3 volumes listed.");
        assertEquals(0, getInProgressVolumeCount(volumes), "There should be 0 in-progress volumes listed.");
        assertEquals(2, getInFailedVolumeCount(volumes), "There should be 2 failed volumes listed.");
        assertEquals("GP2 migration finished. Failed: 2 Succeeded: 1", forthStatus.getStatusReason(), "Status reason should contain AVAILABLE message.");
    }

    @Test
    void testApplyWithMissingVolumeModification() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        List<StackStatus> stacksStatuses = setupTwoVolumeInitialPass(underTestLocal);

        doReturn(true).when(entitlementService).isGp2toGp3MigrationEnabled(any());

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(1, stacksStatuses.size());
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_IN_PROGRESS, firstStatus.getStatus());
        assertEquals("GP2 migration started for 2 GP2 volumes", firstStatus.getStatusReason(), "Status reason should contain started message.");

        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications: return MODIFYING (conversion still in progress)
        // The request for vol-1 should return an empty list
        when(mockAmazonEc2Client.describeVolumeModifications(any(DescribeVolumesModificationsRequest.class))).thenAnswer(inv -> {
            DescribeVolumesModificationsRequest saved = inv.getArgument(0);
            List<VolumeModification> newVolumeModList = new ArrayList<>();
            for (String vid : saved.volumeIds()) {
                if ("vol-0".equals(vid)) {
                    newVolumeModList.add(VolumeModification.builder()
                            .volumeId("vol-0")
                            .modificationState(VolumeModificationState.COMPLETED)
                            .build());
                }
            }
            return DescribeVolumesModificationsResponse.builder()
                    .volumesModifications(newVolumeModList)
                    .build();
        });

        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertTrue(pass2Result, "Pass 2 should return true");

        // Check the 2nd status.
        assertEquals(2, stacksStatuses.size());
        StackStatus thirdStatus = stacksStatuses.get(1);
        assertEquals(Status.AVAILABLE, thirdStatus.getStatus());
        assertEquals("GP2 migration finished. Failed: 1 Succeeded: 1", thirdStatus.getStatusReason(), "Status reason should contain AVAILABLE message.");
    }

    @Test
    void testApplySuccessUnknownVolumeModification() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        List<StackStatus> stacksStatuses = setupTwoVolumeInitialPass(underTestLocal);

        doReturn(true).when(entitlementService).isGp2toGp3MigrationEnabled(any());

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(1, stacksStatuses.size());
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_IN_PROGRESS, firstStatus.getStatus());
        assertEquals("GP2 migration started for 2 GP2 volumes", firstStatus.getStatusReason(), "Status reason should contain started message.");

        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications.
        when(mockAmazonEc2Client.describeVolumeModifications(any(DescribeVolumesModificationsRequest.class))).thenAnswer(inv -> {
            DescribeVolumesModificationsRequest saved = inv.getArgument(0);
            List<VolumeModification> newVolumeModList = new ArrayList<>();
            for (String vid : saved.volumeIds()) {
                if ("vol-0".equals(vid)) {
                    newVolumeModList.add(VolumeModification.builder()
                            .volumeId("vol-0")
                            .modificationState(VolumeModificationState.COMPLETED)
                            .build());
                } else {
                    newVolumeModList.add(VolumeModification.builder()
                            .volumeId("vol-1")
                            .modificationState(VolumeModificationState.UNKNOWN_TO_SDK_VERSION)
                            .build());
                }
            }
            return DescribeVolumesModificationsResponse.builder()
                    .volumesModifications(newVolumeModList)
                    .build();
        });
        // AWS describeVolumes: return GP3 volume in-use
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class))).thenAnswer(inv -> {
            DescribeVolumesRequest saved = inv.getArgument(0);
            List<Volume> newVolumeList = new ArrayList<>();
            for (String vid : saved.volumeIds()) {
                newVolumeList.add(Volume.builder()
                        .volumeId(vid)
                        .size(100)
                        .volumeType(VolumeType.GP3)
                        .state(VolumeState.IN_USE)
                        .build());
            }
            return DescribeVolumesResponse.builder()
                    .volumes(newVolumeList)
                    .build();
        });

        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertTrue(pass2Result, "Pass 2 should return true");

        // Check the 3rd status.
        assertEquals(2, stacksStatuses.size());
        StackStatus thirdStatus = stacksStatuses.get(1);
        assertEquals(Status.AVAILABLE, thirdStatus.getStatus());
        assertEquals("GP2 migration finished. Failed: 0 Succeeded: 2", thirdStatus.getStatusReason(), "Status reason should contain AVAILABLE message.");
    }

    @Test
    void testApplyFailureUnknownVolumeModification() throws Exception {
        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        List<StackStatus> stacksStatuses = setupTwoVolumeInitialPass(underTestLocal);

        doReturn(true).when(entitlementService).isGp2toGp3MigrationEnabled(any());

        // Store the resource save call.
        Resource[] updatedResource = new Resource[1];
        when(resourceService.save(any(Resource.class))).thenAnswer(inv -> {
            Resource saved = inv.getArgument(0);
            updatedResource[0] = saved;
            return saved;
        });

        // Execute pass 1
        boolean pass1Result = underTestLocal.doApply(stack);

        // Check return value should be false.
        assertFalse(pass1Result, "Pass 1 should return false (migration started, another pass needed)");

        // Check first status.
        assertEquals(1, stacksStatuses.size());
        StackStatus firstStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_IN_PROGRESS, firstStatus.getStatus());
        assertEquals("GP2 migration started for 2 GP2 volumes", firstStatus.getStatusReason(), "Status reason should contain started message.");

        // ------
        // Setup for Pass 2
        // ------

        // AWS describeVolumeModifications.
        when(mockAmazonEc2Client.describeVolumeModifications(any(DescribeVolumesModificationsRequest.class))).thenAnswer(inv -> {
            DescribeVolumesModificationsRequest saved = inv.getArgument(0);
            List<VolumeModification> newVolumeModList = new ArrayList<>();
            for (String vid : saved.volumeIds()) {
                if ("vol-0".equals(vid)) {
                    newVolumeModList.add(VolumeModification.builder()
                            .volumeId("vol-0")
                            .modificationState(VolumeModificationState.COMPLETED)
                            .build());
                } else {
                    newVolumeModList.add(VolumeModification.builder()
                            .volumeId("vol-1")
                            .modificationState(VolumeModificationState.UNKNOWN_TO_SDK_VERSION)
                            .build());
                }
            }
            return DescribeVolumesModificationsResponse.builder()
                    .volumesModifications(newVolumeModList)
                    .build();
        });

        // AWS describeVolumes:
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class))).thenAnswer(inv -> {
            DescribeVolumesRequest saved = inv.getArgument(0);
            List<Volume> newVolumeList = new ArrayList<>();
            for (String vid : saved.volumeIds()) {
                if ("vol-0".equals(vid)) {
                    newVolumeList.add(Volume.builder()
                            .volumeId(vid)
                            .size(100)
                            .volumeType(VolumeType.GP3)
                            .state(VolumeState.IN_USE)
                            .build());
                } else {
                    newVolumeList.add(Volume.builder()
                            .volumeId(vid)
                            .size(100)
                            .volumeType(VolumeType.GP2)
                            .state(VolumeState.IN_USE)
                            .build());
                }
            }
            return DescribeVolumesResponse.builder()
                    .volumes(newVolumeList)
                    .build();
        });

        doReturn(List.of(updatedResource[0])).when(resourceService)
                .findAllByStackIdAndResourceTypeIn(eq(STACK_ID), any());

        // Execute pass 2
        boolean pass2Result = underTestLocal.doApply(stack);

        assertFalse(pass2Result, "Pass 2 should return false");

        // Check to make sure there is still one status.
        assertEquals(1, stacksStatuses.size());
        StackStatus thirdStatus = stacksStatuses.get(0);
        assertEquals(Status.UPDATE_IN_PROGRESS, thirdStatus.getStatus());
    }

    @Test
    void testGetGp2VolumesFromResourcesWithNonVolumeResources() {
        // Given - Create resources that are not AWS_VOLUMESET or AWS_ROOT_DISK
        Resource resource1 = new Resource();
        resource1.setResourceType(ResourceType.AWS_INSTANCE);

        Resource resource2 = new Resource();
        resource2.setResourceType(ResourceType.AWS_SUBNET);

        List<Resource> resources = new ArrayList<>();
        resources.add(resource1);
        resources.add(resource2);

        doReturn(Optional.empty()).when(resourceAttributeUtil).getTypedAttributes(any());

        List<VolumeSetAttributes.Volume> result = underTest.getGp2VolumesFromResources(resources);
        assertTrue(result.isEmpty(), "Should return empty list when resources are not AWS_VOLUMESET or AWS_ROOT_DISK");
    }

    @Test
    void testGetGp2VolumesFromResourcesWithOnlyGp3Volume() {
        // Given - Create AWS_VOLUMESET resource with only GP3 volume
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AWS_VOLUMESET);
        List<Resource> resources = new ArrayList<>();
        resources.add(resource);

        // Create VolumeSetAttributes with GP3 volume
        VolumeSetAttributes.Volume gp3Volume = new VolumeSetAttributes.Volume(
                "vol-123", "/dev/sdf", 100, "gp3", null);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                "us-west-1a", true, null, List.of(gp3Volume), 100, "gp3");
        // Mock resourceAttributeUtil to return VolumeSetAttributes
        doReturn(Optional.of(volumeSetAttributes)).when(resourceAttributeUtil).getTypedAttributes(resource);

        List<VolumeSetAttributes.Volume> result = underTest.getGp2VolumesFromResources(resources);
        assertTrue(result.isEmpty(), "Should return empty list when volume is GP3, not GP2");
    }

    @Test
    void testGetGp2VolumesFromResourcesWithOnlyGp2Volume() {
        // Given - Create AWS_VOLUMESET resource with only GP2 volume
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AWS_VOLUMESET);
        List<Resource> resources = new ArrayList<>();
        resources.add(resource);

        // Create VolumeSetAttributes with GP2 volume
        VolumeSetAttributes.Volume gp2Volume = new VolumeSetAttributes.Volume(
                "vol-123", "/dev/sdf", 100, "gp2", null);
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                "us-west-1a", true, null, List.of(gp2Volume), 100, "gp2");
        // Mock resourceAttributeUtil to return VolumeSetAttributes
        doReturn(Optional.of(volumeSetAttributes)).when(resourceAttributeUtil).getTypedAttributes(resource);

        List<VolumeSetAttributes.Volume> result = underTest.getGp2VolumesFromResources(resources);
        assertEquals(1, result.size(), "Should only return a single value");
    }

    @Test
    void testGetGp2VolumesFromResourcesWithNullOptional() {
        // Given - Create AWS_VOLUMESET resource with only GP2 volume
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AWS_VOLUMESET);

        List<Resource> resources = new ArrayList<>();
        resources.add(resource);

        // Mock resourceAttributeUtil to return VolumeSetAttributes
        doReturn(Optional.empty()).when(resourceAttributeUtil).getTypedAttributes(any());

        List<VolumeSetAttributes.Volume> result = underTest.getGp2VolumesFromResources(resources);
        assertTrue(result.isEmpty(), "Should only return an empty list.");
    }

    @Test
    void testGetGp2VolumesFromResourcesEmptyVolumeSet() {
        // Given - Create AWS_VOLUMESET resource with only GP2 volume
        Resource resource = new Resource();
        resource.setResourceType(ResourceType.AWS_VOLUMESET);
        List<Resource> resources = new ArrayList<>();
        resources.add(resource);

        // Create Empty VolumeSetAttributes
        VolumeSetAttributes volumeSetAttributes = new VolumeSetAttributes(
                "us-west-1a", true, null, List.of(), 100, "gp2");
        // Mock resourceAttributeUtil to return VolumeSetAttributes
        doReturn(Optional.of(volumeSetAttributes)).when(resourceAttributeUtil).getTypedAttributes(resource);

        List<VolumeSetAttributes.Volume> result = underTest.getGp2VolumesFromResources(resources);
        assertTrue(result.isEmpty(), "Should only return an empty list.");
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
        when(volumeIopsCalculator.getEquivalentGp3IopsForGp2Volume(anyInt())).thenReturn(3000);

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

    private int getInProgressVolumeCount(List<VolumeSetAttributes.Volume> volumes) {
        int  count = 0;
        for (VolumeSetAttributes.Volume volume : volumes) {
            if (CloudVolumeModificationState.GP2_TO_GP3_IN_PROGRESS.equals(volume.getModificationState())) {
                count++;
            }
        }
        return count;
    }

    private int getInFailedVolumeCount(List<VolumeSetAttributes.Volume> volumes) {
        int  count = 0;
        for (VolumeSetAttributes.Volume volume : volumes) {
            if (CloudVolumeModificationState.GP2_TO_GP3_FAILED.equals(volume.getModificationState())) {
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

}