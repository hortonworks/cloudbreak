package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.audit.CredentialAuditGrpcServiceAssertion;
import com.sequenceiq.it.cloudbreak.assertion.credential.CredentialTestAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class CredentialsTest extends AbstractMockTest {

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private CredentialTestAssertion credentialTestAssertion;

    @Inject
    private CredentialAuditGrpcServiceAssertion credentialAuditGrpcServiceAssertion;

    @Override
    protected void setupTest(TestContext testContext) {

    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak",
            when = "create/modify/delete a credential;",
            then = "validate the events")
    public void testCredentialEvents(TestContext testContext) {
        createDefaultUser(testContext);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .when(credentialTestClient.modify())
                .when(credentialTestClient.delete())
                .then(credentialTestAssertion.checkStructuredEvents())
                .then(credentialAuditGrpcServiceAssertion::create)
                .then(credentialAuditGrpcServiceAssertion::delete)
                .then(credentialAuditGrpcServiceAssertion::modify)
                .validate();
    }
}
