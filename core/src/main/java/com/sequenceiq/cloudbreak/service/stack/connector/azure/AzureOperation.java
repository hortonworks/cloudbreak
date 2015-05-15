package com.sequenceiq.cloudbreak.service.stack.connector.azure;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import com.sequenceiq.cloudbreak.domain.AzureCredential;
import com.sequenceiq.cloudbreak.domain.Stack;
import com.sequenceiq.cloudbreak.service.events.CloudbreakEventService;
import com.sequenceiq.cloudbreak.service.stack.connector.ParallelCloudResourceManager;
import com.sequenceiq.cloudbreak.service.stack.resource.azure.builders.AzureResourceBuilderInit;

public abstract class AzureOperation<T> {
    private CloudbreakEventService cloudbreakEventService;
    private Map<String, Lock> lockMap;
    private AzureResourceBuilderInit azureResourceBuilderInit;
    private ParallelCloudResourceManager cloudResourceManager;
    private Stack stack;
    private boolean queued;

    protected AzureOperation(Builder builder) {
        this.azureResourceBuilderInit = builder.azureResourceBuilderInit;
        this.cloudResourceManager = builder.cloudResourceManager;
        this.cloudbreakEventService = builder.cloudbreakEventService;
        this.lockMap = builder.lockMap;
        this.stack = builder.stack;
        this.queued = builder.queued;
    }

    public T execute() {
        if (queued) {
            Lock lock = getLock(stack);
            try {
                boolean lockSuccess = lock.tryLock();
                if (!lockSuccess) {
                    cloudbreakEventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(),
                            "Waiting for other azure stack operation with the same subscriptionId to be finished.");
                    lock.lock();
                    cloudbreakEventService.fireCloudbreakEvent(stack.getId(), stack.getStatus().name(),
                            "Continue: " + stack.getStatusReason());
                }
                return doExecute(stack);
            } finally {
                lock.unlock();
                removeLock(stack, lock);
            }
        } else {
            return doExecute(stack);
        }
    }

    protected abstract T doExecute(Stack stack);

    protected AzureResourceBuilderInit getAzureResourceBuilderInit() {
        return azureResourceBuilderInit;
    }

    protected ParallelCloudResourceManager getCloudResourceManager() {
        return cloudResourceManager;
    }

    private Lock getLock(Stack stack) {
        synchronized (lockMap) {
            String subscriptionId = ((AzureCredential) stack.getCredential()).getSubscriptionId();
            Lock lock = lockMap.get(subscriptionId);
            if (lock == null) {
                lock = new ReentrantLock(true);
                lockMap.put(subscriptionId, lock);
            }
            return lock;
        }
    }

    private void removeLock(Stack stack, Lock lock) {
        if (lock.tryLock()) {
            lockMap.remove(((AzureCredential) stack.getCredential()).getSubscriptionId());
            lock.unlock();
        }
    }

    public static class Builder {
        private CloudbreakEventService cloudbreakEventService;
        private Map<String, Lock> lockMap;
        private AzureResourceBuilderInit azureResourceBuilderInit;
        private ParallelCloudResourceManager cloudResourceManager;
        private Stack stack;
        private boolean queued = true;

        public Builder withCloudbreakEventService(CloudbreakEventService cloudbreakEventService) {
            this.cloudbreakEventService = cloudbreakEventService;
            return this;
        }

        public Builder withLockMap(Map<String, Lock> lockMap) {
            this.lockMap = lockMap;
            return this;
        }

        public Builder withAzureResourceBuilderInit(AzureResourceBuilderInit azureResourceBuilderInit) {
            this.azureResourceBuilderInit = azureResourceBuilderInit;
            return this;
        }

        public Builder withCloudResourceManager(ParallelCloudResourceManager cloudResourceManager) {
            this.cloudResourceManager = cloudResourceManager;
            return this;
        }

        public Builder withStack(Stack stack) {
            this.stack = stack;
            return this;
        }

        public Builder withQueued(boolean queued) {
            this.queued = queued;
            return this;
        }
    }
}
