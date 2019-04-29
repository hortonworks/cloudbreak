package com.sequenceiq.environment.config.registry;

import static com.sequenceiq.environment.util.Validation.notNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RetryingServiceAddressResolver implements ServiceAddressResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(RetryingServiceAddressResolver.class);

    private static final int SLEEP_TIME = 2000;

    private final ServiceAddressResolver serviceAddressResolver;

    private int maxRetryCount;

    public RetryingServiceAddressResolver(ServiceAddressResolver serviceAddressResolver, int timeoutInMillis) {
        notNull(serviceAddressResolver, "serviceAddressResolver");
        this.serviceAddressResolver = serviceAddressResolver;
        maxRetryCount = timeoutInMillis / SLEEP_TIME;
        if (maxRetryCount <= 0) {
            maxRetryCount = 1;
        }
    }

    @Override
    public String resolveUrl(String serverUrl, String protocol, String serviceId) throws ServiceAddressResolvingException {
        int attemptCount = 0;
        String resolvedAddress = null;
        while (resolvedAddress == null && attemptCount < maxRetryCount) {
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
        while (resolvedAddress == null && attemptCount < maxRetryCount) {
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
        if (attemptCount == maxRetryCount - 1) {
            throw e;
        } else {
            try {
                LOGGER.debug("Unsuccessful address resolving: {}, retrying in {}millis", e.getMessage(), SLEEP_TIME);
                Thread.sleep(SLEEP_TIME);
            } catch (InterruptedException ie) {
                LOGGER.info("Interrupted exception occurred: {}", ie.getMessage());
            }
        }
    }

}
