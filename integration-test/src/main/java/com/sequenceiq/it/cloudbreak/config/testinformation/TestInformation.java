package com.sequenceiq.it.cloudbreak.config.testinformation;

public class TestInformation {
    private String suiteName;

    private String testName;

    public TestInformation(String suiteName, String testName) {
        this.suiteName = suiteName;
        this.testName = testName;
    }

    public String getSuiteName() {
        return suiteName;
    }

    public void setSuiteName(String suiteName) {
        this.suiteName = suiteName;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getTestLabel() {
        return suiteName + "." + testName;
    }
}
