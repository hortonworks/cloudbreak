package com.sequenceiq.it.cloudbreak.testcase.authorization.listfiltering;

import java.util.Arrays;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.credential.model.response.CredentialResponse;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;

public class CredentialListFilteringTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private ResourceCreator resourceCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);

        testContext.as(AuthUserKeys.USER_ACCOUNT_ADMIN);
        testContext.as(AuthUserKeys.USER_ENV_CREATOR_A);
        testContext.as(AuthUserKeys.USER_ENV_CREATOR_B);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are credentials",
            when = "users share with each other",
            then = "they see the other's credential in the list")
    public void testCredentialListFiltering(TestContext testContext) {
        testContext.as(AuthUserKeys.USER_ENV_CREATOR_A);
        CredentialTestDto credentialA = resourceCreator.createDefaultCredential(testContext);

        testContext.as(AuthUserKeys.USER_ENV_CREATOR_B);
        CredentialTestDto credentialB = resourceCreator.createNewCredential(testContext);

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, credentialA.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, credentialB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, credentialA.getName(), credentialB.getName());

        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_A, credentialB.getName());
        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_B, credentialA.getName());

        testContext.given(UmsTestDto.class)
                .assignTarget(CredentialTestDto.class.getSimpleName())
                .withSharedResourceUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .validate();

        testContext.given(UmsTestDto.class)
                .assignTarget(credentialB.getName())
                .withSharedResourceUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_A))
                .validate();

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, credentialA.getName(), credentialB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, credentialA.getName(), credentialB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, credentialA.getName(), credentialB.getName());

        testContext.as(AuthUserKeys.USER_ACCOUNT_ADMIN);
    }

    private void assertUserDoesNotSeeAnyOf(TestContext testContext, String user, String... names) {
        testContext.given(CredentialTestDto.class)
                .when(credentialTestClient.list(), RunningParameter.who(testContext.getTestUsers().getUserByLabel(user)))
                .then((tc, dto, client) -> {
                    Assertions.assertThat(dto.getResponses()
                            .stream()
                            .map(CredentialResponse::getName)
                            .collect(Collectors.toList()))
                            .doesNotContainAnyElementsOf(Arrays.asList(names));
                    return dto;
                });
    }

    private void assertUserSeesAll(TestContext testContext, String user, String... names) {
        testContext.given(CredentialTestDto.class)
                .when(credentialTestClient.list(), RunningParameter.who(testContext.getTestUsers().getUserByLabel(user)))
                .then((tc, dto, client) -> {
                    Assertions.assertThat(dto.getResponses()
                            .stream()
                            .map(CredentialResponse::getName)
                            .collect(Collectors.toList()))
                            .containsAll(Arrays.asList(names));
                    return dto;
                });
    }
}
