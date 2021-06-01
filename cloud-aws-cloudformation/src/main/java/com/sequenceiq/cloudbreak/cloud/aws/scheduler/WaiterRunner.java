package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import static com.sequenceiq.cloudbreak.cloud.aws.scheduler.BackoffCancellablePollingStrategy.getBackoffCancellablePollingStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.amazonaws.AmazonWebServiceRequest;
import com.amazonaws.waiters.Waiter;
import com.amazonaws.waiters.WaiterParameters;
import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class WaiterRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaiterRunner.class);

    private WaiterRunner() {
    }

    public static <I extends AmazonWebServiceRequest> void run(Waiter<I> waiter, I input, CancellationCheck cancellationCheck) {
        run(waiter, input, cancellationCheck, null);
    }

    public static <I extends AmazonWebServiceRequest> void run(Waiter<I> waiter, I input, CancellationCheck cancellationCheck, String exceptionMessage) {
        run(waiter, input, cancellationCheck, exceptionMessage, null);
    }

    public static <I extends AmazonWebServiceRequest> void run(Waiter<I> waiter, I input, CancellationCheck cancellationCheck, String exceptionMessage,
            Supplier<String> reasonSupplier) {
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
            if (reasonSupplier != null) {
                try {
                    messages.add(reasonSupplier.get());
                } catch (Exception ex) {
                    LOGGER.warn("Failed to get reason from supplier", ex);
                    messages.add("Failed to get reason from supplier: " + ex.getLocalizedMessage());
                }
            }
            throw new CloudConnectorException(String.join(" ", messages), e);
        }
    }
}
