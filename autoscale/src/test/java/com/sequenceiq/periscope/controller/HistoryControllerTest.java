package com.sequenceiq.periscope.controller;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.periscope.api.model.AutoscaleClusterHistoryResponse;
import com.sequenceiq.periscope.converter.HistoryConverter;
import com.sequenceiq.periscope.domain.Cluster;
import com.sequenceiq.periscope.domain.History;
import com.sequenceiq.periscope.service.AutoscaleRestRequestThreadLocalService;
import com.sequenceiq.periscope.service.ClusterService;
import com.sequenceiq.periscope.service.HistoryService;
import com.sequenceiq.periscope.service.NotFoundException;

@RunWith(MockitoJUnitRunner.class)
public class HistoryControllerTest {

    @Rule
    public final ExpectedException expectedException = ExpectedException.none();

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

    private String tenant = "testTenant";

    private Long clusterId = 100L;

    private Cluster cluster = new Cluster();

    @Before
    public void setUp() {
        cluster.setId(clusterId);
        when(restRequestThreadLocalService.getCloudbreakTenant()).thenReturn(tenant);
    }

    @Test
    public void testGetHistoryWhenClusterNotFound() {
        when(clusterService.findOneByStackCrnAndTenant(testClusterCrn, tenant)).thenReturn(Optional.empty());

        expectedException.expect(NotFoundException.class);
        expectedException.expectMessage("cluster 'crn:cdp:iam:us-west-1:%s:cluster:mockuser@cloudera.com' not found.");

        underTest.getHistory(testClusterCrn);
    }

    @Test
    public void testGetHistoryWhenNoHistory() {
        when(clusterService.findOneByStackCrnAndTenant(testClusterCrn, tenant)).thenReturn(Optional.of(cluster));
        when(historyService.getHistory(clusterId)).thenReturn(List.of());

        List<AutoscaleClusterHistoryResponse> historyResponses = underTest.getHistory(testClusterCrn);
        assertEquals("History Response size is 0", 0, historyResponses.size());
    }

    @Test
    public void testGetHistoryWhenHistoryFound() {
        List<History> mockHistoryResponses = List.of(new History(), new History());
        when(clusterService.findOneByStackCrnAndTenant(testClusterCrn, tenant)).thenReturn(Optional.of(cluster));
        when(historyService.getHistory(clusterId)).thenReturn(mockHistoryResponses);
        when(historyConverter.convertAllToJson(any(List.class))).thenCallRealMethod();

        List<AutoscaleClusterHistoryResponse> historyResponses = underTest.getHistory(testClusterCrn);
        assertEquals("History Response size is 2", 2, historyResponses.size());
    }
}
