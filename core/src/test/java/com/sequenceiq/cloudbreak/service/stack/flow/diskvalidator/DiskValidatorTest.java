package com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VOLUME_MISSING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VOLUME_MISSING_BY_SIZE;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VOLUME_MOUNT_MISSING;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_VOLUME_SIZE_MISMATCH;
import static com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricTag.ISSUE_TYPE;
import static com.sequenceiq.cloudbreak.service.metrics.CloudbreakMetricTag.PLATFORM_VARIANT;
import static com.sequenceiq.cloudbreak.service.metrics.MetricType.VOLUME_MOUNT_MISSING;
import static com.sequenceiq.cloudbreak.service.metrics.MetricType.VOLUME_MOUNT_SIZE_MISMATCH;
import static com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator.DiskValidator.VOLUMES_INADEQUATE_EVENT_TYPE;
import static com.sequenceiq.cloudbreak.service.stack.flow.diskvalidator.LsblkFetcher.LSBLK_COMMAND;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudVolumeUsageType;
import com.sequenceiq.cloudbreak.cloud.model.VolumeSetAttributes;
import com.sequenceiq.cloudbreak.cluster.util.ResourceAttributeUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.cloudbreak.common.metrics.MetricService;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.common.type.CloudConstants;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.structuredevent.event.CloudbreakEventService;
import com.sequenceiq.common.api.type.ResourceType;

@ExtendWith(MockitoExtension.class)
class DiskValidatorTest {

    public static final String FETCHER_SCRIPT = "fetcher_script";

    private final String awsLsblkOutput = """
            nvme0n1 200\s
            nvme1n1 1000 /hadoopfs/fs4
            nvme3n1 1000 /hadoopfs/fs3
            nvme2n1 1000 /hadoopfs/fs2
            nvme4n1 1000 /hadoopfs/fs1
            """;

    private final String gcpLsblkOutput = """
            sda 200\s
            sdb 1000 /hadoopfs/fs1
            sdc 1000 /hadoopfs/fs2
            sdd 1000 /hadoopfs/fs3
            sde 1000 /hadoopfs/fs4
            """;

    private final String azureLsblkOuptut = """
            sda 200\s
            sdb 64\s
            sdc 1000 /hadoopfs/fs2
            sdd 1000 /hadoopfs/fs3
            sde 1000 /hadoopfs/fs1
            sdf 1000 /hadoopfs/fs4
            """;

    @Spy
    private ResourceAttributeUtil resourceAttributeUtil;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private CloudbreakEventService cloudbreakEventService;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private MetricService metricService;

    @InjectMocks
    private LsblkFetcher lsblkFetcher = spy(new LsblkFetcher());

    @InjectMocks
    private VolumeIdWithDeviceFetcher volumeIdWithDeviceFetcher = spy(new VolumeIdWithDeviceFetcher());

    @InjectMocks
    private DiskValidator underTest;

    private final AtomicLong id = new AtomicLong(0);

    @BeforeEach
    public void init() {
        CloudConnector cloudConnector = mock(CloudConnector.class);
        when(cloudConnector.scriptResources()).thenReturn(() -> FETCHER_SCRIPT);
        when(cloudPlatformConnectors.get(any())).thenReturn(cloudConnector);
    }

