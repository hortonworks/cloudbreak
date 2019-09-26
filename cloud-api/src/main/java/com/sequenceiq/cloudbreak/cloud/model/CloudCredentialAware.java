package com.sequenceiq.cloudbreak.cloud.model;

import com.sequenceiq.cloudbreak.cloud.CloudPlatformAware;

public interface CloudCredentialAware extends CloudPlatformAware {

    CloudCredential getCredential();

}
