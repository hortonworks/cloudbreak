package com.sequenceiq.cloudbreak.service.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.EntitlementService;
import com.sequenceiq.cloudbreak.cloud.aws.common.client.AmazonEc2Client;
import com.sequenceiq.cloudbreak.cloud.aws.common.resource.volume.AwsVolumeIopsCalculator;
import com.sequenceiq.cloudbreak.cloud.aws.common.service.AwsCommonDiskUpdateService;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeModificationState;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.converter.spi.CredentialToCloudCredentialConverter;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Database;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.job.stackpatcher.config.ExistingStackPatcherConfig;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.repository.ResourceRepository;
import com.sequenceiq.cloudbreak.repository.StackRepository;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.environment.credential.CredentialClientService;
import com.sequenceiq.cloudbreak.service.loadbalancer.LoadBalancerFqdnUtil;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.secret.service.SecretAspectService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.stackpatch.AwsGp2ToGp3PatchService;
import com.sequenceiq.cloudbreak.service.stackpatch.StackPatchUsageReporterService;
import com.sequenceiq.cloudbreak.service.stackstatus.StackStatusService;
import com.sequenceiq.cloudbreak.util.CloudConnectorHelper;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.workspace.model.Tenant;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.TenantRepository;
import com.sequenceiq.cloudbreak.workspace.repository.workspace.WorkspaceRepository;
import com.sequenceiq.common.api.type.InstanceGroupType;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.flow.core.FlowLogService;
import com.sequenceiq.flow.service.FlowRetryService;

import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesModificationsResponse;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesRequest;
import software.amazon.awssdk.services.ec2.model.DescribeVolumesResponse;
import software.amazon.awssdk.services.ec2.model.Volume;
import software.amazon.awssdk.services.ec2.model.VolumeModification;
import software.amazon.awssdk.services.ec2.model.VolumeModificationState;
import software.amazon.awssdk.services.ec2.model.VolumeState;
import software.amazon.awssdk.services.ec2.model.VolumeType;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SpringExtension.class)
@SpringBootTest
@ContextConfiguration(initializers = ComponentTestConfig.TestEnvironmentInitializer.class, classes = ComponentTestConfig.class)
@Testcontainers
class AwsGp2ToGp3PatchServiceComponentTest {

    @Container
    public static final PostgreSQLContainer POSTGRES_CONTAINER = new PostgreSQLContainer("postgres:13.2-alpine")
            .withUsername("postgres")
            .withPassword("postgres");

    private static final String STACK_CRN = "crn:cdp:datahub:us-west-1:tenant:cluster:uuid";

    private static final String ENV_CRN = "crn:cdp:environments:us-west-1:tenant:environment:uuid";

    private static final String STACK_NAME = "test-stack";

    private static final String AWS_PLATFORM = "AWS";

    private static final String REGION = "us-west-1";

    private static final String INSTANCE_ID = "i-instance123";

    /**
     * Serialized {@link VolumeSetAttributes} for the DB row (avoids Jackson ambiguity on {@code Volume} constructors when using {@code new Json(object)}).
     */
    private static final String GP2_VOLUMESET_JSON = """
        {
            "availabilityZone": "us-west-1a",
            "deleteOnTermination": true,
            "fstab": "",
            "uuids": "",
            "volumes": [
                {
                    "id": "vol-0",
                    "device": "/dev/xvd0",
                    "size": 100,
                    "type": "gp2",
                    "cloudVolumeUsageType": "GENERAL"
                },
                {
                    "id": "vol-1",
                    "device": "/dev/xvd1",
                    "size": 100,
                    "type": "gp2",
                    "cloudVolumeUsageType": "GENERAL"
                },
                {
                    "id": "vol-2",
                    "device": "/dev/xvd2",
                    "size": 100,
                    "type": "gp2",
                    "cloudVolumeUsageType": "GENERAL"
                }
            ],
            "discoveryFQDN": "m1.cloudera.site"
        }
        """;

