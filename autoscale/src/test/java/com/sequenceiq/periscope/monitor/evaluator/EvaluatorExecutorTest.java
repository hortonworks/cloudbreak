package com.sequenceiq.periscope.monitor.evaluator;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

import javax.annotation.Nonnull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.sequenceiq.periscope.monitor.context.ClusterIdEvaluatorContext;
import com.sequenceiq.periscope.monitor.context.EvaluatorContext;
import com.sequenceiq.periscope.monitor.executor.ExecutorServiceWithRegistry;

public class EvaluatorExecutorTest {

    @Mock
    private ExecutorServiceWithRegistry executorServiceWithRegistry;

    @InjectMocks
    private TestEvaluatorExecutor underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        underTest.setExceptionForExecute(false);
    }

    @Test
    public void testRunCallsFinished() {
        underTest.run();

        verify(executorServiceWithRegistry).finished(any(), anyLong());
    }

    @Test
    public void testRunCallsFinishedWhenThrows() {
        underTest.setExceptionForExecute(true);

        underTest.run();

        verify(executorServiceWithRegistry).finished(any(), anyLong());
    }

    private static class TestEvaluatorExecutor extends EvaluatorExecutor {

        private boolean doThrowFromExecute;

        public void setExceptionForExecute(boolean doThrowFromExecute) {
            this.doThrowFromExecute = doThrowFromExecute;
        }

        @Nonnull
        @Override
        public EvaluatorContext getContext() {
            return new ClusterIdEvaluatorContext(1L);
        }

        @Override
        public String getName() {
            return "";
        }

        @Override
        public void setContext(EvaluatorContext context) {

        }

        @Override
        protected void execute() {
            if (doThrowFromExecute) {
                throw new RuntimeException("exception during execute");
            }
        }
    }
}
