package com.sequenceiq.cloudbreak.service.datalake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.cloud.init.CloudPlatformConnectors;
import com.sequenceiq.cloudbreak.cloud.service.CloudParameterCache;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorNotifier;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.dto.StackDto;
import com.sequenceiq.cloudbreak.service.CloudbreakException;
import com.sequenceiq.cloudbreak.service.cluster.ClusterApiConnectors;
import com.sequenceiq.cloudbreak.service.resource.ResourceService;
import com.sequenceiq.cloudbreak.service.stack.StackDtoService;
import com.sequenceiq.cloudbreak.service.stack.StackService;
import com.sequenceiq.cloudbreak.util.StackUtil;

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
    }
}
