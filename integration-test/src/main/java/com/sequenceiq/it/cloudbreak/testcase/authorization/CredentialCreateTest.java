package com.sequenceiq.it.cloudbreak.testcase.authorization;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class CredentialCreateTest extends AbstractIntegrationTest {

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    private static final String BASE_USER = "CB-AccountAdmin";

    private static final String ZERO_RIGHTS = "CB-zero-roles";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {

    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request is sent with no region in it",
            then = "a BadRequestException should be returned")
    public void testCreateCredentialWithManagementConsoleAdmin(TestContext testContext) {
        useRealUmsUser(testContext, BASE_USER);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a create environment request is sent with no region in it",
            then = "a BadRequestException should be returned")
    public void testCreateCredentialWithZeroRoles(TestContext testContext) {
        useRealUmsUser(testContext, ZERO_RIGHTS);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create(), RunningParameter.key("Unauthorized"))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform " +
                                "environments/createCredential in account 460c0d8f-ae8e-4dce-9cd7-2351762eb9ac")
                                .withKey("Unauthorized"))
                .validate();
    }
}