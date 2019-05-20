package com.sequenceiq.it.cloudbreak;

import java.util.concurrent.Callable;

public abstract class GenericProxyExecutor {

    private static final String GENERIC_EXCEPTION_MESSAGE = "Unable invoke method for proxy instance.";

    public abstract <R> R exec(Callable<R> method);

    static String getGenericExceptionMessage() {
        return GENERIC_EXCEPTION_MESSAGE;
    }

}
