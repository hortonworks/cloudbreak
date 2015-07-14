package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;
import com.sequenceiq.cloudbreak.service.stack.StackService;

@RunWith(MockitoJUnitRunner.class)
public class StackDeletionBasedExitCriteriaTest {

    @InjectMocks
    private StackDeletionBasedExitCriteria underTest;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackService stackService;

    @Test
    public void exitNeededScenariosTest() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        StackDeletionBasedExitCriteriaModel exitCriteriaModel = new StackDeletionBasedExitCriteriaModel(1L);

        when(stackService.findLazy(anyLong())).thenReturn(stack);

        assertFalse(underTest.isExitNeeded(exitCriteriaModel));

        stack.setStatus(Status.DELETE_IN_PROGRESS);
        assertTrue(underTest.isExitNeeded(exitCriteriaModel));

        stack.setStatus(Status.DELETE_COMPLETED);
        assertTrue(underTest.isExitNeeded(exitCriteriaModel));

        stack.setStatus(Status.AVAILABLE);
        assertFalse(underTest.isExitNeeded(exitCriteriaModel));

        cluster.setStatus(Status.DELETE_COMPLETED);
        assertTrue(underTest.isExitNeeded(exitCriteriaModel));

        cluster.setStatus(Status.DELETE_IN_PROGRESS);
        assertTrue(underTest.isExitNeeded(exitCriteriaModel));

        cluster.setStatus(Status.AVAILABLE);
        assertFalse(underTest.isExitNeeded(exitCriteriaModel));

        cluster.setStatus(Status.UPDATE_IN_PROGRESS);
        assertFalse(underTest.isExitNeeded(exitCriteriaModel));

        when(stackService.findLazy(anyLong())).thenThrow(new IllegalArgumentException("test"));
        assertTrue(underTest.isExitNeeded(exitCriteriaModel));
    }

}