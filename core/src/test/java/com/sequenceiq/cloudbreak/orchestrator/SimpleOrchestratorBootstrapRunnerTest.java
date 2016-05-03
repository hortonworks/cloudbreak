package com.sequenceiq.cloudbreak.orchestrator;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.junit.Test;
import org.slf4j.MDC;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteria;
import com.sequenceiq.cloudbreak.orchestrator.state.ExitCriteriaModel;

public class SimpleOrchestratorBootstrapRunnerTest {

    @Test
    public void bootstrapSuccessWithoutException() throws Exception {
        MDC.put("test", "test");
        Boolean call = new OrchestratorBootstrapRunner(new MockBootstrapRunner(1, MDC.getCopyOfContextMap()),
                new MockExitCriteria(),
                new MockExitCriteriaModel(),
                MDC.getCopyOfContextMap()).call();
        assertEquals(true, call);
    }

    public class MockBootstrapRunner implements OrchestratorBootstrap {

        private int count;
        private int retryOk = 2;
        private final Map<String, String> mdcMap;

        public MockBootstrapRunner(int retryOk, Map<String, String> mdcMap) {
            this.mdcMap = mdcMap;
            this.retryOk = retryOk;
        }

        @Override
        public Boolean call() throws Exception {
            count++;
            if (count != retryOk) {
                throw new CloudbreakException("test");
            } else {
                return true;
            }
        }

    }

    public class MockExitCriteriaModel extends ExitCriteriaModel {
        public MockExitCriteriaModel() {

        }
    }

    public class MockExitCriteria implements ExitCriteria {

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