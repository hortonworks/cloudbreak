package com.sequenceiq.cloudbreak.core.flow2.cluster;

import static org.mockito.Mockito.doReturn;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
