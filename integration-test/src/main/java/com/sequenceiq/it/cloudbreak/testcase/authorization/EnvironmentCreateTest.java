package com.sequenceiq.it.cloudbreak.testcase.authorization;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class EnvironmentCreateTest extends AbstractIntegrationTest {

    private static final Set<String> INVALID_REGION = new HashSet<>(Collections.singletonList("MockRegion"));

    private static final String BASE_USER = "CB-AccountAdmin";

    private static final String ZERO_RIGHTS = "CB-zero-roles";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.BASE_USER);
        useRealUmsUser(testContext, AuthUserKeys.MGMT_CONSOLE_ADMIN_B);
        useRealUmsUser(testContext, AuthUserKeys.MGMT_CONSOLE_ADMIN_A);
        useRealUmsUser(testContext, AuthUserKeys.ZERO_RIGHTS);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running env service",
            when = "valid create environment request is sent",
            then = "environment should be created but unauthorized users should not be able to access it")
    public void testCreateEnvironment(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.MGMT_CONSOLE_ADMIN_A);
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .when(environmentTestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_B)))
                .when(environmentTestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.BASE_USER)))
//                .await(EnvironmentStatus.AVAILABLE)
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform environments/describeEnvironment on resource crn:cdp.*")
                                .withKey("EnvironmentGetAction"))
                .when(environmentTestClient.describe(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.ZERO_RIGHTS)))
                .expect(ForbiddenException.class,
                        RunningParameter.expectedMessage("You have no right to perform environments/describeEnvironment on resource crn:cdp.*")
                                .withKey("EnvironmentGetAction"))
//                .when(environmentTestClient.delete(), RunningParameter.who(Actor.useRealUmsUser(AuthUserKeys.MGMT_CONSOLE_ADMIN_A)))
//                .await(EnvironmentStatus.ARCHIVED)
                .validate();
    }

}