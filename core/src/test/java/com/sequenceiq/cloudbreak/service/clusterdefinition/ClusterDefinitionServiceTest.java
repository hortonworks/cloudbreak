package com.sequenceiq.cloudbreak.service.clusterdefinition;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.ResourceStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.controller.exception.BadRequestException;
import com.sequenceiq.cloudbreak.domain.ClusterDefinition;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.repository.ClusterDefinitionRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@RunWith(MockitoJUnitRunner.class)
public class ClusterDefinitionServiceTest {

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @Mock
    private ClusterService clusterService;

    @Mock
    private ClusterDefinitionRepository clusterDefinitionRepository;

    @InjectMocks
    private ClusterDefinitionService underTest;

    private final ClusterDefinition clusterDefinition = new ClusterDefinition();

    @Before
    public void setup() {
        clusterDefinition.setName("name");
        clusterDefinition.setWorkspace(new Workspace());
        clusterDefinition.setStatus(ResourceStatus.USER_MANAGED);
    }

    @Test
    public void testDeletionWithZeroClusters() {
        when(clusterService.findByBlueprint(any())).thenReturn(Collections.emptySet());

        ClusterDefinition deleted = underTest.delete(clusterDefinition);

        assertNotNull(deleted);
    }

    @Test
    public void testDeletionWithNonTerminatedCluster() {
        Cluster cluster = new Cluster();
        cluster.setName("c1");
        cluster.setId(1L);
        cluster.setClusterDefinition(clusterDefinition);
        cluster.setStatus(Status.AVAILABLE);
        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage("c1");
        when(clusterService.findByBlueprint(any())).thenReturn(Set.of(cluster));

        underTest.delete(clusterDefinition);
    }

    @Test
    public void testDeletionWithTerminatedClusters() {
        Cluster cluster1 = new Cluster();
        cluster1.setName("c1");
        cluster1.setId(1L);
        cluster1.setClusterDefinition(clusterDefinition);
        cluster1.setStatus(Status.PRE_DELETE_IN_PROGRESS);
        Cluster cluster2 = new Cluster();
        cluster2.setName("c2");
        cluster2.setId(2L);
        cluster2.setClusterDefinition(clusterDefinition);
        cluster2.setStatus(Status.DELETE_IN_PROGRESS);
        Cluster cluster3 = new Cluster();
        cluster3.setName("c3");
        cluster3.setId(3L);
        cluster3.setClusterDefinition(clusterDefinition);
        cluster3.setStatus(Status.DELETE_COMPLETED);
        Cluster cluster4 = new Cluster();
        cluster4.setName("c4");
        cluster4.setId(4L);
        cluster4.setClusterDefinition(clusterDefinition);
        cluster4.setStatus(Status.DELETE_FAILED);

        when(clusterService.findByBlueprint(any())).thenReturn(Set.of(cluster1, cluster2, cluster3, cluster4));

        ClusterDefinition deleted = underTest.delete(clusterDefinition);

        assertNotNull(deleted);
        verify(clusterService, times(1)).saveAll(anyCollection());
    }

    @Test
    public void testDeletionWithTerminatedAndNonTerminatedClusters() {
        Cluster cluster1 = new Cluster();
        cluster1.setName("c1");
        cluster1.setId(1L);
        cluster1.setClusterDefinition(clusterDefinition);
        cluster1.setStatus(Status.AVAILABLE);
        Cluster cluster2 = new Cluster();
        cluster2.setName("c2");
        cluster2.setId(2L);
        cluster2.setClusterDefinition(clusterDefinition);
        cluster2.setStatus(Status.DELETE_IN_PROGRESS);

        when(clusterService.findByBlueprint(any())).thenReturn(Set.of(cluster1, cluster2));

        try {
            underTest.delete(clusterDefinition);
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("c1"));
            assertFalse(e.getMessage().contains("c2"));
        }
        verify(clusterService, times(1)).saveAll(anyCollection());
    }
}