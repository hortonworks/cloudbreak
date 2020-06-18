package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.BackoffCancellablePollingStrategy.getBackoffCancellablePollingStrategy;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class WaiterRunner {
    private WaiterRunner() {
    }

    public static <I extends AmazonWebServiceRequest> void run(Waiter<I> waiter, I input, CancellationCheck cancellationCheck) {
        try {
            waiter.run(new WaiterParameters<I>()
                    .withRequest(input)
                    .withPollingStrategy(getBackoffCancellablePollingStrategy(cancellationCheck))
            );
        } catch (Exception e) {
            throw new CloudConnectorException(e.getMessage(), e);
        }
    }
}
