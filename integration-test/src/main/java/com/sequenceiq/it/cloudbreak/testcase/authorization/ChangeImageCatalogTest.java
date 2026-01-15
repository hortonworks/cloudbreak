package com.sequenceiq.it.cloudbreak.testcase.authorization;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ACCOUNT_ADMIN;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ENV_CREATOR_A;
import static com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys.ENV_CREATOR_B;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.environmentDatalakePattern;
import static com.sequenceiq.it.cloudbreak.util.AuthorizationTestUtil.environmentFreeIpaPattern;

import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.RunningParameter;
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
import com.sequenceiq.it.cloudbreak.util.ResourceCreator;

public class ChangeImageCatalogTest extends AbstractIntegrationTest {

    @Inject
    private UmsTestClient umsTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private ResourceCreator resourceCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getTestUsers().setSelector(TestUserSelectors.UMS_ONLY);
        testContext.as(AuthUserKeys.ACCOUNT_ADMIN);
        testContext.as(ENV_CREATOR_B);
        testContext.as(ENV_CREATOR_A);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Freeipa created by ENV_CREATOR_A",
            when = "a change image catalog request is sent",
            then = "ACCOUNT_ADMIN and ENV_CREATOR_A can perform the operation but ENV_CREATOR_B should get forbidden exception")
    public void testChangeFreeipaImageCatalog(MockedTestContext testContext) {
        //ENV_CREATOR_A can change FreeIPA image catalog in case of FreeIPA and is created by ENV_CREATOR_A
        testContext
                .as(ENV_CREATOR_A)
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .when(freeIpaTestClient.describe())
                .given(FreeipaChangeImageCatalogTestDto.class)
                    .withImageCatalog(testContext.given(FreeIpaTestDto.class)
                            .getResponse()
                            .getImage()
                            .getCatalog() + "&changed=true")
                .when(freeIpaTestClient.changeImageCatalog())
                .validate();

        //ACCOUNT_ADMIN can change FreeIPA image catalog in case of FreeIPA is created by ENV_CREATOR_A
        testContext
                .as(ACCOUNT_ADMIN)
                .given(FreeipaChangeImageCatalogTestDto.class)
                .withImageCatalog(testContext.given(FreeIpaTestDto.class)
                        .getResponse()
                        .getImage()
                        .getCatalog() + "&changed=true&index=2")
                .when(freeIpaTestClient.changeImageCatalog())
                    .withImageCatalog(testContext.given(FreeIpaTestDto.class)
                            .getResponse()
                            .getImage()
                            .getCatalog() + "&changed=true&index=3")
        //ENV_CREATOR_B can't change FreeIPA image catalog in case of FreeIPA is created by ENV_CREATOR_A
                .whenException(freeIpaTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/changeFreeipaImageCatalog' right on environment "
                        + environmentFreeIpaPattern(testContext)).withWho(testContext.getTestUsers().getUserByLabel(AuthUserKeys.ENV_CREATOR_B)))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataLake and image catalogs created by ENV_CREATOR_A",
            when = "a change image catalog request is sent to use an image catalog created by ENV_CREATOR_A",
            then = "ENV_CREATOR_A, ACCOUNT_ADMIN and ENV_CREATOR_B with shared resource user and environment admin right can perform the operation")
    public void testChangeDataLakeImageCatalog(MockedTestContext testContext) {
        testContext.as(ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);

        ImageCatalogTestDto imageCatalog1 = resourceCreator.createNewImageCatalog(testContext);
        ImageCatalogTestDto imageCatalog2 = resourceCreator.createNewImageCatalog(testContext);
        ImageCatalogTestDto imageCatalog3 = resourceCreator.createNewImageCatalog(testContext);

        //ENV_CREATOR_A can change DL image catalog in case of DL and target image catalog are created by ENV_CREATOR_A
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .when(sdxTestClient.detailedDescribeInternal())
                .given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalog1.getName())
                .when(sdxTestClient.changeImageCatalog())
                .validate();

        //ACCOUNT_ADMIN can change DL image catalog in case of DL and target image catalog are created by ENV_CREATOR_A
        testContext.as(ACCOUNT_ADMIN)
                .given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalog2.getName())
                .when(sdxTestClient.changeImageCatalog())
                .validate();

        //ENV_CREATOR_B can change DL image catalog in case of DL and target image catalog are created by ENV_CREATOR_A and
        //  ENV_CREATOR_B has environment admin right in terms of the environment created by ENV_CREATOR_A and
        //  ENV_CREATOR_B has shared resource right in terms of the image catalog created by ENV_CREATOR_A
        testContext.as(ENV_CREATOR_B)
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(UmsTestDto.class)
                .assignTarget(imageCatalog3.getName())
                .withSharedResourceUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalog3.getName())
                .when(sdxTestClient.changeImageCatalog())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataLake created by ENV_CREATOR_A and image catalogs created by ENV_CREATOR_A and ENV_CREATOR_B",
            when = "a change image catalog request is sent",
            then = "ENV_CREATOR_B should get forbidden excepion by using any image catalog")
    public void testChangeDataLakeImageCatalogFails(MockedTestContext testContext) {
        useRealUmsUser(testContext, ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .when(sdxTestClient.detailedDescribeInternal())
                .validate();

        ImageCatalogTestDto imageCatalogA = resourceCreator.createNewImageCatalog(testContext);

        testContext.as(AuthUserKeys.ENV_CREATOR_B);
        ImageCatalogTestDto imageCatalogB = resourceCreator.createNewImageCatalog(testContext);

        //ENV_CREATOR_B can't change DL image catalog in case of DL is created by ENV_CREATOR_A
        testContext.given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalogB.getName())
                .whenException(sdxTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'datalake/changeImageCatalog' right on any of the environment[(]s[)] " +
                                environmentDatalakePattern(testContext) + " or on .*"))
                .validate();

        //ENV_CREATOR_B can't change ENV_CREATOR_A's DH image catalog in case of having environment admin right but the catalog is created by ENV_CREATOR_A
        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalogA.getName())
                .whenException(sdxTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/useSharedResource' right on imageCatalog .*"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataHub and image catalogs created by ENV_CREATOR_A",
            when = "a change image catalog request is sent to use an image catalog created by ENV_CREATOR_A",
            then = "ENV_CREATOR_A, ACCOUNT_ADMIN and ENV_CREATOR_B with shared resource user and environment admin rights can perform the operation")
    public void testChangeDataHubImageCatalog(MockedTestContext testContext) {
        testContext.as(ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);

        ImageCatalogTestDto imageCatalog1 = resourceCreator.createNewImageCatalog(testContext);
        ImageCatalogTestDto imageCatalog2 = resourceCreator.createNewImageCatalog(testContext);
        ImageCatalogTestDto imageCatalog3 = resourceCreator.createNewImageCatalog(testContext);

        //ENV_CREATOR_A can change DH image catalog in case of DH and target image catalog are created by ENV_CREATOR_A
        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withTelemetryDisabled()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .when(sdxTestClient.detailedDescribeInternal())
                .given(DistroXTestDto.class)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE, RunningParameter.who(testContext.getTestUsers().getUserByLabel((AuthUserKeys.ACCOUNT_ADMIN))))
                .given(DistroXChangeImageCatalogTestDto.class)
                    .withImageCatalog(imageCatalog1.getName())
                .when(distroXClient.changeImageCatalog())
                .validate();

        //ENV_CREATOR_B can change DH image catalog in case of DH and target image catalog are created by ENV_CREATOR_A and
        //  ENV_CREATOR_B has environment admin right in terms of the environment created by ENV_CREATOR_A and
        //  ENV_CREATOR_B has shared resource right in terms of the image catalog created by ENV_CREATOR_A
        testContext.as(ENV_CREATOR_B)
                .given(UmsTestDto.class)
                .assignTarget(imageCatalog2.getName())
                .withSharedResourceUser()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(DistroXChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalog2.getName())
                .when(distroXClient.changeImageCatalog())
                .validate();

        //ACCOUNT_ADMIN can change DH image catalog in case of DH and target image catalog are created by ENV_CREATOR_A
        testContext.as(ACCOUNT_ADMIN)
                .given(DistroXChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalog3.getName())
                .when(distroXClient.changeImageCatalog())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataHub created by ENV_CREATOR_A and image catalogs created by ENV_CREATOR_A and ENV_CREATOR_B",
            when = "a change image catalog request is sent",
            then = "ENV_CREATOR_B should get forbidden excepion by using any image catalog")
    public void testChangeDataHubImageCatalogFails(MockedTestContext testContext) {
        testContext.as(ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);

        testContext
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given(EnvironmentTestDto.class)
                .withTelemetryDisabled()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .when(sdxTestClient.detailedDescribeInternal())
                .given(DistroXTestDto.class)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE, RunningParameter.who(testContext.getTestUsers().getUserByLabel((AuthUserKeys.ACCOUNT_ADMIN))))
                .validate();

        ImageCatalogTestDto imageCatalogA = resourceCreator.createNewImageCatalog(testContext);

        testContext.as(AuthUserKeys.ENV_CREATOR_B);
        ImageCatalogTestDto imageCatalogB = resourceCreator.createNewImageCatalog(testContext);

        //ENV_CREATOR_B can't change DH image catalog in case of DH is created by ENV_CREATOR_A
        testContext.given(DistroXChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalogB.getName())
                .whenException(distroXClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have " +
                                "'datahub/changeImageCatalog' right on any of the environment[(]s[)] " +
                                "[\\[]crn: crn:cdp:environments:.*:.*:environment:.*[]] or on .*"))
                .validate();

        //ENV_CREATOR_B can't change ENV_CREATOR_A's DH image catalog in case of having environment admin right but the catalog is created by ENV_CREATOR_A
        testContext
                .given(UmsTestDto.class)
                .assignTarget(EnvironmentTestDto.class.getSimpleName())
                .withEnvironmentAdmin()
                .when(umsTestClient.assignResourceRole(AuthUserKeys.ENV_CREATOR_B))
                .given(DistroXChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalogA.getName())
                .whenException(distroXClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/useSharedResource' right on imageCatalog .*"))
                .validate();
    }
}
