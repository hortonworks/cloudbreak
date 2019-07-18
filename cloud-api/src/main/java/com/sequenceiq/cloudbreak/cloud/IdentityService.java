package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.CloudCredential;

public interface IdentityService extends CloudPlatformAware {

    String getAccountId(CloudCredential cloudCredential);

}
