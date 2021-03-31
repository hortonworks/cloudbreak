package com.sequenceiq.it.cloudbreak.testcase.authorization.listfiltering;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.imagecatalog.responses.ImageCatalogV4Response;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys;
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;

public class ImageCatalogListFilteringTest extends AbstractIntegrationTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private UmsTestClient umsTestClient;

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
            given = "there are image catalogs",
            when = "users share with each other",
            then = "they see the other's image catalog in the list")
    public void testImageCatalogListFiltering(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_A);
        ImageCatalogTestDto imageCatalogA = resourceCreator.createDefaultImageCatalog(testContext);

        useRealUmsUser(testContext, AuthUserKeys.USER_ENV_CREATOR_B);
        ImageCatalogTestDto imageCatalogB = resourceCreator.createNewImageCatalog(testContext);

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, imageCatalogA.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, imageCatalogB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, imageCatalogA.getName(), imageCatalogB.getName());

        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_A, imageCatalogB.getName());
        assertUserDoesNotSeeAnyOf(testContext, AuthUserKeys.USER_ENV_CREATOR_B, imageCatalogA.getName());

        testContext.given(UmsTestDto.class)
                .assignTarget(ImageCatalogTestDto.class.getSimpleName())
                .withSharedResourceUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_B))
                .validate();

        testContext.given(UmsTestDto.class)
                .assignTarget(imageCatalogB.getName())
                .withSharedResourceUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.USER_ENV_CREATOR_A))
                .validate();

        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_A, imageCatalogA.getName(), imageCatalogB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ENV_CREATOR_B, imageCatalogA.getName(), imageCatalogB.getName());
        assertUserSeesAll(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN, imageCatalogA.getName(), imageCatalogB.getName());

        useRealUmsUser(testContext, AuthUserKeys.USER_ACCOUNT_ADMIN);
    }

    private void assertUserDoesNotSeeAnyOf(TestContext testContext, String user, String... names) {
        useRealUmsUser(testContext, user);
        Assertions.assertThat(testContext.given(ImageCatalogTestDto.class).getAll(testContext.getMicroserviceClient(CloudbreakClient.class))
                .stream()
                .map(ImageCatalogV4Response::getName)
                .collect(Collectors.toList()))
                .doesNotContainAnyElementsOf(Arrays.asList(names));
    }

    private void assertUserSeesAll(TestContext testContext, String user, String... names) {
        useRealUmsUser(testContext, user);
        List<String> visibleNames = testContext.given(ImageCatalogTestDto.class).getAll(testContext.getMicroserviceClient(CloudbreakClient.class))
                .stream()
                .map(ImageCatalogV4Response::getName)
                .collect(Collectors.toList());
        Assertions.assertThat(visibleNames).containsAll(Arrays.asList(names));
    }
}
