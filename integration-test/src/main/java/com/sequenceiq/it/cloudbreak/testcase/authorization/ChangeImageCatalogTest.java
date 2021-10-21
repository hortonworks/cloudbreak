package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeipaChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxChangeImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.ums.UmsTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class ChangeImageCatalogTest extends AbstractIntegrationTest {

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Freeipa created by ENV_CREATOR_A",
            when = "a change image catalog request is sent",
            then = "ACCOUNT_ADMIN and ENV_CREATOR_A can perform the operation but ENV_CREATOR_B should get forbidden exception")
    public void testChangeFreeipaImageCatalog(MockedTestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .when(freeIpaTestClient.describe())
                .given(FreeipaChangeImageCatalogTestDto.class)
                    .withImageCatalog(testContext.given(FreeIpaTestDto.class)
                            .getResponse()
                            .getImage()
                            .getCatalog() + "&changed=true")
                .when(freeIpaTestClient.changeImageCatalog())
                .validate();

        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        testContext.given(FreeipaChangeImageCatalogTestDto.class)
                .withImageCatalog(testContext.given(FreeIpaTestDto.class)
                        .getResponse()
                        .getImage()
                        .getCatalog() + "&changed=true&index=2")
                .when(freeIpaTestClient.changeImageCatalog())
                .withImageCatalog(testContext.given(FreeIpaTestDto.class)
                        .getResponse()
                        .getImage()
                        .getCatalog() + "&changed=true&index=3")
                .whenException(freeIpaTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/changeFreeipaImageCatalog' right on environment .*")
                        .withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataLake and image catalogs created by ENV_CREATOR_A",
            when = "a change image catalog request is sent to use an image catalog created by ENV_CREATOR_A",
            then = "ENV_CREATOR_A, ACCOUNT_ADMIN and ENV_CREATOR_B with env admin role can perform the operation")
    public void testChangeDataLakeImageCatalog(MockedTestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);

        String newImageCatalog1 = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class), 1);
        String newImageCatalog2 = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class), 2);
        String newImageCatalog3 = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class), 3);

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.detailedDescribeInternal())
                .given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalog1)
                .when(sdxTestClient.changeImageCatalog())
                .validate();

        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        testContext.given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalog2)
                .when(sdxTestClient.changeImageCatalog())
                .validate();

        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        testContext.given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .validate();

        testContext.given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalog3)
                .when(sdxTestClient.changeImageCatalog())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataLake and image catalogs created by ENV_CREATOR_A and ENV_CREATOR_B",
            when = "a change image catalog request is sent",
            then = "ENV_CREATOR_A and ENV_CREATOR_B should get forbidden excepion by using ENV_CREATOR_B's image catalog")
    public void testChangeDataLakeImageCatalogFails(MockedTestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.detailedDescribeInternal())
                .validate();

        String newImageCatalogA = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class), 1);

        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        String newImageCatalogB = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class), 2);

        testContext.given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalogA)
                .whenException(sdxTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have " +
                                "'datalake/changeImageCatalog' right on any of the environment[(]s[)] " +
                                "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]] or on .*"))
                .given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalogB)
                .whenException(sdxTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have " +
                                "'datalake/changeImageCatalog' right on any of the environment[(]s[)] " +
                                "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]] or on .*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataHub and image catalogs created by ENV_CREATOR_A",
            when = "a change image catalog request is sent to use a image catalog created by ENV_CREATOR_A",
            then = "ENV_CREATOR_A, ACCOUNT_ADMIN and ENV_CREATOR_B with env admin role can perform the operation")
    public void testChangeDataHubImageCatalog(MockedTestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);

        String newImageCatalog1 = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class), 1);
        String newImageCatalog2 = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class), 2);
        String newImageCatalog3 = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class), 3);

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.detailedDescribeInternal())
                .given(DistroXTestDto.class)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE)
                .when(distroXClient.getInternal())
                .given(DistroXChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalog1)
                .when(distroXClient.changeImageCatalog())
                .validate();

        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        testContext.given(DistroXChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalog2)
                .when(distroXClient.changeImageCatalog())
                .validate();

        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        testContext.given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .validate();

        testContext.given(DistroXChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalog3)
                .when(distroXClient.changeImageCatalog())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataLake and image catalogs created by ENV_CREATOR_A and ENV_CREATOR_B",
            when = "a change image catalog request is sent",
            then = "ENV_CREATOR_A and ENV_CREATOR_B should get forbidden excepion by using ENV_CREATOR_B's image catalog")
    public void testChangeDataHubImageCatalogFails(MockedTestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.detailedDescribeInternal())
                .given(DistroXTestDto.class)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE)
                .when(distroXClient.getInternal())
                .given(DistroXChangeImageCatalogTestDto.class)
                .validate();

        String newImageCatalogA = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class), 1);

        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        String newImageCatalogB = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class), 2);

        testContext.given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalogA)
                .whenException(sdxTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have " +
                                "'datalake/changeImageCatalog' right on any of the environment[(]s[)] " +
                                "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]] or on .*"))
                .given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalogB)
                .whenException(sdxTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have " +
                                "'datalake/changeImageCatalog' right on any of the environment[(]s[)] " +
                                "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]] or on .*"))
                .validate();
    }

    private String createNewImageCatalog(MockedTestContext testContext, ImageCatalogTestDto imageCatalogTestDto, int index) {
        final String newImageCatalogName = imageCatalogTestDto.getResponse().getName() + "-changed" + index;
        final String newImageCatalogUrl = imageCatalogTestDto.getResponse().getUrl() + "&changed=true&index=" + index;
        testContext.given(ImageCatalogTestDto.class)
                .withName(newImageCatalogName)
                .withUrl(newImageCatalogUrl)
                .when(imageCatalogTestClient.createV4());
        return newImageCatalogName;
    }
}
