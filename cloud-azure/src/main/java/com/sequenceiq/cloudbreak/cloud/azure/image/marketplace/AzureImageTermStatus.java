package com.sequenceiq.cloudbreak.cloud.azure.image.marketplace;

public enum AzureImageTermStatus {

    ACCEPTED,
    NOT_ACCEPTED,
    NON_READABLE;

    public static AzureImageTermStatus parseFromBoolean(boolean value) {
        return value ? ACCEPTED : NOT_ACCEPTED;
    }

    public boolean getAsBoolean() {
        return this == AzureImageTermStatus.ACCEPTED;
    }

}
