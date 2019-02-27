package com.sequenceiq.it.cloudbreak.newway.context;

public class TestCaseDescription {

    private final String value;

    private TestCaseDescription(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static TestCaseDescription testCaseDescription(String value) {
        return new TestCaseDescription(value);
    }

    public static class TestCaseDescriptionBuilder {

        private final StringBuilder stringBuilder;

        public TestCaseDescriptionBuilder() {
            stringBuilder = new StringBuilder();
        }

        public TestCaseDescriptionBuilder given(String givenStatement) {
            stringBuilder.append("GIVEN ").append(givenStatement).append('\n');
            return this;
        }

        public TestCaseDescriptionBuilder when(String whenStatement) {
            stringBuilder.append(" WHEN ").append(whenStatement).append('\n');
            return this;
        }

        public TestCaseDescription then(String thenStatement) {
            stringBuilder.append(" THEN ").append(thenStatement);
            return testCaseDescription(stringBuilder.toString());
        }

        public static TestCaseDescriptionBuilder createWithGiven(String given) {
            return new TestCaseDescriptionBuilder().given(given);
        }

    }
}
