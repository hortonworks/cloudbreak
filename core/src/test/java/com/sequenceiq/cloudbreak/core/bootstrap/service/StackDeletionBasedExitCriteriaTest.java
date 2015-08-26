package com.sequenceiq.cloudbreak.core.bootstrap.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.cloud.store.InMemoryStateStore;
import com.sequenceiq.cloudbreak.domain.Cluster;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.domain.Status;

@RunWith(MockitoJUnitRunner.class)
public class StackDeletionBasedExitCriteriaTest {

    @InjectMocks
    private StackDeletionBasedExitCriteria underTest;

    @Test
    public void exitNeededScenariosTest() {
        Stack stack = TestUtil.stack();
        Cluster cluster = TestUtil.cluster(TestUtil.blueprint(), stack, 1L);
        stack.setCluster(cluster);
        StackDeletionBasedExitCriteriaModel exitCriteriaModel = new StackDeletionBasedExitCriteriaModel(1L);

        InMemoryStateStore.put(1L, Status.AVAILABLE);
        assertFalse(underTest.isExitNeeded(exitCriteriaModel));

        InMemoryStateStore.put(1L, Status.DELETE_IN_PROGRESS);
        assertTrue(underTest.isExitNeeded(exitCriteriaModel));

        InMemoryStateStore.put(1L, Status.DELETE_COMPLETED);
        assertTrue(underTest.isExitNeeded(exitCriteriaModel));

        InMemoryStateStore.put(1L, Status.CREATE_IN_PROGRESS);
        assertFalse(underTest.isExitNeeded(exitCriteriaModel));

        InMemoryStateStore.put(1L, Status.UPDATE_IN_PROGRESS);
        assertFalse(underTest.isExitNeeded(exitCriteriaModel));
    }

}