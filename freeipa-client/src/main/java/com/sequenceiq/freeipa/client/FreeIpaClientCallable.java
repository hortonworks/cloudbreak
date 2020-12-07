package com.sequenceiq.freeipa.client;

@FunctionalInterface
public interface FreeIpaClientCallable<T> {
    T run() throws FreeIpaClientException;
}
