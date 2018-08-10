package com.sequenceiq.cloudbreak.cloud.credential;

import com.sequenceiq.cloudbreak.cloud.context.CloudContext;
import com.sequenceiq.cloudbreak.cloud.model.ExtendedCloudCredential;

/**
 * Created by perdos on 11/17/16.
 */
public interface CredentialNotifier {

    void createCredential(CloudContext cloudContext, ExtendedCloudCredential extendedCloudCredential);

    void sendStatusMessage(CloudContext cloudContext, ExtendedCloudCredential cloudCredential, boolean error, String errorMessage);
}
