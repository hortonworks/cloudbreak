package com.sequenceiq.environment.environment.encryption;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class EncryptionResourcesNotFoundException extends CloudbreakServiceException {

    public EncryptionResourcesNotFoundException(String message) {
        super(message);
    }

    public EncryptionResourcesNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public EncryptionResourcesNotFoundException(Throwable cause) {
        super(cause);
    }
}
