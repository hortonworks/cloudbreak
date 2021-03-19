package com.sequenceiq.it.cloudbreak.testcase.authorization.listfiltering;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;

public class DataHubListFilteringTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

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
        testContext.given(UmsTestDto.class)
                .assignTarget(ImageCatalogTestDto.class.getSimpleName())
                .withSharedResourceUser()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there are datahubs",
            when = "users share with each other",
            then = "they see the other's datahub in the list")
    public void testDataHubListFiltering(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_A);
        resourceCreator.createDefaultCredential(testContext);
        EnvironmentTestDto environment = resourceCreator.createDefaultEnvironment(testContext);
        resourceCreator.createNewFreeIpa(testContext, environment);
        resourceCreator.createDefaultDataLake(testContext);
        DistroXTestDto datahubA = resourceCreator.createDefaultDataHubAndWaitAs(testContext, Optional.of(AuthUserKeys.USER_ACCOUNT_ADMIN));

        testContext.given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withDatahubCreator()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .withEnvironmentUser()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .validate();

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, datahubA.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, datahubA.getName());
        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_B, datahubA.getName());

        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_B);
        DistroXTestDto dataHubB = resourceCreator.createNewDataHubAndWaitAs(testContext, Optional.of(AuthUserKeys.USER_ACCOUNT_ADMIN));

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, datahubA.getName(), dataHubB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, datahubA.getName(), dataHubB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, dataHubB.getName());
        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_B, datahubA.getName());

        testContext.given(UmsTestDto.class)
                .assignTarget(DistroXTestDto.class.getSimpleName())
                .withDatahubAdmin()
                .when(environmentTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .validate();

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, datahubA.getName(), dataHubB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, datahubA.getName(), dataHubB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, datahubA.getName(), dataHubB.getName());

        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
    }

    private void assertUserDoesNotSeeAnyOf(TestContext testContext, String user, String... names) {
        useRealUmsUser(testContext, user);
        Assertions.assertThat(testContext.given(DistroXTestDto.class).getAll(testContext.getMicroserviceClient(CloudbreakClient.class))
                .stream()
                .map(StackV4Response::getName)
                .collect(Collectors.toList()))
                .doesNotContainAnyElementsOf(Arrays.asList(names));
    }

    private void assertUserSeesAll(TestContext testContext, String user, String... names) {
        useRealUmsUser(testContext, user);
        List<String> visibleNames = testContext.given(DistroXTestDto.class).getAll(testContext.getMicroserviceClient(CloudbreakClient.class))
                .stream()
                .map(StackV4Response::getName)
                .collect(Collectors.toList());
        Assertions.assertThat(visibleNames).containsAll(Arrays.asList(names));
    }
}
