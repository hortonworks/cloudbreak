package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.config.user.TestUserCreator;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MockSdxBlueprintLoadTests extends AbstractMockTest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private TestUserCreator testUserCreator;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.as(testUserCreator.create("sdxbploadtenant10", "sdxbpload@cloudera.com"));
        createDefaultCredential(testContext);
        createEnvironmentWithFreeIpa(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX request is sent",
            then = "SDX should be available AND deletable without loading blueprints explicitly"
    )
    public void testCustomSDXCanBeCreatedThenDeletedSuccessfully(MockedTestContext testContext) {
        testContext.given(SdxCustomTestDto.class)
                .when(sdxTestClient.createCustom())
                .await(SdxClusterStatusResponse.RUNNING)
                .validate();
    }
}
