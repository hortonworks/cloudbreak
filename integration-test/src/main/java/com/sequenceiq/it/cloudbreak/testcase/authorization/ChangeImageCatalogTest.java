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

    @Inject
    private ResourceCreator resourceCreator;

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

        //ENV_CREATOR_A can change FreeIPA image catalog in case of FreeIPA and is created by ENV_CREATOR_A
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

        //ACCOUNT_ADMIN can change FreeIPA image catalog in case of FreeIPA is created by ENV_CREATOR_A
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
        //ENV_CREATOR_B can't change FreeIPA image catalog in case of FreeIPA is created by ENV_CREATOR_A
                .whenException(freeIpaTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/changeFreeipaImageCatalog' right on environment .*")
                        .withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_B)))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataLake and image catalogs created by ENV_CREATOR_A",
            when = "a change image catalog request is sent to use an image catalog created by ENV_CREATOR_A",
            then = "ENV_CREATOR_A, ACCOUNT_ADMIN and ENV_CREATOR_B with env admin right can perform the operation")
    public void testChangeDataLakeImageCatalog(MockedTestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
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
                .await(EnvironmentStatus.AVAILABLE)
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .when(sdxTestClient.detailedDescribeInternal())
                .given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalog1.getName())
                .when(sdxTestClient.changeImageCatalog())
                .validate();

        //ACCOUNT_ADMIN can change DL image catalog in case of DL and target image catalog are created by ENV_CREATOR_A
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        testContext.given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalog2.getName())
                .when(sdxTestClient.changeImageCatalog())
                .validate();

        //ENV_CREATOR_B can change DL image catalog in case of DL and target image catalog are created by ENV_CREATOR_A and
        //  ENV_CREATOR_B has environment admin right in terms of the environment created by ENV_CREATOR_A and
        //  ENV_CREATOR_B has shared resource right in terms of the image catalog created by ENV_CREATOR_A
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        testContext.given(UmsTestDto.class)
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
            given = "there is a running DataLake and image catalog created by ENV_CREATOR_A",
            when = "a change image catalog request is sent",
            then = "ENV_CREATOR_B should get forbidden excepion by using ENV_CREATOR_B's image catalog")
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

        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        ImageCatalogTestDto imageCatalog = resourceCreator.createNewImageCatalog(testContext);

        testContext.given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalog.getName())
                .whenException(sdxTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have " +
                                "'datalake/changeImageCatalog' right on any of the environment[(]s[)] " +
                                "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]] or on .*"))
                .validate();
    }


    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataHub and image catalogs created by ENV_CREATOR_A",
            when = "a change image catalog request is sent to use an image catalog created by ENV_CREATOR_A",
            then = "ENV_CREATOR_A, ACCOUNT_ADMIN and ENV_CREATOR_B with shared resource user and environment admin rights can perform the operation")
    public void testChangeDataHubImageCatalog(MockedTestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_A);
        createDefaultImageCatalog(testContext);

        ImageCatalogTestDto imageCatalog1 = resourceCreator.createNewImageCatalog(testContext);
        ImageCatalogTestDto imageCatalog2 = resourceCreator.createNewImageCatalog(testContext);
        ImageCatalogTestDto imageCatalog3 = resourceCreator.createNewImageCatalog(testContext);

        //ENV_CREATOR_A can change DH image catalog in case of DH and target image catalog are created by ENV_CREATOR_A
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
                .await(STACK_AVAILABLE, RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ACCOUNT_ADMIN)))
                .given(DistroXChangeImageCatalogTestDto.class)
                    .withImageCatalog(imageCatalog1.getName())
                .when(distroXClient.changeImageCatalog())
                .validate();

        //ENV_CREATOR_B can change DH image catalog in case of DH and target image catalog are created by ENV_CREATOR_A and
        //  ENV_CREATOR_B has environment admin right in terms of the environment created by ENV_CREATOR_A and
        //  ENV_CREATOR_B has shared resource right in terms of the image catalog created by ENV_CREATOR_A
        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        testContext.given(UmsTestDto.class)
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
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
        testContext.given(DistroXChangeImageCatalogTestDto.class)
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
                .await(STACK_AVAILABLE, RunningParameter.who(cloudbreakActor.useRealUmsUser(AuthUserKeys.ACCOUNT_ADMIN)))
                .validate();

        ImageCatalogTestDto imageCatalogA = resourceCreator.createNewImageCatalog(testContext);

        useRealUmsUser(testContext, AuthUserKeys.ENV_CREATOR_B);
        ImageCatalogTestDto imageCatalogB = resourceCreator.createNewImageCatalog(testContext);

        //ENV_CREATOR_B can't change DH image catalog in case of DH is created by ENV_CREATOR_A
        testContext.given(DistroXChangeImageCatalogTestDto.class)
                .withImageCatalog(imageCatalogB.getName())
                .whenException(distroXClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have " +
                                "'datahub/changeImageCatalog' right on any of the environment[(]s[)] " +
                                "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]] or on .*"))
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
                        expectedMessage("Doesn't have " +
                                "'environments/useSharedResource' right on imageCatalog .*"))
                .validate();
    }
}
