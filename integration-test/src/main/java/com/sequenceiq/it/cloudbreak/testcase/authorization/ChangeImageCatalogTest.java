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
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class ChangeImageCatalogTest extends AbstractIntegrationTest {

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
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Freeipa",
            when = "a change image catalog request is sent",
            then = "admin user can perform the operation but env creator user should get forbidden exception")
    public void testChangeFreeipaImageCatalog(MockedTestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);

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
                .whenException(freeIpaTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'environments/changeFreeipaImageCatalog' right on environment .*")
                        .withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataLake",
            when = "a change image catalog request is sent",
            then = "admin user can perform the operation but env creator user should get forbidden exception")
    public void testChangeDataLakeImageCatalog(MockedTestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
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
                .when(sdxTestClient.detailedDescribeInternal());

        String newImageCatalogName = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class));

        testContext.given(SdxChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalogName)
                .when(sdxTestClient.changeImageCatalog())
                .whenException(sdxTestClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have " +
                                "'datalake/changeImageCatalog' right on any of the environment[(]s[)] " +
                                "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]] or on .*")
                                .withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DataHub",
            when = "a change image catalog request is sent",
            then = "admin user can perform the operation but env creator user should get forbidden exception")
    public void testChangeDataHubImageCatalog(MockedTestContext testContext) {
        useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
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
                .when(distroXClient.getInternal());

        String newImageCatalogName = createNewImageCatalog(testContext, testContext.get(ImageCatalogTestDto.class));

        testContext.given(DistroXChangeImageCatalogTestDto.class)
                .withImageCatalog(newImageCatalogName)
                .when(distroXClient.changeImageCatalog())
                .whenException(distroXClient.changeImageCatalog(), ForbiddenException.class,
                        expectedMessage("Doesn't have 'datahub/changeImageCatalog' right on any of the environment[(]s[)] " +
                                "[\\[]crn: crn:cdp:environments:us-west-1:.*:environment:.*[]] or on .*")
                                .withWho(cloudbreakActor.useRealUmsUser(AuthUserKeys.ENV_CREATOR_A)))
                .validate();
    }

    private String createNewImageCatalog(MockedTestContext testContext, ImageCatalogTestDto imageCatalogTestDto) {
        final String newImageCatalogName = imageCatalogTestDto.getResponse().getName() + "-changed";
        final String newImageCatalogUrl = imageCatalogTestDto.getResponse().getUrl() + "&changed=true";
        testContext.given(ImageCatalogTestDto.class)
                .withName(newImageCatalogName)
                .withUrl(newImageCatalogUrl)
                .when(imageCatalogTestClient.createV4());
        return newImageCatalogName;
    }
}
