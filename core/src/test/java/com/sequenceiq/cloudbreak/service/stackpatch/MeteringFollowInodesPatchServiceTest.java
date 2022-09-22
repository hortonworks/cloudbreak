package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceMetadataType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.util.CompressUtil;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class MeteringFollowInodesPatchServiceTest {

    @InjectMocks
    private MeteringFollowInodesPatchService underTest;

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

    @BeforeEach
    public void setUp() {
        underTest = new MeteringFollowInodesPatchService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testIsAffected() throws Exception {
        // GIVEN
        Stack stack = createStack();
        Image image = mock(Image.class);
        given(stackImageService.getCurrentImage(stack.getId())).willReturn(image);
        given(image.getPackageVersions()).willReturn(Map.of("cdp-logging-agent", "0.2.15"));
        // WHEN
        boolean result = underTest.isAffected(stack);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsNotAffected() throws Exception {
        // GIVEN
        Stack stack = createStack();
        Image image = mock(Image.class);
        given(stackImageService.getCurrentImage(stack.getId())).willReturn(image);
        given(image.getPackageVersions()).willReturn(Map.of());
        // WHEN
        boolean result = underTest.isAffected(stack);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testDoApply() throws Exception {
        // GIVEN
        byte[] currentSaltState = "[current_state]".getBytes(StandardCharsets.UTF_8);
        byte[] fluentConfig = "[fluent_state]".getBytes(StandardCharsets.UTF_8);
        byte[] updatedState = "[updated_state]".getBytes(StandardCharsets.UTF_8);
        InstanceMetaData instanceMetaData = createInstanceMetaData();
        InstanceGroup instanceGroup = createInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        given(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(anyLong())).willReturn(instanceMetaDataSet);
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(currentSaltState);
        given(compressUtil.generateCompressedOutputFromFolders(any(), any())).willReturn(fluentConfig);
        given(compressUtil.compareCompressedContent(any(), any(), any())).willReturn(false);
        given(compressUtil.updateCompressedOutputFolders(any(), any(), any())).willReturn(updatedState);
        // WHEN
        boolean result = underTest.doApply(createStack());
        // THEN
        assertTrue(result);
        verify(compressUtil, times(1)).updateCompressedOutputFolders(any(), any(), any());
        verify(gatewayConfigService, times(1)).getAllGatewayConfigs(any());
        verify(telemetryOrchestrator, times(1)).updateAndRestartTelemetryService(any(), any(), any(), any(), any(), any());
        verify(clusterBootstrapper, times(1)).updateSaltComponent(any(), any());
    }

    @Test
    public void testDoApplyOnStoppedStack() throws Exception {
        // GIVEN
        Stack stack = createStack();
        StackStatus stackStatus = new StackStatus();
        stackStatus.setStatus(Status.STOPPED);
        stack.setStackStatus(stackStatus);
        // WHEN
        boolean result = underTest.doApply(stack);
        // THEN
        assertFalse(result);
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
