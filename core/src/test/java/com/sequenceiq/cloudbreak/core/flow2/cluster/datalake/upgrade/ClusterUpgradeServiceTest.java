package com.sequenceiq.cloudbreak.core.flow2.cluster.datalake.upgrade;

import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_MANAGER_UPGRADE_FAILED;
import static com.sequenceiq.cloudbreak.event.ResourceEvent.CLUSTER_UPGRADE_FAILED;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.core.flow2.stack.CloudbreakFlowMessageService;
import com.sequenceiq.cloudbreak.event.ResourceEvent;
import com.sequenceiq.cloudbreak.service.StackUpdater;
import com.sequenceiq.cloudbreak.service.cluster.ClusterService;

public class ClusterUpgradeServiceTest {

    private static final String ERROR_MESSAGE = "error message";

    private static final long STACK_ID = 1L;

    @Mock
    private CloudbreakFlowMessageService flowMessageService;

    @Mock
    private ClusterService clusterService;

    @Mock
    private StackUpdater stackUpdater;

    @InjectMocks
    private ClusterUpgradeService underTest;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testClusterManagerUpgradeFailure() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        underTest.handleUpgradeClusterFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.CLUSTER_MANAGER_UPGRADE_FAILED);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(ERROR_MESSAGE));
        assertEquals(CLUSTER_MANAGER_UPGRADE_FAILED, captor.getValue());
    }

    @Test
    public void testClusterUpgradeFailure() {
        ArgumentCaptor<ResourceEvent> captor = ArgumentCaptor.forClass(ResourceEvent.class);
        underTest.handleUpgradeClusterFailure(STACK_ID, ERROR_MESSAGE, DetailedStackStatus.CLUSTER_UPGRADE_FAILED);
        verify(flowMessageService).fireEventAndLog(eq(STACK_ID), eq(Status.UPDATE_FAILED.name()), captor.capture(), eq(ERROR_MESSAGE));
        assertEquals(CLUSTER_UPGRADE_FAILED, captor.getValue());
    }
}