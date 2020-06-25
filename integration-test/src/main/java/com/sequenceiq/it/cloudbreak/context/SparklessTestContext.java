package com.sequenceiq.it.cloudbreak.context;

import java.util.Optional;

import org.springframework.context.annotation.Primary;

import com.sequenceiq.it.cloudbreak.Prototype;
import com.sequenceiq.it.cloudbreak.exception.TestMethodNameMissingException;

@Prototype
@Primary
public class SparklessTestContext extends TestContext {

    /**
     * In non-mock tests we use the test method name as a tag, so we must be sure that is is there
     */
    @Override
    public Optional<String> getTestMethodName() {
        Optional<String> testMethodName = super.getTestMethodName();
        if (testMethodName.isEmpty()) {
            throw new TestMethodNameMissingException();
        }
        return testMethodName;
    }
}
