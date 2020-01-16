package com.sequenceiq.it.cloudbreak.assertion.environment;

import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class EnvironmentTestAssertion implements Assertion<EnvironmentTestDto, EnvironmentClient> {
    private String expectedCredentialName;

    public EnvironmentTestAssertion(String expectedCredentialName) {
        this.expectedCredentialName = expectedCredentialName;
    }

    @Override
    public EnvironmentTestDto doAssertion(TestContext testContext, EnvironmentTestDto testDto, EnvironmentClient environmentClient) throws Exception {
        String credentialName = testDto.getResponse().getCredential().getName();
        if (!credentialName.equals(expectedCredentialName)) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return testDto;
    }
}
