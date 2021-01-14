package com.sequenceiq.it.cloudbreak.dto.mock;

import com.sequenceiq.it.cloudbreak.dto.mock.verification.VerificationContext;

public interface Verification {
    void handle(String path, Method method, VerificationContext verificationContext);
}