    @Test
    public void testValidateDisksOnAWS() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setPlatformVariant(CloudConstants.AWS);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1AwsResources());
        stackResources.addAll(createNode2AwsResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);
        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", awsLsblkOutput, "fqdn2", awsLsblkOutput));
        mockAwsVolumesCommand(allGatewayConfigs, false, false);
        underTest.validateDisks(stack, Set.of(node1, node2));
        verifyNoInteractions(cloudbreakEventService);
        verifyNoInteractions(metricService);
    }

    @Test
    public void testValidateDisksOnGCP() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setPlatformVariant(CloudConstants.GCP);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1GcpResources());
        stackResources.addAll(createNode2GcpResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);
        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", gcpLsblkOutput, "fqdn2", gcpLsblkOutput));
        mockGcpVolumesCommand(allGatewayConfigs, false, false);
        underTest.validateDisks(stack, Set.of(node1, node2));
        verifyNoInteractions(cloudbreakEventService);
        verifyNoInteractions(metricService);
    }

    @Test
    public void testValidateDisksButOneMountMissingOnAWS() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setId(2L);
        stack.setPlatformVariant(CloudConstants.AWS);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1AwsResources());
        stackResources.addAll(createNode2AwsResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);

        String missingLsblkOutput = """
            nvme0n1 200\s
            nvme1n1 1000 /hadoopfs/fs4
            nvme3n1 1000 /hadoopfs/fs3
            nvme2n1 1000 /hadoopfs/fs2
            nvme4n1 1000
            """;

        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", awsLsblkOutput, "fqdn2", missingLsblkOutput));
        mockAwsVolumesCommand(allGatewayConfigs, false, false);
        underTest.validateDisks(stack, Set.of(node1, node2));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MOUNT_MISSING, List.of("vol-234df4235sdf4423d", "fqdn2"));
        verify(metricService, times(1)).incrementMetricCounter(VOLUME_MOUNT_MISSING,
                ISSUE_TYPE.name(), CLUSTER_VOLUME_MOUNT_MISSING.name(),
                PLATFORM_VARIANT.name(), stack.getPlatformVariant());
        verifyNoMoreInteractions(cloudbreakEventService);
        verifyNoMoreInteractions(metricService);
    }

    @Test
    public void testValidateDisksButOneMountMissingOnGCP() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setId(2L);
        stack.setPlatformVariant(CloudConstants.GCP);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1GcpResources());
        stackResources.addAll(createNode2GcpResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);

        String missingLsblkOutput = """
            sda 200\s
            sdb 1000 /hadoopfs/fs1
            sdc 1000 /hadoopfs/fs2
            sdd 1000 /hadoopfs/fs3
            sde 1000
            """;

        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", gcpLsblkOutput, "fqdn2", missingLsblkOutput));
        mockGcpVolumesCommand(allGatewayConfigs, false, false);
        underTest.validateDisks(stack, Set.of(node1, node2));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MOUNT_MISSING, List.of("perdosdhgcp-w-2-3-1737381500806", "fqdn2"));
        verify(metricService, times(1)).incrementMetricCounter(VOLUME_MOUNT_MISSING,
                ISSUE_TYPE.name(), CLUSTER_VOLUME_MOUNT_MISSING.name(),
                PLATFORM_VARIANT.name(), stack.getPlatformVariant());
        verifyNoMoreInteractions(cloudbreakEventService);
        verifyNoMoreInteractions(metricService);
    }

    @Test
    public void testValidateDisksButTwoDisksAreMissingOnAWS() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setId(2L);
        stack.setPlatformVariant(CloudConstants.AWS);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1AwsResources());
        stackResources.addAll(createNode2AwsResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);

        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", awsLsblkOutput, "fqdn2", awsLsblkOutput));
        mockAwsVolumesCommand(allGatewayConfigs, true, false);
        underTest.validateDisks(stack, Set.of(node1, node2));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("vol-6573543xcfasferq1", "fqdn2"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("vol-234df4235sdf4423d", "fqdn2"));
        verify(metricService, times(2)).incrementMetricCounter(VOLUME_MOUNT_MISSING,
                ISSUE_TYPE.name(), CLUSTER_VOLUME_MISSING.name(),
                PLATFORM_VARIANT.name(), stack.getPlatformVariant());
        verifyNoMoreInteractions(cloudbreakEventService);
        verifyNoMoreInteractions(metricService);
    }

    @Test
    public void testValidateDisksButTwoDisksAreMissingOnGCP() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setPlatformVariant(CloudConstants.GCP);
        stack.setId(2L);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1GcpResources());
        stackResources.addAll(createNode2GcpResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);

        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", gcpLsblkOutput, "fqdn2", gcpLsblkOutput));
        mockGcpVolumesCommand(allGatewayConfigs, true, false);
        underTest.validateDisks(stack, Set.of(node1, node2));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("perdosdhgcp-w-2-3-1737381500806", "fqdn2"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("perdosdhgcp-w-2-2-1737381500806", "fqdn2"));
        verify(metricService, times(2)).incrementMetricCounter(VOLUME_MOUNT_MISSING,
                ISSUE_TYPE.name(), CLUSTER_VOLUME_MISSING.name(),
                PLATFORM_VARIANT.name(), stack.getPlatformVariant());
        verifyNoMoreInteractions(cloudbreakEventService);
        verifyNoMoreInteractions(metricService);
    }

    @Test
    public void testValidateDisksButTwoDisksAreDifferentSizeOnAWS() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setId(2L);
        stack.setPlatformVariant(CloudConstants.AWS);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1AwsResources());
        stackResources.addAll(createNode2AwsResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);

        String awsLsblkDifferentSizeOutput = """
            nvme0n1 200\s
            nvme1n1 1000 /hadoopfs/fs4
            nvme3n1 1000 /hadoopfs/fs3
            nvme2n1 500 /hadoopfs/fs2
            nvme4n1 500 /hadoopfs/fs1
            """;

        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", awsLsblkOutput, "fqdn2", awsLsblkDifferentSizeOutput));
        mockAwsVolumesCommand(allGatewayConfigs, false, false);
        underTest.validateDisks(stack, Set.of(node1, node2));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_SIZE_MISMATCH, List.of("vol-7654567asd123asd1", "fqdn2", "1000 GiB", "500 GiB"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_SIZE_MISMATCH, List.of("vol-234df4235sdf4423d", "fqdn2", "1000 GiB", "500 GiB"));
        verify(metricService, times(2)).incrementMetricCounter(VOLUME_MOUNT_SIZE_MISMATCH,
                PLATFORM_VARIANT.name(), stack.getPlatformVariant());
        verifyNoMoreInteractions(cloudbreakEventService);
        verifyNoMoreInteractions(metricService);
    }

    @Test
    public void testValidateDisksButTwoDisksAreDifferentSizeAndLsblkDiskWithoutSizeOnGCP() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setId(2L);
        stack.setPlatformVariant(CloudConstants.GCP);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1GcpResources());
        stackResources.addAll(createNode2GcpResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);

        String gcpLsblkDifferentSizeOutput = """
            sda 200\s
            sdb 1000 /hadoopfs/fs1
            sdc 1000 /hadoopfs/fs2
            sdd 500 /hadoopfs/fs3
            sde 500 /hadoopfs/fs4
            sdf
            """;

        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", gcpLsblkOutput, "fqdn2", gcpLsblkDifferentSizeOutput));
        mockGcpVolumesCommand(allGatewayConfigs, false, false);
        underTest.validateDisks(stack, Set.of(node1, node2));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_SIZE_MISMATCH, List.of("perdosdhgcp-w-2-2-1737381500806", "fqdn2", "1000 GiB", "500 GiB"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_SIZE_MISMATCH, List.of("perdosdhgcp-w-2-3-1737381500806", "fqdn2", "1000 GiB", "500 GiB"));
        verify(metricService, times(2)).incrementMetricCounter(VOLUME_MOUNT_SIZE_MISMATCH,
                PLATFORM_VARIANT.name(), stack.getPlatformVariant());
        verifyNoMoreInteractions(cloudbreakEventService);
        verifyNoMoreInteractions(metricService);
    }

    @Test
    public void testValidateDisksButOneNodeIsMissingFromAWS() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setId(2L);
        stack.setPlatformVariant(CloudConstants.AWS);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1AwsResources());
        stackResources.addAll(createNode2AwsResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);

        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", awsLsblkOutput, "fqdn2", awsLsblkOutput));
        mockAwsVolumesCommand(allGatewayConfigs, false, true);
        underTest.validateDisks(stack, Set.of(node1, node2));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("vol-537634653asdazxcz", "fqdn2"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("vol-234df4235sdf4423d", "fqdn2"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("vol-6573543xcfasferq1", "fqdn2"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("vol-7654567asd123asd1", "fqdn2"));
        verify(metricService, times(4)).incrementMetricCounter(VOLUME_MOUNT_MISSING,
                ISSUE_TYPE.name(), CLUSTER_VOLUME_MISSING.name(),
                PLATFORM_VARIANT.name(), stack.getPlatformVariant());
        verifyNoMoreInteractions(cloudbreakEventService);
        verifyNoMoreInteractions(metricService);
    }

    @Test
    public void testValidateDisksButOneNodeIsMissingFromGCP() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setId(2L);
        stack.setPlatformVariant(CloudConstants.GCP);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1GcpResources());
        stackResources.addAll(createNode2GcpResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);

        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", gcpLsblkOutput, "fqdn2", gcpLsblkOutput));
        mockGcpVolumesCommand(allGatewayConfigs, false, true);
        underTest.validateDisks(stack, Set.of(node1, node2));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("perdosdhgcp-w-2-0-1737381500806", "fqdn2"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("perdosdhgcp-w-2-1-1737381500806", "fqdn2"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("perdosdhgcp-w-2-2-1737381500806", "fqdn2"));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING, List.of("perdosdhgcp-w-2-3-1737381500806", "fqdn2"));
        verify(metricService, times(4)).incrementMetricCounter(VOLUME_MOUNT_MISSING,
                ISSUE_TYPE.name(), CLUSTER_VOLUME_MISSING.name(),
                PLATFORM_VARIANT.name(), stack.getPlatformVariant());
        verifyNoMoreInteractions(cloudbreakEventService);
        verifyNoMoreInteractions(metricService);
    }

    @Test
    public void testValidateDisksOnAzure() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setPlatformVariant(CloudConstants.AZURE);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1AzureResources());
        stackResources.addAll(createNode2AzureResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);
        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", azureLsblkOuptut, "fqdn2", azureLsblkOuptut));
        underTest.validateDisks(stack, Set.of(node1, node2));
        verifyNoInteractions(cloudbreakEventService);
        verifyNoInteractions(metricService);
    }

    @Test
    public void testValidateDisksOnAzureButOneMountMissing() throws CloudbreakOrchestratorFailedException {
        Node node1 = new Node("1.1.1.1", null, "i-123", null, "fqdn1", "worker");
        Node node2 = new Node("1.1.1.2", null, "i-456", null, "fqdn2", "worker");

        Stack stack = new Stack();
        stack.setId(2L);
        stack.setPlatformVariant(CloudConstants.AZURE);

        Set<Resource> stackResources = new HashSet<>();
        stackResources.addAll(createNode1AzureResources());
        stackResources.addAll(createNode2AzureResources());
        stack.setResources(stackResources);

        List<GatewayConfig> allGatewayConfigs = mock(List.class);
        when(gatewayConfigService.getAllGatewayConfigs(stack)).thenReturn(allGatewayConfigs);

        String missingLsblkOutput = """
            sda 200\s
            sdb 64\s
            sdc 1000 /hadoopfs/fs2
            sdd 1000 /hadoopfs/fs3
            sde 1000\s
            """;

        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), LSBLK_COMMAND))
                .thenReturn(Map.of("fqdn1", azureLsblkOuptut, "fqdn2", missingLsblkOutput));
        underTest.validateDisks(stack, Set.of(node1, node2));
        verify(cloudbreakEventService, times(1)).fireCloudbreakEvent(stack.getId(), VOLUMES_INADEQUATE_EVENT_TYPE,
                CLUSTER_VOLUME_MISSING_BY_SIZE, List.of("fqdn2", "1000 GiB, 1000 GiB"));
        verify(metricService, times(1)).incrementMetricCounter(VOLUME_MOUNT_MISSING,
                ISSUE_TYPE.name(), CLUSTER_VOLUME_MISSING_BY_SIZE.name(),
                PLATFORM_VARIANT.name(), stack.getPlatformVariant());
        verifyNoMoreInteractions(cloudbreakEventService);
        verifyNoMoreInteractions(metricService);
    }

    private Set<Resource> createNode1GcpResources() {
        Set<Resource> resources = new HashSet<>();
        Map<String, String> node1volumes = Map.of(
                "perdosdhgcp-w-1-0-1737381500806", "/dev/nvme1n1",
                "perdosdhgcp-w-1-1-1737381500806", "/dev/nvme2n1",
                "perdosdhgcp-w-1-2-1737381500806", "/dev/nvme3n1",
                "perdosdhgcp-w-1-3-1737381500806", "/dev/nvme4n1");
        resources.add(createVolumeResource("i-123", "fqdn1", node1volumes, ResourceType.GCP_ATTACHED_DISKSET));
        return resources;
    }

    private Set<Resource> createNode2GcpResources() {
        Set<Resource> resources = new HashSet<>();
        Map<String, String> node2volumes = Map.of(
                "perdosdhgcp-w-2-0-1737381500806", "/dev/nvme1n1",
                "perdosdhgcp-w-2-1-1737381500806", "/dev/nvme2n1",
                "perdosdhgcp-w-2-2-1737381500806", "/dev/nvme3n1",
                "perdosdhgcp-w-2-3-1737381500806", "/dev/nvme4n1");
        resources.add(createVolumeResource("i-456", "fqdn2", node2volumes, ResourceType.GCP_ATTACHED_DISKSET));
        return resources;
    }

    private Set<Resource> createNode1AwsResources() {
        Set<Resource> resources = new HashSet<>();
        Map<String, String> node1volumes = Map.of(
                "vol-07e521a158c5ea7d8", "/dev/nvme1n1",
                "vol-094b6ec5b940febda", "/dev/nvme2n1",
                "vol-05f368fd17337d9b1", "/dev/nvme3n1",
                "vol-0aa314e189b706901", "/dev/nvme4n1");
        resources.add(createVolumeResource("i-123", "fqdn1", node1volumes, ResourceType.AWS_VOLUMESET));
        return resources;
    }

    private Set<Resource> createNode2AwsResources() {
        Set<Resource> resources = new HashSet<>();
        Map<String, String> node2volumes = Map.of(
                "vol-537634653asdazxcz", "/dev/nvme1n1",
                "vol-7654567asd123asd1", "/dev/nvme2n1",
                "vol-6573543xcfasferq1", "/dev/nvme3n1",
                "vol-234df4235sdf4423d", "/dev/nvme4n1");
        resources.add(createVolumeResource("i-456", "fqdn2", node2volumes, ResourceType.AWS_VOLUMESET));
        return resources;
    }

    private Set<Resource> createNode1AzureResources() {
        Set<Resource> resources = new HashSet<>();
        Map<String, String> node2volumes = Map.of(
                "node1volume1", "/dev/sdc",
                "node1volume2", "/dev/sdd",
                "node1volume3", "/dev/sde",
                "node1volume4", "/dev/sdf");
        resources.add(createVolumeResource("i-123", "fqdn1", node2volumes, ResourceType.AZURE_VOLUMESET));
        return resources;
    }

    private Set<Resource> createNode2AzureResources() {
        Set<Resource> resources = new HashSet<>();
        Map<String, String> node2volumes = Map.of(
                "node2volume1", "/dev/sdc",
                "node2volume2", "/dev/sdd",
                "node2volume3", "/dev/sde",
                "node2volume4", "/dev/sdf");
        resources.add(createVolumeResource("i-456", "fqdn1", node2volumes, ResourceType.AZURE_VOLUMESET));
        return resources;
    }

    private Resource createVolumeResource(String instanceId, String fqdn, Map<String, String> volumeIdsWithDevice, ResourceType resourceType) {
        Resource resource = new Resource();
        List<VolumeSetAttributes.Volume> volumes = new ArrayList<>();
        for (Map.Entry<String, String> volumeIdWithDevice : volumeIdsWithDevice.entrySet()) {
            String volumeId = volumeIdWithDevice.getKey();
            String device = volumeIdWithDevice.getValue();
            volumes.add(new VolumeSetAttributes.Volume(volumeId, device, 1000, "gp2", CloudVolumeUsageType.GENERAL));
        }
        VolumeSetAttributes volumeSetAttr = new VolumeSetAttributes("az1", false, "", volumes, 1000, "gp2");
        volumeSetAttr.setDiscoveryFQDN(fqdn);
        resource.setId(id.addAndGet(1));
        resource.setAttributes(Json.silent(volumeSetAttr));
        resource.setInstanceId(instanceId);
        resource.setResourceType(resourceType);
        return resource;
    }

    private void mockAwsVolumesCommand(List<GatewayConfig> allGatewayConfigs, boolean oneVolumeMissingOnSecondNode, boolean oneNodeMissing)
            throws CloudbreakOrchestratorFailedException {
        String node1Volumes = """
            vol-05627c4c7d4f7106b nvme0n1
            vol-07e521a158c5ea7d8 nvme1n1
            vol-094b6ec5b940febda nvme2n1
            vol-05f368fd17337d9b1 nvme3n1
            vol-0aa314e189b706901 nvme4n1
            """;

        String node2Volumes = """
            vol-1234asd13421asdd1 nvme0n1
            vol-537634653asdazxcz nvme1n1
            vol-7654567asd123asd1 nvme2n1
            vol-6573543xcfasferq1 nvme3n1
            vol-234df4235sdf4423d nvme4n1
            """;

        String node2VolumesBut2Missing = """
            vol-1234asd13421asdd1 nvme0n1
            vol-537634653asdazxcz nvme1n1
            vol-7654567asd123asd1 nvme2n1
            """;

        Map<String, String> result = new HashMap<>();
        result.put("fqdn1", node1Volumes);
        if (!oneNodeMissing) {
            result.put("fqdn2", oneVolumeMissingOnSecondNode ? node2VolumesBut2Missing : node2Volumes);
        }
        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), FETCHER_SCRIPT)).thenReturn(result);
    }

    private void mockGcpVolumesCommand(List<GatewayConfig> allGatewayConfigs, boolean oneVolumeMissingOnSecondNode, boolean oneNodeMissing)
            throws CloudbreakOrchestratorFailedException {
        String node1Volumes = """
            perdosdhgcp-w-1-0-1737381500806 sdb
            perdosdhgcp-w-1-1-1737381500806 sdc
            perdosdhgcp-w-1-2-1737381500806 sdd
            perdosdhgcp-w-1-3-1737381500806 sde
            perdosdhgcp-w-1-1737382564438 sda
            """;

        String node2Volumes = """
            perdosdhgcp-w-2-0-1737381500806 sdb
            perdosdhgcp-w-2-1-1737381500806 sdc
            perdosdhgcp-w-2-2-1737381500806 sdd
            perdosdhgcp-w-2-3-1737381500806 sde
            perdosdhgcp-w-2-1737382564438 sda
            """;

        String node2VolumesBut2Missing = """
            perdosdhgcp-w-2-0-1737381500806 sdb
            perdosdhgcp-w-2-1-1737381500806 sdc
            perdosdhgcp-w-2-1737382564438 sda
            """;

        Map<String, String> result = new HashMap<>();
        result.put("fqdn1", node1Volumes);
        if (!oneNodeMissing) {
            result.put("fqdn2", oneVolumeMissingOnSecondNode ? node2VolumesBut2Missing : node2Volumes);
        }
        when(hostOrchestrator.runCommandOnHosts(allGatewayConfigs, Set.of("fqdn1", "fqdn2"), FETCHER_SCRIPT)).thenReturn(result);
    }

}