package com.sequenceiq.freeipa.entity.projection;

import com.sequenceiq.freeipa.entity.StackAuthentication;

public interface StackAuthenticationView {

    Long getStackId();

    StackAuthentication getStackAuthentication();
}
