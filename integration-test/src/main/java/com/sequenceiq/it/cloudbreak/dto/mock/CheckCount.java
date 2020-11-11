package com.sequenceiq.it.cloudbreak.dto.mock;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class CheckCount {
    private CheckCount() {
    }

    public static <T> Verification<T> times(int expectedTimes) {
        return (path, method, client, model, calls) -> {
            if (calls.size() != expectedTimes) {
                throw new TestFailException(String.format(path + " " + method.getMethodName() + " method call count expected to be %d, but was %d",
                        expectedTimes, calls.size()));
            }
        };
    }

    public static <T> Verification<T> atLeast(int expectedMinimumTimes) {
        return (path, method, client, model, calls) -> {
            if (calls.size() < expectedMinimumTimes) {
                throw new TestFailException(String.format(path + " " + method.getMethodName() + " method call count expected to be %d at least, but was %d",
                        expectedMinimumTimes, calls.size()));
            }
        };
    }
}
