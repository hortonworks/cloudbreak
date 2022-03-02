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
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.service.stackpatch.config.LoggingAgentAutoRestartPatchConfig;
import com.sequenceiq.cloudbreak.util.CompressUtil;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;
import com.sequenceiq.common.api.type.InstanceGroupType;

@ExtendWith(MockitoExtension.class)
public class LoggingAgentAutoRestartPatchServiceTest {

    @InjectMocks
    private LoggingAgentAutoRestartPatchService underTest;

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
    private ImageCatalogService imageCatalogService;

    @Mock
    private LoggingAgentAutoRestartPatchConfig loggingAgentAutoRestartPatchConfig;

    @BeforeEach
    public void setUp() {
        underTest = new LoggingAgentAutoRestartPatchService();
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testIsAffected() throws CloudbreakImageNotFoundException {
        // GIVEN
        Stack stack = createStack();
        Image image = mock(Image.class);
        given(stackImageService.getCurrentImage(stack)).willReturn(image);
        given(image.getPackageVersions()).willReturn(Map.of("cdp-logging-agent", "0.2.11"));
        initIsAffectedMocks();
        // WHEN
        underTest.init();
        boolean result = underTest.isAffected(stack);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsAffectedIfVersionEquals() throws CloudbreakImageNotFoundException {
        // GIVEN
        Stack stack = createStack();
        Image image = mock(Image.class);
        given(stackImageService.getCurrentImage(stack)).willReturn(image);
        given(image.getPackageVersions()).willReturn(Map.of("cdp-logging-agent", "0.2.13"));
        initIsAffectedMocks();
        // WHEN
        underTest.init();
        boolean result = underTest.isAffected(stack);
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsNotAffected() throws CloudbreakImageNotFoundException {
        // GIVEN
        Stack stack = createStack();
        Image image = mock(Image.class);
        given(stackImageService.getCurrentImage(stack)).willReturn(image);
        given(image.getPackageVersions()).willReturn(Map.of("cdp-logging-agent", "0.2.15"));
        initIsAffectedMocks();
        // WHEN
        underTest.init();
        boolean result = underTest.isAffected(stack);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsAffectedByDate() throws Exception {
        // GIVEN
        Stack stack = createStack();
        Image image = mock(Image.class);
        ImageCatalog imageCatalog = mock(ImageCatalog.class);
        StatedImage statedImageMock = mock(StatedImage.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image imageFromStated = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        given(image.getPackageVersions()).willReturn(new HashMap<>());
        given(stackImageService.getCurrentImage(stack)).willReturn(image);
        given(stackImageService.getImageCatalogFromStackAndImage(any(), any())).willReturn(imageCatalog);
        given(stackImageService.getStatedImageInternal(any(Stack.class), any(Image.class), any(ImageCatalog.class))).willReturn(Optional.of(statedImageMock));
        given(statedImageMock.getImage()).willReturn(imageFromStated);
        long sampleTimestamp = underTest.dateStringToTimestampForImage("2022-01-10");
        given(imageFromStated.getCreated()).willReturn(sampleTimestamp);
        initIsAffectedMocks();
        // WHEN
        underTest.init();
        boolean result = underTest.isAffected(stack);
        // THEN
        assertTrue(result);
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
        given(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(anyLong())).willReturn(instanceMetaDataSet);
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
        given(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(anyLong())).willReturn(instanceMetaDataSet);
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
        given(instanceMetaDataService.findNotTerminatedAndNotZombieForStack(anyLong())).willReturn(instanceMetaDataSet);
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(currentSaltState);
        given(compressUtil.generateCompressedOutputFromFolders(any(), any())).willReturn(fluentConfig);
        given(compressUtil.compareCompressedContent(any(), any(), any())).willReturn(false);
        doThrow(new CloudbreakOrchestratorFailedException("salt error")).when(telemetryOrchestrator)
                .executeLoggingAgentDiagnostics(any(), any(), any(), any());
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
        stack.setResourceCrn("crn:cdp:datahub:us-west-1:accountId:cluster:name");
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

    private void initIsAffectedMocks() {
        given(loggingAgentAutoRestartPatchConfig.getAffectedVersionFrom()).willReturn("0.2.13");
        given(loggingAgentAutoRestartPatchConfig.getDateAfter()).willReturn("2022-01-01");
        given(loggingAgentAutoRestartPatchConfig.getDateBefore()).willReturn("2022-02-01");
    }
}
