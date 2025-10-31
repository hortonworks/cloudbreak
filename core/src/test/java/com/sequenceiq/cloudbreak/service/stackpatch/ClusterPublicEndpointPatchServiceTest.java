package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.TestUtil;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.DetailedStackStatus;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackStatus;
import com.sequenceiq.cloudbreak.service.publicendpoint.ClusterPublicEndpointManagementService;
import com.sequenceiq.cloudbreak.service.stackpatch.config.ClusterPublicEndpointPatchConfig;

@ExtendWith(MockitoExtension.class)
class ClusterPublicEndpointPatchServiceTest {

    @Mock
    private ClusterPublicEndpointManagementService clusterPublicEndpointManagementService;

    @Mock
    private ClusterPublicEndpointPatchConfig clusterPublicEndpointPatchConfig;

    @InjectMocks
    private ClusterPublicEndpointPatchService underTest;

    @Test
    public void testIsAffectedShouldReturnTrueWhenClusterIsAffected() {
        when(clusterPublicEndpointPatchConfig.getRelatedStacks()).thenReturn(Set.of(1L, 2L));
        boolean affected = underTest.isAffected(TestUtil.stack());
        assertTrue(affected);
    }

    @Test
    public void testIsAffectedShouldReturnFalseWhenClusterIsAffectedButStopped() {
        when(clusterPublicEndpointPatchConfig.getRelatedStacks()).thenReturn(Set.of(1L, 2L));
        Stack stack = TestUtil.stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.STOPPED));
        boolean affected = underTest.isAffected(stack);
        assertFalse(affected);
    }

    @Test
    public void testIsAffectedShouldReturnFalseWhenClusterIsAffectedButDeleted() {
        when(clusterPublicEndpointPatchConfig.getRelatedStacks()).thenReturn(Set.of(1L, 2L));
        Stack stack = TestUtil.stack();
        stack.setStackStatus(new StackStatus(stack, DetailedStackStatus.DELETE_COMPLETED));
        boolean affected = underTest.isAffected(stack);
        assertFalse(affected);
    }

    @Test
    public void testDoApply() {
        Stack stack = TestUtil.stack();
        boolean result = underTest.doApply(stack);
        assertTrue(result);
        verify(clusterPublicEndpointManagementService, times(1)).refreshDnsEntries(eq(stack));
    }
}