package com.sequenceiq.cloudbreak.cloud.mock;

import org.springframework.stereotype.Component;

import com.sequenceiq.cloudbreak.cloud.context.AuthenticatedContext;

@Component
public class MockStackUtil {
    public String getStackName(AuthenticatedContext ac) {
        return ac.getCloudContext().getCrn();
    }
}
