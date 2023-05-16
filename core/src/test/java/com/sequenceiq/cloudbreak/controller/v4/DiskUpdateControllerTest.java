package com.sequenceiq.cloudbreak.controller.v4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskModificationRequest;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskUpdateRequest;
import com.sequenceiq.cloudbreak.cloud.model.Volume;
import com.sequenceiq.cloudbreak.exception.CloudbreakApiException;
import com.sequenceiq.cloudbreak.service.datalake.DiskUpdateService;

@ExtendWith(MockitoExtension.class)
public class DiskUpdateControllerTest {

    @Mock
    private DiskUpdateService diskUpdateService;

    @InjectMocks
    private DiskUpdateController underTest;

    @Test
    void testIsDiskTypeChangeSupported() {
        doReturn(true).when(diskUpdateService).isDiskTypeChangeSupported(anyString());

        boolean diskTypeSupported = underTest.isDiskTypeChangeSupported("AWS");
        assertTrue(diskTypeSupported);
    }

    @Test
    void testUpdateDiskTypeAndSize() throws Exception {
        DiskModificationRequest diskModificationRequest = new DiskModificationRequest();
        diskModificationRequest.setDiskUpdateRequest(mock(DiskUpdateRequest.class));
        diskModificationRequest.setVolumesToUpdate(List.of(mock(Volume.class)));
        diskModificationRequest.setStackId(1L);
        underTest.updateDiskTypeAndSize(diskModificationRequest);
        verify(diskUpdateService, times(1)).updateDiskTypeAndSize(diskModificationRequest.getDiskUpdateRequest(),
                diskModificationRequest.getVolumesToUpdate(), diskModificationRequest.getStackId());
    }

    @Test
    void testUpdateDiskTypeAndSizeThrowsException() throws Exception {
        DiskModificationRequest diskModificationRequest = new DiskModificationRequest();
        diskModificationRequest.setDiskUpdateRequest(mock(DiskUpdateRequest.class));
        diskModificationRequest.setVolumesToUpdate(List.of(mock(Volume.class)));
        diskModificationRequest.setStackId(1L);
        doThrow(new Exception("TEST EXCEPTION")).when(diskUpdateService).updateDiskTypeAndSize(eq(diskModificationRequest.getDiskUpdateRequest()),
                eq(diskModificationRequest.getVolumesToUpdate()), eq(diskModificationRequest.getStackId()));
        Exception exception = assertThrows(Exception.class, () -> underTest.updateDiskTypeAndSize(diskModificationRequest));
        verify(diskUpdateService, times(1)).updateDiskTypeAndSize(diskModificationRequest.getDiskUpdateRequest(),
                diskModificationRequest.getVolumesToUpdate(), diskModificationRequest.getStackId());
        assertEquals("TEST EXCEPTION", exception.getMessage());
    }

    @Test
    void testStopCluster() throws Exception {
        Long stackId = 1L;
        underTest.stopCMServices(stackId);
        verify(diskUpdateService, times(1)).stopCMServices(stackId);
    }

    @Test
    void testStopClusterThrowsException() throws Exception {
        Long stackId = 1L;
        doThrow(new Exception("TEST EXCEPTION")).when(diskUpdateService).stopCMServices(eq(stackId));
        Exception exception = assertThrows(Exception.class, () -> underTest.stopCMServices(stackId));
        verify(diskUpdateService, times(1)).stopCMServices(stackId);
        assertEquals("TEST EXCEPTION", exception.getMessage());
    }

    @Test
    void testStartCluster() throws Exception {
        Long stackId = 1L;
        underTest.startCMServices(stackId);
        verify(diskUpdateService, times(1)).startCMServices(stackId);
    }

    @Test
    void testStartClusterThrowsException() throws Exception {
        Long stackId = 1L;
        doThrow(new Exception("TEST EXCEPTION")).when(diskUpdateService).startCMServices(eq(stackId));
        Exception exception = assertThrows(Exception.class, () -> underTest.startCMServices(stackId));
        verify(diskUpdateService, times(1)).startCMServices(stackId);
        assertEquals("TEST EXCEPTION", exception.getMessage());
    }

    @Test
    void testResizeDisks() throws Exception {
        Long stackId = 1L;
        underTest.resizeDisks(stackId, "test");
        verify(diskUpdateService, times(1)).resizeDisks(stackId, "test");
    }

    @Test
    void testResizeDisksThrowsException() throws Exception {
        Long stackId = 1L;
        doThrow(new CloudbreakApiException("TEST EXCEPTION")).when(diskUpdateService).resizeDisks(eq(stackId), eq("test"));
        Exception exception = assertThrows(CloudbreakApiException.class, () -> underTest.resizeDisks(stackId, "test"));
        verify(diskUpdateService, times(1)).resizeDisks(stackId, "test");
        assertEquals("TEST EXCEPTION", exception.getMessage());
    }
}
