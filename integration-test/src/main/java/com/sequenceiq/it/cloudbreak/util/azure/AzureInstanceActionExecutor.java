package com.sequenceiq.it.cloudbreak.util.azure;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sequenceiq.it.cloudbreak.util.wait.CompletableWaitUtil;

import rx.Completable;

public class AzureInstanceActionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(AzureInstanceActionExecutor.class);

    private final List<String> instanceIds;

    private final Function<String, Completable> instanceAction;

    private final Function<String, AzureInstanceActionResult> getInstanceActionResult;

    private final int timeoutValue;

    private final TimeUnit timeoutUnit;

    private AzureInstanceActionExecutor(
            List<String> instanceIds,
            Function<String, Completable> instanceAction,
            Function<String, AzureInstanceActionResult> getInstanceActionResult,
            int timeoutValue,
            TimeUnit timeoutUnit) {
        this.instanceAction = instanceAction;
        this.getInstanceActionResult = getInstanceActionResult;
        this.instanceIds = instanceIds;
        this.timeoutValue = timeoutValue;
        this.timeoutUnit = timeoutUnit;
    }

    public void execute() {
        new CompletableWaitUtil(
                Completable.merge(instanceIds.stream().map(instanceAction).collect(Collectors.toList())),
                timeoutValue,
                timeoutUnit,
                () -> instanceIds.parallelStream()
                        .map(getInstanceActionResult)
                        .collect(Collectors.toList())
        ).doWait();

    }

    public static AzureInstanceExecutorBuilder builder() {
        return new AzureInstanceExecutorBuilder();
    }

    public static class AzureInstanceExecutorBuilder {

        private Function<String, Completable> instanceAction;

        private Function<String, AzureInstanceActionResult> instanceStatusCheck;

        private List<String> instanceIds;

        private int timeoutValue;

        private TimeUnit timeoutUnit;

        /**
         * Required
         *
         * @param instanceIds ids of instances to execute the action on
         * @return the builder
         */
        public AzureInstanceExecutorBuilder onInstances(List<String> instanceIds) {
            this.instanceIds = instanceIds;
            return this;
        }

        /**
         * Required
         *
         * @param action to carry out on the instances
         * @return the builder
         */
        public AzureInstanceExecutorBuilder withInstanceAction(Function<String, Completable> action) {
            this.instanceAction = action;
            return this;
        }

        /**
         * Required
         *
         * @param getInstanceActionResult - checks if the action has reached the desired end state
         * @return the builder
         */
        public AzureInstanceExecutorBuilder withInstanceStatusCheck(Function<String, AzureInstanceActionResult> getInstanceActionResult) {
            this.instanceStatusCheck = getInstanceActionResult;
            return this;
        }

        public AzureInstanceExecutorBuilder withTimeout(int timeout, TimeUnit timeoutUnit) {
            this.timeoutValue = timeout;
            this.timeoutUnit = timeoutUnit;
            return this;
        }

        /**
         * Will create the AzureInstanceExecutor.
         * Required arguments: instanceIds, instanceAction, instanceStatusCheck
         *
         * @return AzureInstanceExecutor
         * @throws NullPointerException if any of the required arguments are missing
         */
        public AzureInstanceActionExecutor build() {
            if (instanceIds == null || instanceAction == null || instanceStatusCheck == null) {
                throw new NullPointerException("Arguments of AzureInstaneExecutor should not be null");
            }
            return new AzureInstanceActionExecutor(instanceIds, instanceAction, instanceStatusCheck, timeoutValue, timeoutUnit);
        }
    }
}
