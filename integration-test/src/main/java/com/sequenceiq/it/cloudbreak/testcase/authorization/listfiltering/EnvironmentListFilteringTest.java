package com.sequenceiq.it.cloudbreak.testcase.authorization.listfiltering;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.SimpleEnvironmentResponse;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;

public class EnvironmentListFilteringTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private CloudbreakActor actor;

    @Inject
    private ResourceCreator resourceCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_A);
        //hacky way to let access to image catalog
        initializeDefaultBlueprints(testContext);
        resourceCreator.createDefaultImageCatalog(testContext);
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_B);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are environments",
            when = "users share with each other",
            then = "they see the other's environment in the list")
    public void testEnvironmentListFiltering(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_A);
        resourceCreator.createDefaultCredential(testContext);
        EnvironmentTestDto environmentA = resourceCreator.createDefaultEnvironment(testContext);

        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_B);
        CredentialTestDto credential = resourceCreator.createNewCredential(testContext);
        EnvironmentTestDto environmentB = resourceCreator.createNewEnvironment(testContext, credential);

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, environmentA.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, environmentB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, environmentA.getName(), environmentB.getName());

        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_A, environmentB.getName());
        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_B, environmentA.getName());

        testContext.given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .validate();

        testContext.given(UmsTestDto.class)
                .assignTarget(environmentB.getName())
                .withEnvironmentUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_A))
                .validate();

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, environmentA.getName(), environmentB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, environmentA.getName(), environmentB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, environmentA.getName(), environmentB.getName());

        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
    }

    private void assertUserDoesNotSeeAnyOf(TestContext testContext, String user, String... names) {
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.list(), RunningParameter.who(actor.useRealUmsUser(user)))
                .then((tc, dto, client) -> {
                    Assertions.assertThat(dto.getResponseSimpleEnvSet()
                            .stream()
                            .map(SimpleEnvironmentResponse::getName)
                            .collect(Collectors.toList()))
                            .doesNotContainAnyElementsOf(Arrays.asList(names));
                    return dto;
                });
    }

    private void assertUserSeesAll(TestContext testContext, String user, String... names) {
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.list(), RunningParameter.who(actor.useRealUmsUser(user)))
                .then((tc, dto, client) -> {
                    Assertions.assertThat(dto.getResponseSimpleEnvSet()
                            .stream()
                            .map(SimpleEnvironmentResponse::getName)
                            .collect(Collectors.toList()))
                            .containsAll(Arrays.asList(names));
                    return dto;
                });
    }
}
