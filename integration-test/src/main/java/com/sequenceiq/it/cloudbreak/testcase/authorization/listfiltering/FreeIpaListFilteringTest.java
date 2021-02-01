package com.sequenceiq.it.cloudbreak.testcase.authorization.listfiltering;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.list.ListFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys;
import com.sequenceiq.it.cloudbreak.testcase.mock.AbstractMockTest;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;

public class FreeIpaListFilteringTest extends AbstractMockTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ResourceCreator resourceCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_B);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are freeipas",
            when = "users share with each other",
            then = "they see the other's freeipa in the list")
    public void testFreeIpaListFiltering(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_A);
        resourceCreator.createDefaultCredential(testContext);
        EnvironmentTestDto environmentA = resourceCreator.createDefaultEnvironment(testContext);
        resourceCreator.createDefaultFreeIpa(testContext);

        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_B);
        CredentialTestDto credential = resourceCreator.createNewCredential(testContext);
        EnvironmentTestDto environmentB = resourceCreator.createNewEnvironment(testContext, credential);
        resourceCreator.createNewFreeIpa(testContext, environmentB);

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, environmentA.getCrn());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, environmentB.getCrn());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, environmentA.getCrn(), environmentB.getCrn());

        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_A, environmentB.getCrn());
        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_B, environmentA.getCrn());

        testContext.given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .validate();

        testContext.given(UmsTestDto.class)
                .assignTarget(environmentB.getName())
                .withEnvironmentUser()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_A))
                .validate();

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, environmentA.getCrn(), environmentB.getCrn());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, environmentA.getCrn(), environmentB.getCrn());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, environmentA.getCrn(), environmentB.getCrn());

        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
    }

    private void assertUserDoesNotSeeAnyOf(TestContext testContext, String user, String... envCrns) {
        useRealUmsUser(testContext, user);
        Assertions.assertThat(testContext.given(FreeIpaTestDto.class)
                .getAll(testContext.getMicroserviceClient(FreeIpaClient.class))
                .stream()
                .map(ListFreeIpaResponse::getName)
                .collect(Collectors.toList()))
                .doesNotContainAnyElementsOf(Arrays.asList(envCrns));
    }

    private void assertUserSeesAll(TestContext testContext, String user, String... envCrns) {
        useRealUmsUser(testContext, user);
        List<String> visibleEnvCrns = testContext.given(FreeIpaTestDto.class)
                .getAll(testContext.getMicroserviceClient(FreeIpaClient.class))
                .stream()
                .map(ListFreeIpaResponse::getEnvironmentCrn)
                .collect(Collectors.toList());
        Assertions.assertThat(visibleEnvCrns).containsAll(Arrays.asList(envCrns));
    }
}
