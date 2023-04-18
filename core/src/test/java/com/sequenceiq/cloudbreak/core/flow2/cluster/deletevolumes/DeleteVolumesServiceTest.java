package com.sequenceiq.cloudbreak.core.flow2.cluster.deletevolumes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.StackDeleteVolumesRequest;
import com.sequenceiq.cloudbreak.cloud.CloudConnector;
import com.sequenceiq.cloudbreak.cloud.ResourceVolumeConnector;
import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.model.CloudPlatformVariant;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.core.flow2.cluster.ClouderaManagerPollingUtilService;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.reactor.api.event.resource.DeleteVolumesHandlerRequest;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.InstanceGroupService;
import com.sequenceiq.cloudbreak.template.model.ServiceComponent;
import com.sequenceiq.cloudbreak.util.StackUtil;

@ExtendWith(MockitoExtension.class)
public class DeleteVolumesServiceTest {

    @Mock
    private CloudPlatformConnectors cloudPlatformConnectors;

    @Mock
    private StackUtil stackUtil;

    @Mock
    private ResourceService resourceService;

    @Mock
    private ClusterApiConnectors clusterApiConnectors;

    @Mock
    private ClouderaManagerPollingUtilService clouderaManagerPollingUtilService;

    @Mock
    private InstanceGroupService instanceGroupService;

    @InjectMocks
    private DeleteVolumesService underTest;

    @Mock
    private StackDto stackDto;

    @Mock
    private CloudResource cloudResource;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Mock
    private ResourceVolumeConnector resourceVolumeConnector;

    @Mock
    private CloudConnector cloudConnector;

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterModificationService clusterModificationService;

    @Mock
    private CloudPlatformVariant cloudPlatformVariant;

    @Test
    public void detachResourcesSuccess() throws Exception {
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(resourceVolumeConnector).when(cloudConnector).volumeConnector();
        List<CloudResource> resources = List.of(cloudResource);
        underTest.detachResources(resources, cloudPlatformVariant, authenticatedContext);
    }

    @Test
    public void detachResourcesFailure() throws Exception {
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(resourceVolumeConnector).when(cloudConnector).volumeConnector();
        List<CloudResource> resources = List.of(cloudResource);
        doThrow(new Exception("TEST")).when(resourceVolumeConnector).detachVolumes(any(), any());
        Exception exception = assertThrows(Exception.class, () -> underTest.detachResources(resources, cloudPlatformVariant, authenticatedContext));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    public void deleteResourcesSuccess() throws Exception {
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(resourceVolumeConnector).when(cloudConnector).volumeConnector();
        List<CloudResource> resources = List.of(cloudResource);
        underTest.deleteResources(resources, cloudPlatformVariant, authenticatedContext);
    }

    @Test
    public void deleteResourcesFailure() throws Exception {
        doReturn(cloudConnector).when(cloudPlatformConnectors).get(any());
        doReturn(resourceVolumeConnector).when(cloudConnector).volumeConnector();
        List<CloudResource> resources = List.of(cloudResource);
        doThrow(new Exception("TEST")).when(resourceVolumeConnector).deleteVolumes(any(), any());
        Exception exception = assertThrows(Exception.class, () -> underTest.deleteResources(resources, cloudPlatformVariant, authenticatedContext));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    public void updateCbdbResourcesSuccess() throws Exception {
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");
        DeleteVolumesHandlerRequest deleteVolumesRequest = new DeleteVolumesHandlerRequest(List.of(cloudResource), stackDeleteVolumesRequest,
                "MOCK", Set.of());
        doReturn(new HashSet<>()).when(stackDto).getResources();
        underTest.deleteVolumeResources(stackDto, deleteVolumesRequest);
    }

    @Test
    public void updateCbdbResourcesFailure() {
        StackDeleteVolumesRequest stackDeleteVolumesRequest = new StackDeleteVolumesRequest();
        stackDeleteVolumesRequest.setStackId(1L);
        stackDeleteVolumesRequest.setGroup("COMPUTE");
        DeleteVolumesHandlerRequest deleteVolumesRequest = new DeleteVolumesHandlerRequest(List.of(cloudResource), stackDeleteVolumesRequest,
                "MOCK", Set.of());
        doReturn(new HashSet<>()).when(stackDto).getResources();
        doThrow(new RuntimeException("TEST")).when(resourceService).deleteAll(anyList());
        Exception exception = assertThrows(Exception.class, () -> underTest.deleteVolumeResources(stackDto, deleteVolumesRequest));
        assertEquals("TEST", exception.getMessage());
    }

    @Test
    public void testStartClouderaManagerService() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDto.class));
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        doReturn(1L).when(stackDto).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);

        underTest.startClouderaManagerService(stackDto, hostTemplateServiceComponents);
        verify(clusterModificationService, times(1)).startClouderaManagerService("yarn");
    }

    @Test
    public void testStartClouderaManagerServiceException() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDto.class));
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        doReturn(1L).when(stackDto).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        doThrow(new Exception("Test")).when(clusterModificationService).startClouderaManagerService("yarn");

        Exception exception = assertThrows(Exception.class, () -> underTest.startClouderaManagerService(stackDto, hostTemplateServiceComponents));
        assertEquals("Unable to start CM services for service yarn, in stack 1: Test", exception.getMessage());
    }

    @Test
    public void testStopClouderaManagerService() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDto.class));
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        doReturn(1L).when(stackDto).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);

        underTest.stopClouderaManagerService(stackDto, hostTemplateServiceComponents);
        verify(clusterModificationService, times(1)).stopClouderaManagerService("yarn");
    }

    @Test
    public void testStopClouderaManagerServiceException() throws Exception {
        doReturn(clusterApi).when(clusterApiConnectors).getConnector(any(StackDto.class));
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
        doReturn(1L).when(stackDto).getId();
        Set<ServiceComponent> hostTemplateServiceComponents = new HashSet<>();
        ServiceComponent serviceComponent = ServiceComponent.of("yarn", "yarn");
        hostTemplateServiceComponents.add(serviceComponent);
        doThrow(new Exception("Test")).when(clusterModificationService).stopClouderaManagerService("yarn");

        Exception exception = assertThrows(Exception.class, () -> underTest.stopClouderaManagerService(stackDto, hostTemplateServiceComponents));
        assertEquals("Unable to stop CM services for service yarn, in stack 1: Test", exception.getMessage());
    }
}
