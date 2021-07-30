package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import java.util.ArrayList;
import java.util.List;

public class CmSyncOperationSummary {

    private final boolean success;

    private final String message;

    private CmSyncOperationSummary(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static CmSyncOperationSummary ofSuccess(String message) {
        return new CmSyncOperationSummary(true, message);
    }

    public static CmSyncOperationSummary ofError(String message) {
        return new CmSyncOperationSummary(false, message);
    }

    public boolean hasSucceeded() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "CmSyncOperationSummary{" +
                "success=" + success +
                ", message='" + message + '\'' +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private boolean success = true;

        private final List<String> messages = new ArrayList<>();

        public Builder withSuccess(String message) {
            messages.add(message);
            return this;
        }

        public Builder withError(String message) {
            messages.add(message);
            success = false;
            return this;
        }

        public Builder merge(Builder other) {
            messages.addAll(other.getMessages());
            success = success && other.isSuccess();
            return this;
        }

        public CmSyncOperationSummary build() {
            String message = String.join(" ", messages);
            return success
                    ? CmSyncOperationSummary.ofSuccess(message)
                    : CmSyncOperationSummary.ofError(message);
        }

        boolean isSuccess() {
            return success;
        }

        List<String> getMessages() {
            return messages;
        }
    }

}