    @Inject
    private ResourceAttributeUtil resourceAttributeUtil;

    @Inject
    private ResourceService resourceService;

    @MockBean
    private CloudConnectorHelper cloudConnectorHelper;

    @MockBean
    private CredentialToCloudCredentialConverter credentialToCloudCredentialConverter;

    @MockBean
    private CredentialClientService credentialClientService;

    @MockBean
    private LoadBalancerFqdnUtil soadBalancerFqdnUtil;

    @MockBean
    private HostOrchestrator hostOrchestrator;

    @MockBean
    private AwsCommonDiskUpdateService awsCommonDiskUpdateService;

    @Inject
    private AwsVolumeIopsCalculator volumeIopsCalculator;

    @Inject
    private StackStatusService stackStatusService;

    @MockBean
    private StackService stackService;

    @MockBean
    private GatewayConfigService gatewayConfigService;

    @Inject
    private StackUtil stackUtil;

    @MockBean
    private FlowLogService flowLogService;

    @MockBean
    private FlowRetryService flowRetryService;

    @MockBean
    private ExistingStackPatcherConfig existingStackPatcherConfig;

    @MockBean
    private EntitlementService entitlementService;

    @MockBean
    private StackPatchUsageReporterService stackPatchUsageReporterService;

    @Mock
    private AmazonEc2Client mockAmazonEc2Client;

    // Avoid SecretAspectService.init() pre-invoking repositories (triggers SecretAspects WARN spam without Vault).
    @MockBean
    private SecretAspectService secretAspectService;

    @Inject
    private StackRepository stackRepository;

    @Inject
    private ResourceRepository resourceRepository;

    @Inject
    private TenantRepository tenantRepository;

    @Inject
    private WorkspaceRepository workspaceRepository;

    @Inject
    private AwsGp2ToGp3PatchService underTest;

    private Stack stack;

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

    /**
     * Inserts a minimal {@code stack} and {@code resource} row (plus workspace/tenant) for component verification.
     */
    private void saveTestStackAndResourceInDatabase() {
        Tenant tenant = new Tenant();
        tenant.setName("aws-gp2-gp3-test-tenant");
        tenant = tenantRepository.save(tenant);

        Workspace workspace = new Workspace();
        workspace.setName("aws-gp2-gp3-test-ws");
        workspace.setTenant(tenant);
        workspace = workspaceRepository.save(workspace);

        stack = new Stack();
        stack.setDatabase(new Database());
        stack.setName(STACK_NAME);
        stack.setResourceCrn(STACK_CRN);
        stack.setEnvironmentCrn(ENV_CRN);
        stack.setCloudPlatform(AWS_PLATFORM);
        stack.setPlatformVariant(AWS_PLATFORM);
        stack.setRegion(REGION);
        stack.setWorkspace(workspace);
        stack.setInstanceGroups(createInstanceGroupsWithMetadata());
        stack = stackRepository.save(stack);

        Resource dbResource = new Resource(ResourceType.AWS_VOLUMESET, "resource-1", stack, "us-west-1a");
        dbResource.setAttributes(new Json(GP2_VOLUMESET_JSON));
        resourceRepository.save(dbResource);
    }

