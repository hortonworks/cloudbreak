package com.sequenceiq.periscope.utils;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class DelayedAnswer<T> implements Answer<T> {

    private final long delay;

    private final T ret;

    public DelayedAnswer(long delay, T ret) {
        this.delay = delay;
        this.ret = ret;
    }

    public static <T> DelayedAnswer<T> delayed(T ret) {
        return delayed(2000L, ret);
    }

    public static <T> DelayedAnswer<T> delayed(long delay, T ret) {
        return new DelayedAnswer<>(delay, ret);
    }

    @Override
    public T answer(InvocationOnMock invocation) throws Throwable {
        Thread.sleep(delay);
        return ret;
    }
}
