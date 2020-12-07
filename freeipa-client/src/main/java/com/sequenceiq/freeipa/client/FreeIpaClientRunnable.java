package com.sequenceiq.freeipa.client;

@FunctionalInterface
public interface FreeIpaClientRunnable {
    void run() throws FreeIpaClientException;
}
