package com.sequenceiq.it.cloudbreak.config.testinformation;

import org.springframework.stereotype.Service;

@Service
public class TestInformationService {
    private static final ThreadLocal<TestInformation> TEST_INFORMATION_THREAD_LOCAL = new ThreadLocal<>();

    public void setTestInformation(TestInformation testInformation) {
        TEST_INFORMATION_THREAD_LOCAL.set(testInformation);
    }

    public TestInformation getTestInformation() {
        return TEST_INFORMATION_THREAD_LOCAL.get();
    }

    public void removeTestInformation() {
        TEST_INFORMATION_THREAD_LOCAL.remove();
    }
}