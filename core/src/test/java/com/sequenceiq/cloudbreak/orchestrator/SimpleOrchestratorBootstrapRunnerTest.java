package com.sequenceiq.cloudbreak.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.orchestrator.exception.CloudbreakOrchestratorTimeoutException;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;
import com.sequenceiq.cloudbreak.service.CloudbreakException;

class SimpleOrchestratorBootstrapRunnerTest {

    private static final String EXCEPTION_MESSAGE = "exceptionTest";

    @Test
    void bootstrapSuccessWithoutException() throws Exception {
        MDC.put("test", "test");
        Boolean call = new OrchestratorBootstrapRunner(new MockBootstrapRunner(1),
                new MockExitCriteria(),
                new MockExitCriteriaModel(),
                MDC.getCopyOfContextMap()).call();
        assertEquals(true, call);
    }

    @Test
    void bootstrapSuccessWithException() throws Exception {
        MDC.put("test", "test");
        OrchestratorBootstrapRunner runner = new OrchestratorBootstrapRunner(new MockBootstrapRunner(-1),
                new MockExitCriteria(),
                new MockExitCriteriaModel(),
                MDC.getCopyOfContextMap(), 2, 1, 2);
        Boolean result = null;
        try {
            result = runner.call();
        } catch (CloudbreakOrchestratorTimeoutException exception) {
            assertTrue(exception.getMessage().contains(EXCEPTION_MESSAGE));
        }
        assertNull(result);
    }

    private static class MockBootstrapRunner implements OrchestratorBootstrap {

        private int count;

        private final int retryOk;

        private MockBootstrapRunner(int retryOk) {
            this.retryOk = retryOk;
        }

        @Override
        public Boolean call() throws Exception {
            count++;
            if (count != retryOk) {
                throw new CloudbreakException(EXCEPTION_MESSAGE);
            } else {
                return true;
            }
        }

        @Override
        public String toString() {
            return "MockBootstrapRunner{" +
                    "count=" + count +
                    ", retryOk=" + retryOk +
                    '}';
        }
    }

    private static class MockExitCriteriaModel extends ExitCriteriaModel {

    }

    private static class MockExitCriteria implements ExitCriteria {

        @Override
        public boolean isExitNeeded(ExitCriteriaModel exitCriteriaModel) {
            return false;
        }

        @Override
        public String exitMessage() {
            return "test";
        }
    }
}