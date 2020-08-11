package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.BackoffCancellablePollingStrategy.getBackoffCancellablePollingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.util.StringUtils;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class WaiterRunner {
    private WaiterRunner() {
    }

    public static <I extends AmazonWebServiceRequest> void run(Waiter<I> waiter, I input, CancellationCheck cancellationCheck) {
        run(waiter, input, cancellationCheck, null);
    }

    public static <I extends AmazonWebServiceRequest> void run(Waiter<I> waiter, I input, CancellationCheck cancellationCheck, String exceptionMessage) {
        try {
            waiter.run(new WaiterParameters<I>()
                    .withRequest(input)
                    .withPollingStrategy(getBackoffCancellablePollingStrategy(cancellationCheck))
            );
        } catch (Exception e) {
            List<String> messages = new ArrayList<>();
            if (StringUtils.hasText(exceptionMessage)) {
                messages.add(exceptionMessage);
            }
            if (StringUtils.hasText(e.getMessage())) {
                messages.add(e.getMessage());
            }
            Optional.ofNullable(e.getCause()).ifPresent(throwable -> messages.add(throwable.getMessage()));
            throw new CloudConnectorException(String.join(" ", messages), e);
        }
    }
}
