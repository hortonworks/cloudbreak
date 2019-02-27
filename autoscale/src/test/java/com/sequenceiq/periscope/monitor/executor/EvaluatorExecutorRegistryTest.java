package com.sequenceiq.periscope.monitor.executor;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import com.sequenceiq.cloudbreak.service.Clock;
import com.sequenceiq.periscope.monitor.evaluator.EvaluatorExecutor;

public class EvaluatorExecutorRegistryTest {

    private static final String EXECUTOR_ONE = "one";

    private static final String EXECUTOR_ANOTHER = "another";

    private static final long CLUSTER_ONE = 1L;

    private static final long CLUSTER_ANOTHER = 2L;

    private static final int TIMEOUT = 600000;

    @Mock
    private Clock clock;

    @InjectMocks
    private EvaluatorExecutorRegistry underTest;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        ReflectionTestUtils.setField(underTest, "timeout", TIMEOUT);
        ReflectionTestUtils.setField(underTest, "submittedEvaluators", new ConcurrentHashMap<>());
    }

    @Test
    public void testPutIfAbsent() {

        assertTrue(underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE));
    }

    @Test
    public void testPutIfAbsentReturnsFalseWhenAddingSameValues() {
        underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE);
        when(clock.getCurrentTimeMillis()).thenReturn(600000L);

        assertFalse(underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE));
    }

    @Test
    public void testPutIfAbsentReturnsTrueWhenAddRemoveAddSameValues() {
        underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE);
        underTest.remove(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE);

        assertTrue(underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE));
    }

    @Test
    public void testPutIfAbsentReturnsTrueWhenAddingDifferentExecutor() {
        underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE);

        assertTrue(underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ANOTHER), CLUSTER_ONE));
    }

    @Test
    public void testPutIfAbsentReturnsTrueWhenAddingDifferentCluster() {
        underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE);

        assertTrue(underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ANOTHER));
    }

    @Test
    public void testRemoveWhenEmpty() {
        underTest.remove(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE);

        assertTrue(underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE));
    }

    @Test
    public void testPutIfAbsentWhenElementHasBeenTooLongInRegister() {
        when(clock.getCurrentTimeMillis()).thenReturn(0L);
        underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE);
        when(clock.getCurrentTimeMillis()).thenReturn(600001L);

        assertTrue(underTest.putIfAbsent(getEvaluatorExecutor(EXECUTOR_ONE), CLUSTER_ONE));
    }

    @Test
    public void testPutIfAbsentThreadSafe() {
        int numberOfElements = 100;
        int firstHalf = numberOfElements / 2;
        for (int i = 0; i < firstHalf; i++) {
            underTest.putIfAbsent(getEvaluatorExecutor("name" + i), i);
        }

        Map<Integer, Boolean> results = new ConcurrentHashMap<>();
        new ThreadSafetyTester(numberOfElements)
                .withBlockToTest(i -> results.put(i, underTest.putIfAbsent(getEvaluatorExecutor("name" + i), i)))
                .run()
                .waitUntilFinished();

        assertTrue(IntStream.range(0, firstHalf).noneMatch(results::get));
        assertTrue(IntStream.range(firstHalf, numberOfElements).allMatch(results::get));
    }

    private EvaluatorExecutor getEvaluatorExecutor(String name) {
        EvaluatorExecutor evaluatorExecutor = mock(EvaluatorExecutor.class);
        when(evaluatorExecutor.getName()).thenReturn(name);
        return evaluatorExecutor;
    }
}
