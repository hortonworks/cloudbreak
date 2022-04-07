package com.sequenceiq.cloudbreak.service.upgrade.sync.operationresult;

import java.util.ArrayList;
import java.util.List;

public class CmSyncOperationStatus {

    private final boolean success;

    private final String message;

    private CmSyncOperationStatus(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static CmSyncOperationStatus ofSuccess(String message) {
        return new CmSyncOperationStatus(true, message);
    }

    public static CmSyncOperationStatus ofError(String message) {
        return new CmSyncOperationStatus(false, message);
    }

    public boolean hasSucceeded() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "CmSyncOperationStatus{" +
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

        public CmSyncOperationStatus build() {
            String message = String.join(" ", messages);
            return success
                    ? CmSyncOperationStatus.ofSuccess(message)
                    : CmSyncOperationStatus.ofError(message);
        }

        boolean isSuccess() {
            return success;
        }

        List<String> getMessages() {
            return messages;
        }
    }

}
