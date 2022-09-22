package com.sequenceiq.cloudbreak.cloud.aws.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

import software.amazon.awssdk.awscore.AwsResponse;
import software.amazon.awssdk.core.waiters.Waiter;

public class WaiterRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WaiterRunner.class);

    private WaiterRunner() {
    }

    public static <R extends AwsResponse> void run(Supplier<R> pollingFunction, Waiter<R> waiter) {
        run(pollingFunction, waiter, null);
    }

    public static <R extends AwsResponse> void run(Supplier<R> pollingFunction, Waiter<R> waiter, String exceptionMessage) {
        run(pollingFunction, waiter, exceptionMessage, null);
    }

    public static <R extends AwsResponse> void run(Supplier<R> pollingFunction, Waiter<R> waiter, String exceptionMessage, Supplier<String> reasonSupplier) {
        try {
            waiter.run(pollingFunction);
        } catch (Exception e) {
            handleWaiterError(exceptionMessage, reasonSupplier, e);
        }
    }

    public static void handleWaiterError(String errorMessage, Exception exception) {
        handleWaiterError(errorMessage, null, exception);
    }

    public static void handleWaiterError(String errorMessage, Supplier<String> reasonSupplier, Exception exception) {
        LOGGER.error("Exception in AWS waiter: ", exception);
        List<String> messages = new ArrayList<>();
        if (StringUtils.hasText(errorMessage)) {
            messages.add(errorMessage);
        }
        if (StringUtils.hasText(exception.getMessage())) {
            messages.add(exception.getMessage());
        }
        Optional.ofNullable(exception.getCause()).ifPresent(throwable -> messages.add(throwable.getMessage()));
        if (reasonSupplier != null) {
            try {
                messages.add(reasonSupplier.get());
            } catch (Exception ex) {
                LOGGER.warn("Failed to get reason from supplier", ex);
                messages.add("Failed to get reason from supplier: " + ex.getLocalizedMessage());
            }
        }
        throw new CloudConnectorException(String.join(" ", messages), exception);
    }

}
