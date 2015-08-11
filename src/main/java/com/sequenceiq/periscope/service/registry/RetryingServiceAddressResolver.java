package com.sequenceiq.periscope.service.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryingServiceAddressResolver implements ServiceAddressResolver {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetryingServiceAddressResolver.class);
    private static final int MAX_ATTEMPT_COUNT = 10;
    private static final int SLEEPTIME = 2000;

    private ServiceAddressResolver serviceAddressResolver;

    public RetryingServiceAddressResolver(ServiceAddressResolver serviceAddressResolver) {
        this.serviceAddressResolver = serviceAddressResolver;
    }

    @Override
    public String resolveUrl(String serverUrl, String protocol, String serviceId) throws ServiceAddressResolvingException {
        int attemptCount = 0;
        String resolvedAddress = null;
        while (resolvedAddress == null && attemptCount < MAX_ATTEMPT_COUNT) {
            try {
                resolvedAddress = serviceAddressResolver.resolveUrl(serverUrl, protocol, serviceId);
            } catch (ServiceAddressResolvingException e) {
                handleException(e, attemptCount);
            }
            attemptCount++;
        }
        return resolvedAddress;
    }

    @Override
    public String resolveHostPort(String host, String port, String serviceId) throws ServiceAddressResolvingException {
        int attemptCount = 0;
        String resolvedAddress = null;
        while (resolvedAddress == null && attemptCount < MAX_ATTEMPT_COUNT) {
            try {
                resolvedAddress = serviceAddressResolver.resolveHostPort(host, port, serviceId);
            } catch (ServiceAddressResolvingException e) {
                handleException(e, attemptCount);
            }
            attemptCount++;
        }
        return resolvedAddress;
    }

    private void handleException(ServiceAddressResolvingException e, int attemptCount) throws ServiceAddressResolvingException {
        if (attemptCount == MAX_ATTEMPT_COUNT - 1) {
            throw e;
        } else {
            try {
                LOGGER.warn("Unsuccessful address resolving: {}, retrying in {}millis", e.getMessage(), SLEEPTIME);
                Thread.sleep(SLEEPTIME);
            } catch (InterruptedException ie) {
                LOGGER.warn("Interrupted exception occurred.", ie.getMessage());
            }
        }
    }
}
