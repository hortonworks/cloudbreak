package com.sequenceiq.periscope.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.NotFoundException;
import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.converter.HistoryConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;

@ExtendWith(MockitoExtension.class)
public class HistoryControllerTest {

    @InjectMocks
    private HistoryController underTest;

    @Mock
    private HistoryService historyService;

    @Mock
    private HistoryConverter historyConverter;

    @Mock
    private ClusterService clusterService;

    @Mock
    private AutoscaleRestRequestThreadLocalService restRequestThreadLocalService;

    private String testClusterCrn = "crn:cdp:iam:us-west-1:%s:cluster:mockuser@cloudera.com";

    private String testClusterName = "testCluster";

    private String tenant = "testTenant";

    private Long clusterId = 100L;

    private Cluster cluster = new Cluster();

    @BeforeEach
    public void setUp() {
        cluster.setId(clusterId);
        when(restRequestThreadLocalService.getCloudbreakTenant()).thenReturn(tenant);
    }

    @Test
    public void testGetHistoryWhenClusterNotFound() {
        when(clusterService.findOneByStackCrnAndTenant(testClusterCrn, tenant)).thenReturn(Optional.empty());

        NotFoundException err = assertThrows(NotFoundException.class, () -> underTest.getHistoryByCrn(testClusterCrn, 10));
        assertEquals("cluster 'crn:cdp:iam:us-west-1:%s:cluster:mockuser@cloudera.com' not found.", err.getMessage());
    }

    @Test
    public void testGetHistoryWhenNoHistory() {
        when(clusterService.findOneByStackCrnAndTenant(testClusterCrn, tenant)).thenReturn(Optional.of(cluster));
        when(historyService.getHistory(clusterId, 10)).thenReturn(List.of());

        List<AutoscaleClusterHistoryResponse> historyResponses = underTest.getHistoryByCrn(testClusterCrn, 10);
        assertEquals(0, historyResponses.size(), "History Response size is 0");
    }

    @Test
    public void testGetHistoryByCrnWhenHistoryFound() {
        List<History> mockHistoryResponses = List.of(new History(), new History());

        when(clusterService.findOneByStackCrnAndTenant(testClusterCrn, tenant)).thenReturn(Optional.of(cluster));
        when(historyService.getHistory(clusterId, 10)).thenReturn(mockHistoryResponses);
        when(historyConverter.convertAllToJson(any(List.class))).thenCallRealMethod();

        List<AutoscaleClusterHistoryResponse> historyResponses = underTest.getHistoryByCrn(testClusterCrn, 10);
        assertEquals(2, historyResponses.size(), "History Response size is 2");
    }

    @Test
    public void testGetHistoryByNameWhenHistoryFound() {
        List<History> mockHistoryResponses = List.of(new History(), new History());
        when(clusterService.findOneByStackNameAndTenant(testClusterName, tenant)).thenReturn(Optional.of(cluster));
        when(historyService.getHistory(clusterId, 10)).thenReturn(mockHistoryResponses);
        when(historyConverter.convertAllToJson(any(List.class))).thenCallRealMethod();

        List<AutoscaleClusterHistoryResponse> historyResponses = underTest.getHistoryByName(testClusterName, 10);
        assertEquals(2, historyResponses.size(), "History Response size is 2");
    }
}
