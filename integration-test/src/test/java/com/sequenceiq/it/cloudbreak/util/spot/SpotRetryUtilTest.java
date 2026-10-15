package com.sequenceiq.it.cloudbreak.util.spot;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testng.ITestNGMethod;
import org.testng.internal.ConstructorOrMethod;

@ExtendWith(MockitoExtension.class)
class SpotRetryUtilTest {

    private Method method;

    @Mock
    private SpotUtil spotUtil;

    @Mock
    private ITestNGMethod testMethod;

    @Mock
    private ConstructorOrMethod constructorOrMethod;

    private SpotRetryUtil underTest;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        method = ExampleTest.class.getDeclaredMethod("exampleMethod");
        when(constructorOrMethod.getMethod()).thenReturn(method);
        when(testMethod.getConstructorOrMethod()).thenReturn(constructorOrMethod);

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
