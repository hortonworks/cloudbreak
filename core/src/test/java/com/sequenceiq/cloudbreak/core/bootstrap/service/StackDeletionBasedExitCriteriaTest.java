package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.scheduler.PollGroup;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.domain.stack.cluster.Cluster;
import com.sequenceiq.cloudbreak.domain.stack.Stack;

@RunWith(MockitoJUnitRunner.class)
public class StackDeletionBasedExitCriteriaTest {

    @InjectMocks
    private StackDeletionBasedExitCriteria underTest;

    @Test
    public void exitNeededScenariosTest() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.clusterDefinition(), stack, 1L);
        stack.setCluster(cluster);
        StackDeletionBasedExitCriteriaModel exitCriteriaModel = new StackDeletionBasedExitCriteriaModel(1L);

        InMemoryStateStore.putStack(1L, PollGroup.POLLABLE);
        assertFalse(underTest.isExitNeeded(exitCriteriaModel));

        InMemoryStateStore.putStack(1L, PollGroup.CANCELLED);
        assertTrue(underTest.isExitNeeded(exitCriteriaModel));

    }

}