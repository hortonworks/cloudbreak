package com.sequenceiq.it.cloudbreak.dto.mock;

public class CheckCount {
    private CheckCount() {
    }

    public static Verification times(int expectedTimes) {
        return (path, method, context) -> {
            if (context.getCalls().size() != expectedTimes) {
                context.getErrors().add(String.format(path + " " + method.getMethodName() + " method call count expected to be %d, but was %d", expectedTimes,
                        context.getCalls().size()));
            }
        };
    }

    public static Verification atLeast(int expectedMinimumTimes) {
        return (path, method, context) -> {
            if (context.getCalls().size() < expectedMinimumTimes) {
                context.getErrors().add(String.format(path + " " + method.getMethodName() + " method call count expected to be %d at least, but was %d",
                        expectedMinimumTimes, context.getCalls().size()));
            }
        };
    }
}
