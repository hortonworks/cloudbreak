package com.sequenceiq.cloudbreak.service.credential;

import com.sequenceiq.cloudbreak.domain.CloudPlatform;
import com.sequenceiq.cloudbreak.domain.Credential;

public interface CredentialHandler<T extends Credential> {

    CloudPlatform getCloudPlatform();

    T init(T credential);

    boolean delete(T credential);

    T update(T credential) throws Exception;
}
