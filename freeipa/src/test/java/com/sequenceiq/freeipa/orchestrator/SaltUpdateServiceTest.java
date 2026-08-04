package com.sequenceiq.freeipa.orchestrator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.sequenceiq.flow.api.model.FlowIdentifier;
import com.sequenceiq.flow.api.model.FlowType;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.service.client.CachedEnvironmentClientService;
import com.sequenceiq.freeipa.service.freeipa.flow.FreeIpaFlowManager;
import com.sequenceiq.freeipa.service.stack.StackService;

@ExtendWith(MockitoExtension.class)
class SaltUpdateServiceTest {

    private static final String ENVIRONMENT_CRN = "crn:cdp:environments:us-west-1:tenant:environment:env-uuid";

    private static final String ACCOUNT_ID = "tenant";

    private static final Long STACK_ID = 42L;

    @Mock
    private FreeIpaFlowManager flowManager;

    @Mock
    private StackService stackService;

    @Mock
    private CachedEnvironmentClientService cachedEnvironmentClientService;

    @InjectMocks
    private SaltUpdateService underTest;

    @Test
    void updateSaltStatesEvictsEnvironmentCacheBeforeFiringEvent() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(flowManager.notify(anyString(), any())).thenReturn(new FlowIdentifier(FlowType.FLOW, "flow-id"));

        underTest.updateSaltStates(ENVIRONMENT_CRN, ACCOUNT_ID);

        InOrder inOrder = inOrder(cachedEnvironmentClientService, flowManager);
        inOrder.verify(cachedEnvironmentClientService).evictCache(ENVIRONMENT_CRN);
        inOrder.verify(flowManager).notify(anyString(), any());
    }

    @Test
    void updateSaltStatesReturnsFlowIdentifierFromFlowManager() {
        Stack stack = new Stack();
        stack.setId(STACK_ID);
        stack.setEnvironmentCrn(ENVIRONMENT_CRN);
        FlowIdentifier expected = new FlowIdentifier(FlowType.FLOW, "flow-id");
        when(stackService.getByEnvironmentCrnAndAccountId(ENVIRONMENT_CRN, ACCOUNT_ID)).thenReturn(stack);
        when(flowManager.notify(anyString(), any())).thenReturn(expected);

        FlowIdentifier actual = underTest.updateSaltStates(ENVIRONMENT_CRN, ACCOUNT_ID);

        assertThat(actual).isEqualTo(expected);
    }
}
