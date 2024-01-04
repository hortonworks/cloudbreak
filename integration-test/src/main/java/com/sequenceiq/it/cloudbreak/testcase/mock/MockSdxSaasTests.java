package com.sequenceiq.it.cloudbreak.testcase.mock;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxSaasTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxSaasTestDto;

public class MockSdxSaasTests extends AbstractMockTest {

    @Inject
    private SdxSaasTestClient sdxSaasTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultEnvironment(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX Saas Create request is sent",
            then = "SDX should be available AND deletable"
    )
    public void testSdxSaasCreation(TestContext testContext) {
        createDefaultFreeIpa(testContext);
        testContext
                .given(SdxSaasTestDto.class)
                .when(sdxSaasTestClient.create())
                .given(DistroXTestDto.class)
                .when(distroXTestClient.create())
                .awaitForFlow()
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.cascadingDelete())
                .await(EnvironmentStatus.ARCHIVED)
                .validate();
    }
}
