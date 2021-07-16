package com.sequenceiq.freeipa.flow.chain;

import static com.sequenceiq.freeipa.flow.chain.AbstractCommonChainAction.REPAIR;
import static com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction.CHAINED_ACTION;
import static com.sequenceiq.freeipa.flow.chain.FlowChainAwareAction.FINAL_CHAIN;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.statemachine.StateContext;

import com.sequenceiq.flow.core.FlowParameters;
import com.sequenceiq.flow.core.helloworld.HelloWorldContext;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldEvent;
import com.sequenceiq.flow.core.helloworld.config.HelloWorldState;
import com.sequenceiq.flow.core.helloworld.flowevents.HelloWorldFlowTrigger;

class AbstractCommonChainActionTest {

    private DummyCommonChainAction underTest = new DummyCommonChainAction();

    @Test
    public void testShouldCompleteOperation() {
        assertTrue(underTest.shouldCompleteOperation(Map.of()));
        assertFalse(underTest.shouldCompleteOperation(Map.of(REPAIR, true)));
        assertFalse(underTest.shouldCompleteOperation(Map.of(REPAIR, true, CHAINED_ACTION, true)));
        assertFalse(underTest.shouldCompleteOperation(Map.of(REPAIR, false, CHAINED_ACTION, true)));
        assertFalse(underTest.shouldCompleteOperation(Map.of(REPAIR, true, CHAINED_ACTION, false)));
        assertTrue(underTest.shouldCompleteOperation(Map.of(REPAIR, false, CHAINED_ACTION, false)));
        assertTrue(underTest.shouldCompleteOperation(Map.of(FINAL_CHAIN, true)));
        assertTrue(underTest.shouldCompleteOperation(Map.of(FINAL_CHAIN, false)));
        assertTrue(underTest.shouldCompleteOperation(Map.of(REPAIR, true, CHAINED_ACTION, true, FINAL_CHAIN, true)));
        assertTrue(underTest.shouldCompleteOperation(Map.of(REPAIR, false, CHAINED_ACTION, true, FINAL_CHAIN, true)));
        assertTrue(underTest.shouldCompleteOperation(Map.of(REPAIR, true, CHAINED_ACTION, false, FINAL_CHAIN, true)));
        assertTrue(underTest.shouldCompleteOperation(Map.of(REPAIR, false, CHAINED_ACTION, false, FINAL_CHAIN, true)));
        assertFalse(underTest.shouldCompleteOperation(Map.of(REPAIR, true, CHAINED_ACTION, true, FINAL_CHAIN, false)));
        assertFalse(underTest.shouldCompleteOperation(Map.of(REPAIR, false, CHAINED_ACTION, true, FINAL_CHAIN, false)));
        assertFalse(underTest.shouldCompleteOperation(Map.of(REPAIR, true, CHAINED_ACTION, false, FINAL_CHAIN, false)));
        assertTrue(underTest.shouldCompleteOperation(Map.of(REPAIR, false, CHAINED_ACTION, false, FINAL_CHAIN, false)));
    }

    private static class DummyCommonChainAction extends AbstractCommonChainAction<HelloWorldState, HelloWorldEvent, HelloWorldContext, HelloWorldFlowTrigger> {

        protected DummyCommonChainAction() {
            super(HelloWorldFlowTrigger.class);
        }

        @Override
        protected HelloWorldContext createFlowContext(FlowParameters flowParameters, StateContext<HelloWorldState, HelloWorldEvent> stateContext,
                HelloWorldFlowTrigger payload) {
            return null;
        }

        @Override
        protected void doExecute(HelloWorldContext context, HelloWorldFlowTrigger payload, Map<Object, Object> variables) throws Exception {

        }

        @Override
        protected Object getFailurePayload(HelloWorldFlowTrigger payload, Optional<HelloWorldContext> flowContext, Exception ex) {
            return null;
        }
    }

}