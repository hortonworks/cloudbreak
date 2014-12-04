package com.sequenceiq.cloudbreak.service.credential;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;

public interface CredentialHandler<T extends Credential> {

    T init(T credential);

    CloudPlatform getCloudPlatform();
}
