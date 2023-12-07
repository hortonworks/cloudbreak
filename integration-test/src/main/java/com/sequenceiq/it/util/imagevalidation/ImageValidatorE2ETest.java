package com.sequenceiq.it.util.imagevalidation;

import com.sequenceiq.it.cloudbreak.context.TestContext;

public interface ImageValidatorE2ETest {

    String getImageId(TestContext testContext);

    boolean isPrewarmedImageTest();

}
