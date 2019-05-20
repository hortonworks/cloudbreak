package com.sequenceiq.it.cloudbreak.assertion.environment;

import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public class EnvironmentTestAssertion implements Assertion<EnvironmentTestDto> {
    private String expectedCredentialName;

    public EnvironmentTestAssertion(String expectedCredentialName) {
        this.expectedCredentialName = expectedCredentialName;
    }

    @Override
    public EnvironmentTestDto doAssertion(TestContext testContext, EnvironmentTestDto testDto, CloudbreakClient cloudbreakClient) throws Exception {
        String credentialName = testDto.getResponse().getCredentialName();
        if (!credentialName.equals(expectedCredentialName)) {
            throw new TestFailException("Credential is not attached to environment");
        }
        return testDto;
    }
}
