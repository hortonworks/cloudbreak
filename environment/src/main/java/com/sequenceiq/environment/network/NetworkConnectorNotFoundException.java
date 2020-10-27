package com.sequenceiq.environment.network;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class NetworkConnectorNotFoundException extends CloudbreakServiceException {

    public NetworkConnectorNotFoundException(String message) {
        super(message);
    }

    public NetworkConnectorNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NetworkConnectorNotFoundException(Throwable cause) {
        super(cause);
    }
}
