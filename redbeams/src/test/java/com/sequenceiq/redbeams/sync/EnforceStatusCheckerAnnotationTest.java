package com.sequenceiq.redbeams.sync;

import org.junit.jupiter.api.Test;

import com.sequenceiq.cloudbreak.quartz.EnforceStatusCheckerAnnotationUtil;

public class EnforceStatusCheckerAnnotationTest {

    @Test
    public void enforceDisablingConcurrentExecution() {
        EnforceStatusCheckerAnnotationUtil.enforceDisablingConcurrentExecution();
    }
}
