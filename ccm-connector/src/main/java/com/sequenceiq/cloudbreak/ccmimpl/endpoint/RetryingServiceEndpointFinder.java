package com.sequenceiq.cloudbreak.ccmimpl.endpoint;

import java.util.Objects;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpoint;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointFinder;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointLookupException;
import com.sequenceiq.cloudbreak.ccm.endpoint.ServiceEndpointRequest;
import com.sequenceiq.cloudbreak.ccmimpl.util.RetryUtil;

/**
 * Service endpoint finders that retries lookups on a delegate finder.
 */
public class RetryingServiceEndpointFinder implements ServiceEndpointFinder {

    private static final Logger LOG = LoggerFactory.getLogger(RetryingServiceEndpointFinder.class);

    /**
     * The delegate service endpoint finder.
     */
    private final ServiceEndpointFinder delegate;

    /**
     * Creates a retrying service endpoint finder with the specified parameters.
     *
     * @param delegate the delegate service endpoint finder
     */
    public RetryingServiceEndpointFinder(@Nonnull ServiceEndpointFinder delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate is null");
    }

    @Nonnull
    @Override
    public <T extends ServiceEndpoint> T getServiceEndpoint(ServiceEndpointRequest<T> serviceEndpointRequest)
            throws ServiceEndpointLookupException, InterruptedException {

        String targetInstanceId = serviceEndpointRequest.getTargetInstance().getTargetInstanceId();
        String actionDescription = "discover service endpoint for instance " + targetInstanceId;

        return RetryUtil.performWithRetries(
                () -> delegate.getServiceEndpoint(serviceEndpointRequest), actionDescription,
                serviceEndpointRequest.getWaitUntilTime().orElse(null), serviceEndpointRequest.getPollingIntervalInMs(),
                ServiceEndpointLookupException.class,
                () -> new ServiceEndpointLookupException(String.format("Timed out while trying to %s", actionDescription), true),
                LOG);
    }
}
