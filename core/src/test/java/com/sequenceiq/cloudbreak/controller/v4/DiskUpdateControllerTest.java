package com.sequenceiq.cloudbreak.controller.v4;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.DiskModificationRequest;
import com.sequenceiq.cloudbreak.service.diskupdate.DiskUpdateService;
import com.sequenceiq.cloudbreak.service.stack.flow.StackOperationService;

@ExtendWith(MockitoExtension.class)
public class DiskUpdateControllerTest {

    @Mock
    private DiskUpdateService diskUpdateService;

    @Mock
    private StackOperationService stackOperationService;

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
        diskModificationRequest.setStackId(1L);
        underTest.updateDiskTypeAndSize(diskModificationRequest);
        verify(stackOperationService, times(1)).datalakeUpdateDisks(diskModificationRequest);
    }

    @Test
    void testUpdateDiskTypeAndSizeThrowsException() throws Exception {
        DiskModificationRequest diskModificationRequest = new DiskModificationRequest();
        diskModificationRequest.setStackId(1L);
        doThrow(new RuntimeException("TEST EXCEPTION")).when(stackOperationService).datalakeUpdateDisks(diskModificationRequest);
        Exception exception = assertThrows(RuntimeException.class, () -> underTest.updateDiskTypeAndSize(diskModificationRequest));
        assertEquals("TEST EXCEPTION", exception.getMessage());
        verify(stackOperationService, times(1)).datalakeUpdateDisks(diskModificationRequest);
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
}