package com.sequenceiq.it.cloudbreak.dto.mock;

public class CheckCount {
    private CheckCount() {
    }

    public static <T> Verification<T> times(int expectedTimes) {
        return (path, method, client, model, calls) -> {
            if (calls.size() != expectedTimes) {
                //TODO please don't remove this. It will be enabled if the following jira will be resolved: https://jira.cloudera.com/browse/CB-9111
//                throw new TestFailException(String.format(path + " " + method.getMethodName() + " method call count expected to be %d, but was %d",
//                        expectedTimes, calls.size()));
            }
        };
    }

    public static <T> Verification<T> atLeast(int expectedMinimumTimes) {
        return (path, method, client, model, calls) -> {
            if (calls.size() < expectedMinimumTimes) {
                //TODO please don't remove this. It will be enabled if the following jira will be resolved: https://jira.cloudera.com/browse/CB-9111
//                throw new TestFailException(String.format(path + " " + method.getMethodName() + " method call count expected to be %d at least, but was %d",
//                        expectedMinimumTimes, calls.size()));
            }
        };
    }
}
