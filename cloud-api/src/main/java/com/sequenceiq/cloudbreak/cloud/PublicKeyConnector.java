package com.sequenceiq.cloudbreak.cloud;

import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyRegisterRequest;
import com.sequenceiq.cloudbreak.cloud.model.publickey.PublicKeyUnregisterRequest;

/**
 * Public key connector
 */
public interface PublicKeyConnector extends CloudPlatformAware {

    void register(PublicKeyRegisterRequest request);

    void unregister(PublicKeyUnregisterRequest request);

}
