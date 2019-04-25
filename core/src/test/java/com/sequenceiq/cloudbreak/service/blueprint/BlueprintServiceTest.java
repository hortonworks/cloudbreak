package com.sequenceiq.cloudbreak.service.blueprint;

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
import com.sequenceiq.cloudbreak.domain.Blueprint;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.workspace.Workspace;
import com.sequenceiq.cloudbreak.repository.BlueprintRepository;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

@RunWith(MockitoJUnitRunner.class)
public class BlueprintServiceTest {

    @Rule
    public final ExpectedException exceptionRule = ExpectedException.none();

    @Mock
    private ClusterService clusterService;

    @Mock
    private BlueprintRepository blueprintRepository;

    @InjectMocks
    private BlueprintService underTest;

    private final Blueprint blueprint = new Blueprint();

    @Before
    public void setup() {
        blueprint.setName("name");
        blueprint.setWorkspace(new Workspace());
        blueprint.setStatus(ResourceStatus.USER_MANAGED);
    }

    @Test
    public void testDeletionWithZeroClusters() {
        when(clusterService.findByBlueprint(any())).thenReturn(Collections.emptySet());

        Blueprint deleted = underTest.delete(blueprint);

        assertNotNull(deleted);
    }

    @Test
    public void testDeletionWithNonTerminatedCluster() {
        Cluster cluster = new Cluster();
        cluster.setName("c1");
        cluster.setId(1L);
        cluster.setBlueprint(blueprint);
        cluster.setStatus(Status.AVAILABLE);
        exceptionRule.expect(BadRequestException.class);
        exceptionRule.expectMessage("c1");
        when(clusterService.findByBlueprint(any())).thenReturn(Set.of(cluster));

        underTest.delete(blueprint);
    }

    @Test
    public void testDeletionWithTerminatedClusters() {
        Cluster cluster1 = new Cluster();
        cluster1.setName("c1");
        cluster1.setId(1L);
        cluster1.setBlueprint(blueprint);
        cluster1.setStatus(Status.PRE_DELETE_IN_PROGRESS);
        Cluster cluster2 = new Cluster();
        cluster2.setName("c2");
        cluster2.setId(2L);
        cluster2.setBlueprint(blueprint);
        cluster2.setStatus(Status.DELETE_IN_PROGRESS);
        Cluster cluster3 = new Cluster();
        cluster3.setName("c3");
        cluster3.setId(3L);
        cluster3.setBlueprint(blueprint);
        cluster3.setStatus(Status.DELETE_COMPLETED);
        Cluster cluster4 = new Cluster();
        cluster4.setName("c4");
        cluster4.setId(4L);
        cluster4.setBlueprint(blueprint);
        cluster4.setStatus(Status.DELETE_FAILED);

        when(clusterService.findByBlueprint(any())).thenReturn(Set.of(cluster1, cluster2, cluster3, cluster4));

        Blueprint deleted = underTest.delete(blueprint);

        assertNotNull(deleted);
        verify(clusterService, times(1)).saveAll(anyCollection());
    }

    @Test
    public void testDeletionWithTerminatedAndNonTerminatedClusters() {
        Cluster cluster1 = new Cluster();
        cluster1.setName("c1");
        cluster1.setId(1L);
        cluster1.setBlueprint(blueprint);
        cluster1.setStatus(Status.AVAILABLE);
        Cluster cluster2 = new Cluster();
        cluster2.setName("c2");
        cluster2.setId(2L);
        cluster2.setBlueprint(blueprint);
        cluster2.setStatus(Status.DELETE_IN_PROGRESS);

        when(clusterService.findByBlueprint(any())).thenReturn(Set.of(cluster1, cluster2));

        try {
            underTest.delete(blueprint);
        } catch (BadRequestException e) {
            assertTrue(e.getMessage().contains("c1"));
            assertFalse(e.getMessage().contains("c2"));
        }
        verify(clusterService, times(1)).saveAll(anyCollection());
    }
}