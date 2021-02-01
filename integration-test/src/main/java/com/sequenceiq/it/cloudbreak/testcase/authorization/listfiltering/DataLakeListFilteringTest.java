package com.sequenceiq.it.cloudbreak.testcase.authorization.listfiltering;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;
import com.sequenceiq.sdx.api.model.SdxClusterResponse;

public class DataLakeListFilteringTest extends AbstractIntegrationTest {

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
            given = "there are datalakes",
            when = "users share with each other",
            then = "they see the other's datalake in the list")
    public void testDataLakeListFiltering(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_A);
        resourceCreator.createDefaultCredential(testContext);
        resourceCreator.createDefaultEnvironment(testContext);
        SdxInternalTestDto dataLakeA = resourceCreator.createDefaultDataLake(testContext);

        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_B);
        CredentialTestDto credential = resourceCreator.createNewCredential(testContext);
        EnvironmentTestDto environmentB = resourceCreator.createNewEnvironment(testContext, credential);
        SdxInternalTestDto dataLakeB = resourceCreator.createNewDataLake(testContext, environmentB);

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, dataLakeA.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, dataLakeB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, dataLakeA.getName(), dataLakeB.getName());

        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_A, dataLakeB.getName());
        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_B, dataLakeA.getName());

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

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, dataLakeA.getName(), dataLakeB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, dataLakeA.getName(), dataLakeB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, dataLakeA.getName(), dataLakeB.getName());

        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
    }

    private void assertUserDoesNotSeeAnyOf(TestContext testContext, String user, String... names) {
        useRealUmsUser(testContext, user);
        Assertions.assertThat(testContext.given(SdxInternalTestDto.class)
                .getAll(testContext.getMicroserviceClient(SdxClient.class))
                .stream()
                .map(SdxClusterResponse::getName)
                .collect(Collectors.toList()))
                .doesNotContainAnyElementsOf(Arrays.asList(names));
    }

    private void assertUserSeesAll(TestContext testContext, String user, String... names) {
        useRealUmsUser(testContext, user);
        List<String> visibleNames = testContext.given(SdxInternalTestDto.class)
                .getAll(testContext.getMicroserviceClient(SdxClient.class))
                .stream()
                .map(SdxClusterResponse::getName)
                .collect(Collectors.toList());
        Assertions.assertThat(visibleNames).containsAll(Arrays.asList(names));
    }
}
