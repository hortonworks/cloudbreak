package com.sequenceiq.cloudbreak.cloud.exception;

import java.util.StringJoiner;

public class QuotaExceededException extends Exception {

    private final int currentLimit;

    private final int currentUsage;

    private final int additionalRequired;

    private final String quotaErrorMessage;

    public QuotaExceededException(int currentLimit, int currentUsage, int additionalRequired, String quotaErrorMessage, Exception e) {
        super(e);
        this.currentLimit = currentLimit;
        this.currentUsage = currentUsage;
        this.additionalRequired = additionalRequired;
        this.quotaErrorMessage = quotaErrorMessage;
    }

    public int getCurrentLimit() {
        return currentLimit;
    }

    public int getCurrentUsage() {
        return currentUsage;
    }

    public int getAdditionalRequired() {
        return additionalRequired;
    }

    public String getQuotaErrorMessage() {
        return quotaErrorMessage;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", QuotaExceededException.class.getSimpleName() + "[", "]")
                .add("currentLimit=" + currentLimit)
                .add("currentUsage=" + currentUsage)
                .add("additionalRequired=" + additionalRequired)
                .add("quotaErrorMessage='" + quotaErrorMessage + "'")
                .toString();
    }

}
