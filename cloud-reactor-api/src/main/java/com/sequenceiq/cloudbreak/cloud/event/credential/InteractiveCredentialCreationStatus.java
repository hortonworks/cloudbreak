package com.sequenceiq.cloudbreak.cloud.event.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.event.CloudPlatformRequest;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

/**
 * Created by perdos on 11/17/16.
 */
public class InteractiveCredentialCreationStatus extends CloudPlatformRequest<InteractiveCredentialCreationRequest> {

    private final ExtendedCloudCredential extendedCloudCredential;

    private final boolean error;

    private final String message;

    public InteractiveCredentialCreationStatus(boolean error, String message, CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential) {
        super(cloudContext, extendedCloudCredential);
        this.error = error;
        this.extendedCloudCredential = extendedCloudCredential;
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public boolean isError() {
        return error;
    }

    public ExtendedCloudCredential getExtendedCloudCredential() {
        return extendedCloudCredential;
    }
}
