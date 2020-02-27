package com.sequenceiq.cloudbreak.common.service;

import java.util.function.Supplier;

public interface TransactionExecutorService {

    <T> T required(Supplier<T> callback);

    <T> T requiresNew(Supplier<T> callback);

    <T> T mandatory(Supplier<T> callback);

    <T> T supports(Supplier<T> callback);

    <T> T notSupported(Supplier<T> callback);

    <T> T never(Supplier<T> callback);

    void required(Runnable callback);

    void requiresNew(Runnable callback);

    void mandatory(Runnable callback);

    void supports(Runnable callback);

    void notSupported(Runnable callback);

    void never(Runnable callback);
}
