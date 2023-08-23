package com.sequenceiq.cloudbreak.service.datalake;

import static com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.DiskResizeEvent.DISK_RESIZE_TRIGGER_EVENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.Authenticator;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.aws.common.connector.resource.AwsResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.disk.resize.request.DiskResizeRequest;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.domain.Template;
import com.sequenceiq.cloudbreak.domain.VolumeTemplate;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.service.template.TemplateService;
import com.sequenceiq.cloudbreak.util.StackUtil;
import com.sequenceiq.cloudbreak.view.InstanceGroupView;
import com.sequenceiq.cloudbreak.workspace.model.Workspace;

@ExtendWith(MockitoExtension.class)
public class DiskUpdateServiceTest {

    private static final long STACK_ID = 1L;

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private StackDtoService stackDtoService;

    @Mock
    private CloudParameterCache cloudParameterCache;

    @Mock
    private StackService stackService;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ReactorNotifier reactorNotifier;

    @Mock
    private TemplateService templateService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @InjectMocks
    private DiskUpdateService underTest;

    @Test
    void testIsDiskTypeChangeSupported() {
        String platform = "AWS";
        doReturn(true).when(cloudParameterCache).isDiskTypeChangeSupported(platform);
        boolean diskTypeSupported = underTest.isDiskTypeChangeSupported(platform);
        assertTrue(diskTypeSupported);
        verify(cloudParameterCache, times(1)).isDiskTypeChangeSupported(platform);
    }

    @Test
    void testIsDiskTypeChangeNotSupported() {
        String platform = "AWS";
        doReturn(false).when(cloudParameterCache).isDiskTypeChangeSupported(platform);
        boolean diskTypeSupported = underTest.isDiskTypeChangeSupported(platform);
        assertFalse(diskTypeSupported);
        verify(cloudParameterCache, times(1)).isDiskTypeChangeSupported(platform);
    }

    @Test
    void testStopCMServices() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        doReturn(stackDto).when(stackDtoService).getById(STACK_ID);
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stackDto);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        underTest.stopCMServices(STACK_ID);
    }

    @Test
    void testStopCMServicesException() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        doReturn(stackDto).when(stackDtoService).getById(STACK_ID);
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stackDto);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        CloudbreakException exception = new CloudbreakException("TEST_EXCEPTION");
        doThrow(exception).when(clusterModificationService).stopCluster(true);
        Exception returnException = assertThrows(Exception.class, () -> underTest.stopCMServices(STACK_ID));
        assertEquals("TEST_EXCEPTION", returnException.getMessage());
    }

    @Test
    void testStartCMServices() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        doReturn(stackDto).when(stackDtoService).getById(STACK_ID);
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stackDto);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        underTest.startCMServices(STACK_ID);
    }

    @Test
    void testStartCMServicesException() throws Exception {
        StackDto stackDto = mock(StackDto.class);
        ClusterApi clusterApi = mock(ClusterApi.class);
        doReturn(stackDto).when(stackDtoService).getById(STACK_ID);
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(stackDto);
        ClusterModificationService clusterModificationService = mock(ClusterModificationService.class);
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        CloudbreakException exception = new CloudbreakException("TEST_EXCEPTION");
        doThrow(exception).when(clusterModificationService).startCluster();
        Exception returnException = assertThrows(Exception.class, () -> underTest.startCMServices(STACK_ID));
        assertEquals("TEST_EXCEPTION", returnException.getMessage());
    }

    @Test
    void testResizeDisks() throws Exception {
        Stack stack = mock(Stack.class);
        doReturn(stack).when(stackService).getByIdWithListsInTransaction(STACK_ID);
        underTest.resizeDisks(STACK_ID, "test");

        ArgumentCaptor<DiskResizeRequest> captor = ArgumentCaptor.forClass(DiskResizeRequest.class);
        verify(reactorNotifier).notify(any(), any(), captor.capture());
        assertEquals("test", captor.getValue().getInstanceGroup());
        assertEquals(DISK_RESIZE_TRIGGER_EVENT.selector(), captor.getValue().getSelector());
        assertEquals(STACK_ID, captor.getValue().getResourceId());
    }

    @Test
    void testUpdateDiskTypeAndSize() throws Exception {
        String instanceGroup = "master";
        Long stackId = 1L;
        CloudConnector cloudConnector = mock(CloudConnector.class);
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        AwsResourceVolumeConnector awsResourceVolumeConnector = mock(AwsResourceVolumeConnector.class);
        doReturn(awsResourceVolumeConnector).when(cloudConnector).volumeConnector();
        StackDto stack = mock(StackDto.class);
        doReturn("AWS").when(stack).getPlatformVariant();
        doReturn("AWS").when(stack).getCloudPlatform();
        Workspace workspace = mock(Workspace.class);
        doReturn(workspace).when(stack).getWorkspace();
        doReturn(stackId).when(workspace).getId();
        doReturn("crn:cdp:iam:us-west-1:someworkspace:user:someuser").when(stack).getResourceCrn();
        Authenticator authenticator  = mock(Authenticator.class);
        doReturn(authenticator).when(cloudConnector).authentication();
        doReturn(stack).when(stackDtoService).getById(stackId);

        InstanceGroupView instanceGroupView = mock(InstanceGroupView.class);
        Template template = mock(Template.class);
        doReturn(template).when(instanceGroupView).getTemplate();
        VolumeTemplate volumeTemplate = new VolumeTemplate();
        doReturn(Set.of(volumeTemplate)).when(template).getVolumeTemplates();
        doReturn(Optional.of(instanceGroupView)).when(instanceGroupService).findInstanceGroupViewByStackIdAndGroupName(stackId, instanceGroup);
        DiskUpdateRequest diskUpdateRequest = new DiskUpdateRequest();
        diskUpdateRequest.setSize(200);
        diskUpdateRequest.setVolumeType("test");
        diskUpdateRequest.setGroup("master");
        Volume volume = mock(Volume.class);
        doReturn("vol-1").when(volume).getId();
        underTest.updateDiskTypeAndSize(diskUpdateRequest, List.of(volume), stackId);

        verify(templateService).savePure(template);
        verify(awsResourceVolumeConnector).updateDiskVolumes(any(), eq(List.of("vol-1")), eq("test"), eq(200));
        verify(resourceService).saveAll(any());
        VolumeTemplate resultVolumeTemplate = template.getVolumeTemplates().stream().findFirst().get();
        assertEquals(200, resultVolumeTemplate.getVolumeSize());
        assertEquals("test", resultVolumeTemplate.getVolumeType());
    }
}