    @Test
    @Transactional
    void testApplyThreePassesWhenConversionTakesLongerThanJobInterval() throws Exception {
        when(flowLogService.isOtherFlowRunning(anyLong())).thenReturn(false);
        when(existingStackPatcherConfig.getIntervalInHours()).thenReturn(6);

        AwsGp2ToGp3PatchService underTestLocal = spy(underTest);

        saveTestStackAndResourceInDatabase();
        long stackId = stack.getId();
        assertTrue(stackRepository.findById(stackId).isPresent(), "stack row should exist");
        assertEquals(1, resourceRepository.findAllByStackId(stackId).size(), "one resource row should exist for the stack");
        assertEquals(1, resourceRepository.findAllByStackIdAndResourceTypeIn(stackId,
                List.of(ResourceType.AWS_ROOT_DISK, ResourceType.AWS_VOLUMESET)).size(), "one resource row should exist for the stack");

        doReturn(mockAmazonEc2Client).when(underTestLocal).getAwsClient(any());

        doReturn(true).when(entitlementService).isGp2toGp3MigrationEnabled(any());

        Volume awsVolume0 = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        Volume awsVolume1 = Volume.builder()
                .volumeId("vol-1")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        Volume awsVolume2 = Volume.builder()
                .volumeId("vol-2")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(DescribeVolumesResponse.builder().volumes(awsVolume0, awsVolume1, awsVolume2).build());
        when(awsCommonDiskUpdateService.modifyVolumesWithIops(any(), any(), any(), any())).thenReturn(null);

        boolean pass1Result = underTestLocal.apply(stack);

        assertFalse(pass1Result, "migration should need another pass after work is started");
        List<Resource> resources = resourceRepository.findAllByStackIdAndResourceTypeIn(stackId,
                List.of(ResourceType.AWS_ROOT_DISK, ResourceType.AWS_VOLUMESET));
        assertEquals(1, resources.size(), "one resource row should exist for the stack");
        Optional<VolumeSetAttributes> volumeSetOptional = resourceAttributeUtil.getTypedAttributes(resources.getFirst(), VolumeSetAttributes.class);
        assertTrue(volumeSetOptional.isPresent(), "volume set attributes should exist");
        List<VolumeSetAttributes.Volume> volumes = volumeSetOptional.get().getVolumes();
        assertEquals(3, volumes.size(), "three volumes should exist for the stack");
        for (VolumeSetAttributes.Volume volume : volumes) {
            if (volume.getId().equals("vol-0")) {
                assertEquals(CloudVolumeModificationState.GP2_TO_GP3_IN_PROGRESS, volume.getModificationState(),
                        "modification state should be GP2_TO_GP3_IN_PROGRESS");
            }
            if (volume.getId().equals("vol-1")) {
                assertEquals(CloudVolumeModificationState.GP2_TO_GP3_IN_PROGRESS, volume.getModificationState(),
                        "modification state should be GP2_TO_GP3_IN_PROGRESS");
            }
            if (volume.getId().equals("vol-2")) {
                assertEquals(CloudVolumeModificationState.GP2_TO_GP3_IN_PROGRESS, volume.getModificationState(),
                        "modification state should be GP2_TO_GP3_IN_PROGRESS");
            }
        }

        //----

        awsVolume0 = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP3)
                .state(VolumeState.IN_USE)
                .build();
        awsVolume1 = Volume.builder()
                .volumeId("vol-1")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        awsVolume2 = Volume.builder()
                .volumeId("vol-2")
                .size(100)
                .volumeType(VolumeType.GP2)
                .state(VolumeState.IN_USE)
                .build();
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(DescribeVolumesResponse.builder().volumes(awsVolume0, awsVolume1, awsVolume2).build());

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
                            .volumeId(vid)
                            .modificationState(VolumeModificationState.MODIFYING)
                            .build());
                }
            }
            return DescribeVolumesModificationsResponse.builder()
                    .volumesModifications(newVolumeModList)
                    .build();
        });


        boolean pass2Result = underTestLocal.apply(stack);

        assertFalse(pass2Result, "migration should need another pass after work is started");
        resources = resourceRepository.findAllByStackIdAndResourceTypeIn(stackId, List.of(ResourceType.AWS_ROOT_DISK, ResourceType.AWS_VOLUMESET));
        assertEquals(1, resources.size(), "one resource row should exist for the stack");
        volumeSetOptional = resourceAttributeUtil.getTypedAttributes(resources.getFirst(), VolumeSetAttributes.class);
        assertTrue(volumeSetOptional.isPresent(), "volume set attributes should exist");
        volumes = volumeSetOptional.get().getVolumes();
        assertEquals(3, volumes.size(), "three volumes should exist for the stack");
        for (VolumeSetAttributes.Volume volume : volumes) {
            if (volume.getId().equals("vol-0")) {
                assertEquals(CloudVolumeModificationState.GP2_TO_GP3_FINISHED, volume.getModificationState(),
                        "modification state should be GP2_TO_GP3_FINISHED");
            }
            if (volume.getId().equals("vol-1")) {
                assertEquals(CloudVolumeModificationState.GP2_TO_GP3_IN_PROGRESS, volume.getModificationState(),
                        "modification state should be GP2_TO_GP3_IN_PROGRESS");
            }
            if (volume.getId().equals("vol-2")) {
                assertEquals(CloudVolumeModificationState.GP2_TO_GP3_IN_PROGRESS, volume.getModificationState(),
                        "modification state should be GP2_TO_GP3_IN_PROGRESS");
            }
        }

        //----

        awsVolume0 = Volume.builder()
                .volumeId("vol-0")
                .size(100)
                .volumeType(VolumeType.GP3)
                .state(VolumeState.IN_USE)
                .build();
        awsVolume1 = Volume.builder()
                .volumeId("vol-1")
                .size(100)
                .volumeType(VolumeType.GP3)
                .state(VolumeState.IN_USE)
                .build();
        awsVolume2 = Volume.builder()
                .volumeId("vol-2")
                .size(100)
                .volumeType(VolumeType.GP3)
                .state(VolumeState.IN_USE)
                .build();
        when(mockAmazonEc2Client.describeVolumes(any(DescribeVolumesRequest.class)))
                .thenReturn(DescribeVolumesResponse.builder().volumes(awsVolume0, awsVolume1, awsVolume2).build());

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


        boolean pass3Result = underTestLocal.apply(stack);

        assertTrue(pass3Result, "migration should need another pass after work is started");
        List<StackStatus> statusList = stackStatusService.findAllStackStatusesById(stack.getId());
        assertEquals(2, statusList.size(), "status list should have two entries");
        List<StackStatus> sorted = statusList.stream()
                .sorted(Comparator.comparing(StackStatus::getCreated))
                .toList();
        assertEquals(Status.UPDATE_IN_PROGRESS, sorted.getFirst().getStatus(), "one resource row should exist for the stack");
        assertEquals(Status.AVAILABLE, sorted.getLast().getStatus(), "one resource row should exist for the stack");
        resources = resourceRepository.findAllByStackIdAndResourceTypeIn(stackId, List.of(ResourceType.AWS_ROOT_DISK, ResourceType.AWS_VOLUMESET));
        assertEquals(1, resources.size(), "one resource row should exist for the stack");
        volumeSetOptional = resourceAttributeUtil.getTypedAttributes(resources.getFirst(), VolumeSetAttributes.class);
        assertTrue(volumeSetOptional.isPresent(), "volume set attributes should exist");
        volumes = volumeSetOptional.get().getVolumes();
        assertEquals(3, volumes.size(), "three volumes should exist for the stack");
        for (VolumeSetAttributes.Volume volume : volumes) {
            if (volume.getId().equals("vol-0")) {
                assertEquals(CloudVolumeModificationState.GP2_TO_GP3_FINISHED, volume.getModificationState(),
                        "modification state should be GP2_TO_GP3_FINISHED");
            }
            if (volume.getId().equals("vol-1")) {
                assertEquals(CloudVolumeModificationState.GP2_TO_GP3_FINISHED, volume.getModificationState(),
                        "modification state should be GP2_TO_GP3_FINISHED");
            }
            if (volume.getId().equals("vol-2")) {
                assertEquals(CloudVolumeModificationState.GP2_TO_GP3_FINISHED, volume.getModificationState(),
                        "modification state should be GP2_TO_GP3_FINISHED");
            }
        }

    }

}
