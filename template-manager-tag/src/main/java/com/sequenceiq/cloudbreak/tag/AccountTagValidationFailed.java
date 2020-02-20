package com.sequenceiq.cloudbreak.tag;

import com.sequenceiq.cloudbreak.common.exception.CloudbreakServiceException;

public class AccountTagValidationFailed extends CloudbreakServiceException {

    public AccountTagValidationFailed(String message) {
        super(message);
    }
}
