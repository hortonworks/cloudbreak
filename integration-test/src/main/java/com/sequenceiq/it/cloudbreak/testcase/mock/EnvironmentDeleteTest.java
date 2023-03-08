package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateRetryAction;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;

public class EnvironmentDeleteTest extends AbstractMockTest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "valid create environment request is sent",
            then = "environment should be created")
    public void testCreateEnvironmentVol2(MockedTestContext testContext) {
        CloudbreakUser user1 = cloudbreakActor.create("originalTenant", "originalUser");
        CloudbreakUser user2 = cloudbreakActor.create("someOtherTenant", "someOtherUser");
        testContext
                .as(user1)
                .given(ImageCatalogTestDto.class)
                .when(new ImageCatalogCreateRetryAction())
                .init(BlueprintTestDto.class)
                .when(blueprintTestClient.listV4())
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                .given("network1", EnvironmentNetworkTestDto.class)
                .withMock(null)
                .given("env1", EnvironmentTestDto.class)
                .withNetwork("network1")
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.list())
                .getTestContext()

                .as(user2)
                .given(ImageCatalogTestDto.class)
                .when(new ImageCatalogCreateRetryAction())
                .init(BlueprintTestDto.class)
                .when(blueprintTestClient.listV4())
                .given(CredentialTestDto.class)
                .when(credentialTestClient.create())
                //.given("network1", EnvironmentNetworkTestDto.class)
                .given(EnvironmentNetworkTestDto.class)
                .given("env2", EnvironmentTestDto.class)
                //.withNetwork("network1")
                .withNetwork()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.list())
                .getTestContext()

                .as(user1)
                .given("env1", EnvironmentTestDto.class)
                .when(environmentTestClient.delete())
                .validate();
    }

    protected void createDefaultCredential(MockedTestContext testContext) {
        testContext.given(CredentialTestDto.class)
                .when(credentialTestClient.create());
    }

    protected void createDefaultImageCatalog(MockedTestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(new ImageCatalogCreateRetryAction());
    }

    protected void createUser(MockedTestContext testContext, String tenant, String user) {
        testContext.as(cloudbreakActor.create(tenant, user));
    }

    protected void initializeDefaultBlueprints(MockedTestContext testContext) {
        testContext
                .init(BlueprintTestDto.class)
                .when(blueprintTestClient.listV4());
    }

}
