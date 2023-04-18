package com.sequenceiq.cloudbreak.core.flow2.cluster;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dyngr.exception.PollerStoppedException;
import com.sequenceiq.cloudbreak.cluster.api.ClusterApi;
import com.sequenceiq.cloudbreak.cluster.api.ClusterModificationService;

@ExtendWith(MockitoExtension.class)
public class ClouderaManagerPollingUtilServiceTest {

    @Mock
    private ClusterApi clusterApi;

    @Mock
    private ClusterModificationService clusterModificationService;

    @InjectMocks
    private ClouderaManagerPollingUtilService underTest;

    @BeforeEach
    void setUp() {
        doReturn(clusterModificationService).when(clusterApi).clusterModificationService();
    }

    @Test
    public void pollClouderaManagerServicesSuccess() throws Exception {
        Map<String, String> readStatusMap = new HashMap<>();
        readStatusMap.put("test_service", "STARTED");
        doReturn(readStatusMap).when(clusterModificationService).fetchServiceStatuses();
        underTest.pollClouderaManagerServices(clusterApi, "TEST_SERVICE", "STARTED");
    }

    @Test
    public void pollClouderaManagerServicesFailure() throws Exception {
        doThrow(new Exception("TEST")).when(clusterModificationService).fetchServiceStatuses();
        PollerStoppedException ex = assertThrows(PollerStoppedException.class, () ->
                underTest.pollClouderaManagerServices(clusterApi, "TEST_SERVICE", "STARTED"));
        assertEquals("java.lang.Exception: TEST", ex.getMessage());
    }
}
