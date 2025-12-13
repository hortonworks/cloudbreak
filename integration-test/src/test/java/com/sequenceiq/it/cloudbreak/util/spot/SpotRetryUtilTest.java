package com.sequenceiq.it.cloudbreak.util.spot;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.lang.reflect.Method;

import org.testng.ITestNGMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.internal.ConstructorOrMethod;

public class SpotRetryUtilTest {

    private Method method;

    private SpotUtil spotUtil;

    private ITestNGMethod testMethod;

    private SpotRetryUtil underTest;

    @BeforeMethod
    public void setUp() throws NoSuchMethodException {
        ConstructorOrMethod constructorOrMethod = mock(ConstructorOrMethod.class);
        method = ExampleTest.class.getDeclaredMethod("exampleMethod");
        when(constructorOrMethod.getMethod()).thenReturn(method);
        testMethod = mock(ITestNGMethod.class);
        when(testMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);

        spotUtil = mock(SpotUtil.class);
        underTest = new SpotRetryUtil(spotUtil, true);
    }

    @Test
    void shouldRetryOnFirstTryButNotOnSecondWhenShouldUseSpotInstances() {
        when(spotUtil.shouldUseSpotInstancesForTest(method)).thenReturn(true);

        // first try - is not retried, and it will retry
        assertFalse(underTest.isRetried(testMethod));
        assertTrue(underTest.willRetry(testMethod));

        // second try - retried, and it will not retry
        assertTrue(underTest.isRetried(testMethod));
        assertFalse(underTest.willRetry(testMethod));
    }

    @Test
    void shouldNotRetryWhenShouldUseSpotInstancesIsFalse() {
        when(spotUtil.shouldUseSpotInstancesForTest(method)).thenReturn(false);

        boolean result = underTest.willRetry(testMethod);
        assertFalse(result);
    }

    @Test
    void shouldNotRetryWhenRetryEnabledIsFalse() {
        underTest = new SpotRetryUtil(spotUtil, true);

        boolean result = underTest.willRetry(testMethod);
        assertFalse(result);
    }

    private static class ExampleTest {
        private void exampleMethod() {
        }
    }
}
