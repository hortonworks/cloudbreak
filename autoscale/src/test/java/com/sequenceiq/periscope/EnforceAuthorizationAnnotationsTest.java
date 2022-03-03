package com.sequenceiq.periscope;

import org.junit.jupiter.api.Test;

import com.sequenceiq.authorization.EnforceAuthorizationAnnotationTestUtil;
import com.sequenceiq.authorization.EnforcePropertyProviderTestUtil;

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
