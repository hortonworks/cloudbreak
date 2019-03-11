package com.sequenceiq.it.cloudbreak.newway.assertion;

import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.entity.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class CheckEnvironmentCredential implements AssertionV2<EnvironmentTestDto> {
    private String expectedCredentialName;

    public CheckEnvironmentCredential(String expectedCredentialName) {
        this.expectedCredentialName = expectedCredentialName;
    }

    @Override
    public EnvironmentTestDto doAssertion(TestContext testContext, EnvironmentTestDto environment, CloudbreakClient cloudbreakClient) throws Exception {
        String credentialName = environment.getResponse().getCredentialName();
        if (!credentialName.equals(expectedCredentialName)) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return environment;
    }
}
