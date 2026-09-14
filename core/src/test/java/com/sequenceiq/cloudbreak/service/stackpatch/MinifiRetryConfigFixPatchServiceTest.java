package com.sequenceiq.cloudbreak.service.stackpatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;
import com.sequenceiq.cloudbreak.common.orchestration.Node;
import com.sequenceiq.cloudbreak.core.bootstrap.service.ClusterDeletionBasedExitCriteriaModel;
import com.sequenceiq.cloudbreak.core.flow2.service.ReactorFlowManager;
import com.sequenceiq.cloudbreak.domain.stack.Stack;
import com.sequenceiq.cloudbreak.domain.stack.StackPatchType;
import com.sequenceiq.cloudbreak.orchestrator.host.HostOrchestrator;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadata;
import com.sequenceiq.cloudbreak.orchestrator.metadata.OrchestratorMetadataProvider;
import com.sequenceiq.cloudbreak.service.retry.RetryType;
import com.sequenceiq.cloudbreak.util.StackStatusAndReachabilityValidatorUtil;

@ExtendWith(MockitoExtension.class)
class MinifiRetryConfigFixPatchServiceTest {

    private static final Long STACK_ID = 1L;

    private static final String CRN = "crn:cluster";

    private static final String FQDN = "host1.example.com";

    @Mock
    private OrchestratorMetadataProvider orchestratorMetadataProvider;

    @Mock
    private HostOrchestrator hostOrchestrator;

    @Mock
    private StackStatusAndReachabilityValidatorUtil stackStatusAndReachabilityValidatorUtil;

    @Mock
    private ReactorFlowManager reactorFlowManager;

    @InjectMocks
    private MinifiRetryConfigFixPatchService underTest;

    private Stack stack;

    private Node node;

    private OrchestratorMetadata metadata;

    @BeforeEach
    void setUp() {
        stack = new Stack();
        stack.setId(STACK_ID);
        stack.setResourceCrn(CRN);
        node = new Node("10.0.0.1", "1.2.3.4", "i-1", "m5.xlarge", FQDN, "master");
        metadata = new OrchestratorMetadata(Collections.emptyList(), Set.of(node),
                ClusterDeletionBasedExitCriteriaModel.nonCancellableModel(), null);
    }

    @Test
    void getStackPatchTypeReturnsMinifiRetryConfigFix() {
        assertTrue(StackPatchType.MINIFI_RETRY_CONFIG_FIX == underTest.getStackPatchType());
    }

    @Test
    void isAffectedThrowsWhenStackNotReachable() {
        when(stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)).thenReturn(false);

        assertThrows(CloudbreakServiceException.class, () -> underTest.isAffected(stack));
    }

    @Test
    void isAffectedReturnsTrueWhenMinifiActive() throws Exception {
        when(stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)).thenReturn(true);
        when(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).thenReturn(metadata);
        when(hostOrchestrator.runCommandOnHosts(anyList(), eq(Set.of(FQDN)), any(), eq(RetryType.NO_RETRY)))
                .thenReturn(Map.of(FQDN, "active\n"));

        assertTrue(underTest.isAffected(stack));
    }

    @Test
    void isAffectedReturnsFalseWhenMinifiInactive() throws Exception {
        when(stackStatusAndReachabilityValidatorUtil.validateStackStatusAndReachability(stack)).thenReturn(true);
        when(orchestratorMetadataProvider.getOrchestratorMetadata(STACK_ID)).thenReturn(metadata);
        when(hostOrchestrator.runCommandOnHosts(anyList(), eq(Set.of(FQDN)), any(), eq(RetryType.NO_RETRY)))
                .thenReturn(Map.of(FQDN, "inactive\n"));

        assertFalse(underTest.isAffected(stack));
    }

    @Test
    void doApplyTriggersSaltUpdateFlow() throws Exception {
        boolean applied = underTest.doApply(stack);

        assertTrue(applied);
        verify(reactorFlowManager).triggerSaltUpdate(STACK_ID);
    }

    @Test
    void doApplyThrowsWhenFlowTriggerFails() {
        when(reactorFlowManager.triggerSaltUpdate(STACK_ID)).thenThrow(new RuntimeException("boom"));

        assertThrows(ExistingStackPatchApplyException.class, () -> underTest.doApply(stack));
        verify(orchestratorMetadataProvider, never()).getOrchestratorMetadata(any());
    }
}
