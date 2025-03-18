package com.sequenceiq.authorization;

import org.junit.jupiter.api.Test;

public class EnforceAuthorizationAnnotationsTest {

    @Test
    public void testIfControllerClassHasProperAnnotation() {
        EnforceAuthorizationAnnotationTestUtil.testIfControllerClassHasProperAnnotation();
    }

    @Test
    public void testIfControllerMethodsHaveProperAuthorizationAnnotation() {
        EnforceAuthorizationAnnotationTestUtil.testIfControllerMethodsHaveProperAuthorizationAnnotation();
    }

    @Test
    public void testIfAllNecessaryResourceProviderPresent() {
        EnforcePropertyProviderTestUtil.testIfAllNecessaryResourceProviderImplemented();
    }
}