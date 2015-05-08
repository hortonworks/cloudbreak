package com.sequenceiq.cloudbreak.orchestrator;

import static com.sequenceiq.cloudbreak.orchestrator.SimpleContainerBootstrapRunner.simpleContainerBootstrapRunner;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.sequenceiq.cloudbreak.core.CloudbreakException;
import com.sequenceiq.cloudbreak.orchestrator.containers.ContainerBootstrap;

public class SimpleContainerBootstrapRunnerTest {

    @Test
    public void bootstrapSuccessWithoutException() throws Exception {
        Boolean call = simpleContainerBootstrapRunner(new MockBootstrapRunner(4)).call();
        assertEquals(true, call);
    }

    @Test(expected = CloudbreakException.class)
    public void bootstrapUnSuccessWithException() throws Exception {
        simpleContainerBootstrapRunner(new MockBootstrapRunner(10)).call();
    }

    public class MockBootstrapRunner implements ContainerBootstrap {

        private int count;
        private int retryOk = 4;

        public MockBootstrapRunner(int retryOk) {
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
}