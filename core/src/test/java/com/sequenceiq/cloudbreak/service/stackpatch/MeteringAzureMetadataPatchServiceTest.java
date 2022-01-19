package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.StackType;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
import com.sequenceiq.cloudbreak.auth.security.internal.InternalCrnModifier;
import com.sequenceiq.cloudbreak.cloud.model.Image;
import com.sequenceiq.cloudbreak.cluster.service.ClusterComponentConfigProvider;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterBootstrapper;
import com.sequenceiq.cloudbreak.domain.ImageCatalog;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceGroup;
import com.sequenceiq.cloudbreak.domain.stack.instance.InstanceMetaData;
import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorFailedException;
import com.sequenceiq.cloudbreak.orchestrator.host.TelemetryOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.model.Node;
import com.sequenceiq.cloudbreak.service.GatewayConfigService;
import com.sequenceiq.cloudbreak.service.image.ImageCatalogService;
import com.sequenceiq.cloudbreak.service.image.StatedImage;
import com.sequenceiq.cloudbreak.service.stack.InstanceMetaDataService;
import com.sequenceiq.cloudbreak.service.stack.StackImageService;
import com.sequenceiq.cloudbreak.util.CompressUtil;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class MeteringAzureMetadataPatchServiceTest {

    private static final String CUSTOM_RPM_URL_SAMPLE =
            "https://archive.cloudera.com/cp_clients/thunderhead-metering-heartbeat-application-0.1-SNAPSHOT.x86_64.rpm";

    private static final String DATE_SAMPLE = "2022-01-02";

    @InjectMocks
    private MeteringAzureMetadataPatchService underTest;

    @Mock
    private CompressUtil compressUtil;

    @Mock
    private TelemetryOrchestrator telemetryOrchestrator;

    @Mock
    private StackImageService stackImageService;

    @Mock
    private InstanceMetaDataService instanceMetaDataService;

    @Mock
    private GatewayConfigService gatewayConfigService;

    @Mock
    private ClusterComponentConfigProvider clusterComponentConfigProvider;

    @Mock
    private ClusterBootstrapper clusterBootstrapper;

    @Mock
    private ImageCatalogService imageCatalogService;

    @Mock
    private InternalCrnModifier internalCrnModifier;

    @BeforeEach
    public void setUp() {
        underTest = new MeteringAzureMetadataPatchService(DATE_SAMPLE, CUSTOM_RPM_URL_SAMPLE);
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testIsAffected() throws Exception {
        // GIVEN
        Image imageMock = mock(Image.class);
        ImageCatalog imageCatalogMock = mock(ImageCatalog.class);
        StatedImage statedImageMock = mock(StatedImage.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image imageFromStated = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        given(internalCrnModifier.getInternalCrnWithAccountId(anyString())).willReturn("accountId");
        given(stackImageService.getCurrentImage(any())).willReturn(imageMock);
        given(imageMock.getImageCatalogName()).willReturn("myCatalog");
        given(imageCatalogService.getImageCatalogByName(anyLong(), anyString())).willReturn(imageCatalogMock);
        given(imageCatalogService.getImageByCatalogName(anyLong(), isNull(), isNull())).willReturn(statedImageMock);
        given(statedImageMock.getImage()).willReturn(imageFromStated);
        long sampleTimestamp = underTest.dateStringToTimestampForImage("2021-12-24");
        given(imageFromStated.getCreated()).willReturn(sampleTimestamp);
        // WHEN
        boolean result = underTest.isAffected(createStack());
        // THEN
        assertTrue(result);
    }

    @Test
    public void testIsNotAffectedByDate() throws Exception {
        // GIVEN
        Image imageMock = mock(Image.class);
        ImageCatalog imageCatalogMock = mock(ImageCatalog.class);
        StatedImage statedImageMock = mock(StatedImage.class);
        com.sequenceiq.cloudbreak.cloud.model.catalog.Image imageFromStated = mock(com.sequenceiq.cloudbreak.cloud.model.catalog.Image.class);
        given(internalCrnModifier.getInternalCrnWithAccountId(anyString())).willReturn("accountId");
        given(stackImageService.getCurrentImage(any())).willReturn(imageMock);
        given(imageMock.getImageCatalogName()).willReturn("myCatalog");
        given(imageCatalogService.getImageCatalogByName(anyLong(), anyString())).willReturn(imageCatalogMock);
        given(imageCatalogService.getImageByCatalogName(anyLong(), isNull(), isNull())).willReturn(statedImageMock);
        given(statedImageMock.getImage()).willReturn(imageFromStated);
        long sampleTimestamp = underTest.dateStringToTimestampForImage("2022-02-02");
        given(imageFromStated.getCreated()).willReturn(sampleTimestamp);
        // WHEN
        boolean result = underTest.isAffected(createStack());
        // THEN
        assertFalse(result);
    }

    @Test
    public void testIsNotAffectedByCloudPlatform() {
        // GIVEN
        Stack stack = createStack();
        stack.setCloudPlatform(CloudPlatform.AWS.name());
        // WHEN
        boolean result = underTest.isAffected(stack);
        // THEN
        assertFalse(result);
    }

    @Test
    public void testDoApply() throws ExistingStackPatchApplyException, IOException {
        // GIVEN
        byte[] currentSaltState = "[current_state]".getBytes(StandardCharsets.UTF_8);
        byte[] meteringConfig = "[metering_state]".getBytes(StandardCharsets.UTF_8);
        byte[] updatedState = "[updated_state]".getBytes(StandardCharsets.UTF_8);
        InstanceMetaData instanceMetaData = createInstanceMetaData();
        InstanceGroup instanceGroup = createInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        given(instanceMetaDataService.findNotTerminatedForStack(anyLong())).willReturn(instanceMetaDataSet);
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(currentSaltState);
        given(compressUtil.generateCompressedOutputFromFolders(any(), any())).willReturn(meteringConfig);
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
        byte[] meteringConfig = "[metering_state]".getBytes(StandardCharsets.UTF_8);
        InstanceMetaData instanceMetaData = createInstanceMetaData();
        InstanceGroup instanceGroup = createInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        given(instanceMetaDataService.findNotTerminatedForStack(anyLong())).willReturn(instanceMetaDataSet);
        given(telemetryOrchestrator.collectUnresponsiveNodes(any(), any(), any())).willReturn(
                Set.of(new Node(null, null, null, null)));
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(currentSaltState);
        given(compressUtil.generateCompressedOutputFromFolders(any(), any())).willReturn(meteringConfig);
        given(compressUtil.compareCompressedContent(any(), any(), any())).willReturn(false);
        // WHEN
        ExistingStackPatchApplyException exception = assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(createStack()));
        // THEN
        assertTrue(exception.getMessage().contains("Not found any available nodes"));

    }

    @Test
    public void testDoApplyWithMatchingStates() throws ExistingStackPatchApplyException, IOException {
        // GIVEN
        byte[] currentSaltState = "[current_state]".getBytes(StandardCharsets.UTF_8);
        byte[] meteringConfig = "[metering_state]".getBytes(StandardCharsets.UTF_8);
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(currentSaltState);
        given(compressUtil.generateCompressedOutputFromFolders(any(), any())).willReturn(meteringConfig);
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
        byte[] meteringConfig = "[metering_state]".getBytes(StandardCharsets.UTF_8);
        InstanceMetaData instanceMetaData = createInstanceMetaData();
        InstanceGroup instanceGroup = createInstanceGroup();
        instanceMetaData.setInstanceGroup(instanceGroup);
        Set<InstanceMetaData> instanceMetaDataSet = Set.of(instanceMetaData);
        given(instanceMetaDataService.findNotTerminatedForStack(anyLong())).willReturn(instanceMetaDataSet);
        given(clusterComponentConfigProvider.getSaltStateComponent(anyLong())).willReturn(currentSaltState);
        given(compressUtil.generateCompressedOutputFromFolders(any(), any())).willReturn(meteringConfig);
        given(compressUtil.compareCompressedContent(any(), any(), any())).willReturn(false);
        doThrow(new CloudbreakOrchestratorFailedException("salt error")).when(telemetryOrchestrator)
                .upgradeMetering(any(), any(), any(), any(), any());
        // WHEN
        ExistingStackPatchApplyException exception = assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(createStack()));
        // THEN
        assertTrue(exception.getMessage().contains("salt error"));
    }

    private Stack createStack() {
        Stack stack = new Stack();
        stack.setId(1L);
        stack.setType(StackType.WORKLOAD);
        stack.setCloudPlatform(CloudPlatform.AZURE.name());
        stack.setResourceCrn("crn:cdp:datahub:us-west-1:accountId:cluster:name");
        Cluster cluster = new Cluster();
        cluster.setId(1L);
        Workspace workspace = new Workspace();
        workspace.setId(1L);
        stack.setWorkspace(workspace);
        stack.setCluster(cluster);
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
        Template template = new Template();
        template.setInstanceType("myInstanceType");
        instanceGroup.setTemplate(template);
        return instanceGroup;
    }

    private InstanceMetaData createInstanceMetaData() {
        InstanceMetaData instanceMetaData = new InstanceMetaData();
        instanceMetaData.setInstanceStatus(InstanceStatus.SERVICES_RUNNING);
        instanceMetaData.setClusterManagerServer(true);
        return instanceMetaData;
    }
}
