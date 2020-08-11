package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.credential.CredentialTestAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class CredentialsTest extends AbstractIntegrationTest {

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private CredentialTestAssertion credentialTestAssertion;

    @Override
    protected void setupTest(TestContext testContext) {

    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent, then a valid force delete request is sent",
            then = "the environment should be deleted")
    public void testCredentialEvents(TestContext testContext) {
        createDefaultUser(testContext);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .when(credentialTestClient.modify())
                .when(credentialTestClient.delete())
                .then(credentialTestAssertion.checkStructuredEvents())
                .validate();
    }
}
