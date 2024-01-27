package com.sequenceiq.cloudbreak.cloud.azure.resource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;
import com.sequenceiq.cloudbreak.cloud.model.CloudResource;

@ExtendWith(MockitoExtension.class)
class AzureResourceVolumeConnectorTest {

    @Mock
    private AzureVolumeResourceBuilder azureVolumeResourceBuilder;

    @InjectMocks
    private AzureResourceVolumeConnector underTest;

    @Mock
    private AuthenticatedContext authenticatedContext;

    @Test
    void testUpdateDiskVolumes() {
        List<String> volumeIds = List.of("test-vol-1");
        underTest.updateDiskVolumes(authenticatedContext, volumeIds, "test", 100);
        verify(azureVolumeResourceBuilder).modifyVolumes(authenticatedContext, volumeIds, "test", 100);
    }

    @Test
    void testUpdateDiskVolumesFail() {
        List<String> volumeIds = List.of("test-vol-1");
        doThrow(new RuntimeException("TEST")).when(azureVolumeResourceBuilder).modifyVolumes(authenticatedContext, volumeIds, "test", 100);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> underTest.updateDiskVolumes(authenticatedContext, volumeIds, "test", 100));
        assertEquals("TEST", exception.getMessage());
        verify(azureVolumeResourceBuilder).modifyVolumes(authenticatedContext, volumeIds, "test", 100);
    }

    @Test
    void testDetachVolumes() throws Exception {
        List<CloudResource> resources = List.of();
        underTest.detachVolumes(authenticatedContext, resources);
        verify(azureVolumeResourceBuilder).detachVolumes(authenticatedContext, resources);
    }

    @Test
    void testDetachVolumesFail() {
        List<CloudResource> resources = List.of();
        doThrow(new RuntimeException("TEST")).when(azureVolumeResourceBuilder).detachVolumes(authenticatedContext, resources);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> underTest.detachVolumes(authenticatedContext, resources));
        assertEquals("TEST", exception.getMessage());
        verify(azureVolumeResourceBuilder).detachVolumes(authenticatedContext, resources);
    }

    @Test
    void testDeleteVolumes() throws Exception {
        List<CloudResource> resources = List.of();
        underTest.deleteVolumes(authenticatedContext, resources);
        verify(azureVolumeResourceBuilder).deleteVolumes(authenticatedContext, resources);
    }

    @Test
    void testDeleteVolumesFail() {
        List<CloudResource> resources = List.of();
        doThrow(new RuntimeException("TEST")).when(azureVolumeResourceBuilder).deleteVolumes(authenticatedContext, resources);
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> underTest.deleteVolumes(authenticatedContext, resources));
        assertEquals("TEST", exception.getMessage());
        verify(azureVolumeResourceBuilder).deleteVolumes(authenticatedContext, resources);
    }
}
