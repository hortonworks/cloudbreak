package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.CloudbreakImageNotFoundException;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.GatewayConfig;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stackpatch.config.DisableRegionForFluentdPatchConfig;
import com.sequenceiq.cloudbreak.util.CompressUtil;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class DisableRegionForFluentdPatchServiceTest {

    @InjectMocks
    private DisableRegionForFluentdPatchService underTest;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private CompressUtil compressUtil;

    @Mock
    private TelemetryOrchestrator telemetryOrchestrator;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private DisableRegionForFluentdPatchConfig disableRegionForFluentdPatchConfig;

    @BeforeEach
    public void setUp() {
        underTest = new DisableRegionForFluentdPatchService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testIsAffected() throws CloudbreakImageNotFoundException {
        // GIVEN
        Stack stack = createStack();
        Image image = mock(Image.class);
        given(stackImageService.getCurrentImage(stack.getId())).willReturn(image);
        given(image.getPackageVersions()).willReturn(Map.of("cdp-logging-agent", "0.2.15"));
        given(disableRegionForFluentdPatchConfig.getVersionModelFromAffectedVersion()).willReturn(ModuleDescriptor.Version.parse("0.2.16"));
        // WHEN
        boolean result = underTest.isAffected(stack);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsAffectedEquals() throws CloudbreakImageNotFoundException {
        // GIVEN
        Stack stack = createStack();
        Image image = mock(Image.class);
        given(stackImageService.getCurrentImage(stack.getId())).willReturn(image);
        given(image.getPackageVersions()).willReturn(Map.of("cdp-logging-agent", "0.2.16"));
        given(disableRegionForFluentdPatchConfig.getVersionModelFromAffectedVersion()).willReturn(ModuleDescriptor.Version.parse("0.2.16"));
        // WHEN
        boolean result = underTest.isAffected(stack);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsNotAffected() throws CloudbreakImageNotFoundException {
        // GIVEN
        Stack stack = createStack();
        Image image = mock(Image.class);
        given(stackImageService.getCurrentImage(stack.getId())).willReturn(image);
        given(image.getPackageVersions()).willReturn(Map.of("cdp-logging-agent", "0.2.17"));
        given(disableRegionForFluentdPatchConfig.getVersionModelFromAffectedVersion()).willReturn(ModuleDescriptor.Version.parse("0.2.16"));
        // WHEN
        boolean result = underTest.isAffected(stack);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testDoApply() throws ExistingStackPatchApplyException, IOException {
        // GIVEN
        byte[] currentSaltState = "[current_state]".getBytes(StandardCharsets.UTF_8);
        byte[] fluentConfig = "[fluent_state]".getBytes(StandardCharsets.UTF_8);
        byte[] updatedState = "[updated_state]".getBytes(StandardCharsets.UTF_8);
        InstanceMetaData instanceMetaData = createInstanceMetaData();
        InstanceGroup instanceGroup = createInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        given(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(anyLong())).willReturn(instanceMetaDataSet);
        given(gatewayConfigService.getPrimaryGatewayConfig(any(Stack.class))).willReturn(gatewayConfig);
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(currentSaltState);
        given(compressUtil.generateCompressedOutputFromFolders(any(), any())).willReturn(fluentConfig);
        given(compressUtil.compareCompressedContent(any(), any(), any())).willReturn(false);
        given(compressUtil.updateCompressedOutputFolders(any(), any(), any())).willReturn(updatedState);
        // WHEN
        underTest.doApply(createStack());
        // THEN
        verify(compressUtil, times(1)).updateCompressedOutputFolders(any(), any(), any());
    }

    @Test
    public void testDoApplyWithoutResponsiveNodes() throws ExistingStackPatchApplyException, IOException, CloudbreakOrchestratorFailedException {
        // GIVEN
        byte[] currentSaltState = "[current_state]".getBytes(StandardCharsets.UTF_8);
        byte[] fluentConfig = "[fluent_state]".getBytes(StandardCharsets.UTF_8);
        InstanceMetaData instanceMetaData = createInstanceMetaData();
        InstanceGroup instanceGroup = createInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        given(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(anyLong())).willReturn(instanceMetaDataSet);
        given(gatewayConfigService.getPrimaryGatewayConfig(any(Stack.class))).willReturn(gatewayConfig);
        given(telemetryOrchestrator.collectUnresponsiveNodes(any(), any(), any())).willReturn(Set.of(new Node(null, null, null, null)));
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(currentSaltState);
        given(compressUtil.generateCompressedOutputFromFolders(any(), any())).willReturn(fluentConfig);
        given(compressUtil.compareCompressedContent(any(), any(), any())).willReturn(false);
        // WHEN
        boolean result = underTest.doApply(createStack());
        // THEN
        assertFalse(result);
    }

    @Test
    public void testDoApplyWithMatchingStates() throws ExistingStackPatchApplyException, IOException {
        // GIVEN
        byte[] currentSaltState = "[current_state]".getBytes(StandardCharsets.UTF_8);
        byte[] fluentConfig = "[fluent_state]".getBytes(StandardCharsets.UTF_8);
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(currentSaltState);
        given(compressUtil.generateCompressedOutputFromFolders(any(), any())).willReturn(fluentConfig);
        given(compressUtil.compareCompressedContent(any(), any(), any())).willReturn(true);
        // WHEN
        underTest.doApply(createStack());
        // THEN
        verify(compressUtil, times(1)).compareCompressedContent(any(), any(), any());
        verify(compressUtil, times(0)).updateCompressedOutputFolders(any(), any(), any());
    }

    @Test
    public void testDoApplyWithoutSaltState() {
        // GIVEN
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(null);
        // WHEN
        ExistingStackPatchApplyException exception = assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(createStack()));
        // THEN
        assertTrue(exception.getMessage().contains("Salt state is empty for stack"));
    }

    @Test
    public void testDoApplyWithSaltOrchestratorError() throws IOException, CloudbreakOrchestratorFailedException {
        // GIVEN
        byte[] currentSaltState = "[current_state]".getBytes(StandardCharsets.UTF_8);
        byte[] fluentConfig = "[fluent_state]".getBytes(StandardCharsets.UTF_8);
        InstanceMetaData instanceMetaData = createInstanceMetaData();
        InstanceGroup instanceGroup = createInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        GatewayConfig gatewayConfig = mock(GatewayConfig.class);
        given(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(anyLong())).willReturn(instanceMetaDataSet);
        given(gatewayConfigService.getPrimaryGatewayConfig(any(Stack.class))).willReturn(gatewayConfig);
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(currentSaltState);
        given(compressUtil.generateCompressedOutputFromFolders(any(), any())).willReturn(fluentConfig);
        given(compressUtil.compareCompressedContent(any(), any(), any())).willReturn(false);
        doThrow(new CloudbreakOrchestratorFailedException("salt error")).when(telemetryOrchestrator)
                .updateAndRestartTelemetryService(any(), any(), any(), any(), any(), any());
        // WHEN
        ExistingStackPatchApplyException exception = assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(createStack()));
        // THEN
        assertTrue(exception.getMessage().contains("salt error"));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setType(StackType.WORKLOAD);
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        stack.setWorkspace(workspace);
        stack.setCluster(cluster);
        stack.setResourceCrn("crn:cdp:datahub:eu-1:accountId:cluster:name");
        InstanceGroup instanceGroup = createInstanceGroup();
        instanceGroup.setInstanceMetaData(Set.of(createInstanceMetaData()));
        stack.setInstanceGroups(Set.of(instanceGroup));
        return stack;
    }

    private InstanceGroup createInstanceGroup() {
        InstanceMetaData instanceMetaData = createInstanceMetaData();
        InstanceGroup instanceGroup = new InstanceGroup();
        instanceGroup.setGroupName("master");
        instanceGroup.setInstanceMetaData(Set.of(instanceMetaData));
        instanceGroup.setInstanceGroupType(InstanceGroupType.GATEWAY);
        Template template = new Template();
        template.setInstanceType("myInstanceType");
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }

    private InstanceMetaData createInstanceMetaData() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        instanceMetaData.setInstanceMetadataType(InstanceMetadataType.GATEWAY_PRIMARY);
        return instanceMetaData;
    }
}
