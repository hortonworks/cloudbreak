package com.sequenceiq.cloudbreak.cloud.aws;

import com.sequenceiq.cloudbreak.cloud.exception.CloudConnectorException;

public class AwsDefaultRegionSelectionFailed extends CloudConnectorException {

    public AwsDefaultRegionSelectionFailed(String message) {
        super(message);
    }

    public AwsDefaultRegionSelectionFailed(String message, Throwable cause) {
        super(message, cause);
    }

    public AwsDefaultRegionSelectionFailed(Throwable cause) {
        super(cause);
    }

    protected AwsDefaultRegionSelectionFailed(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
