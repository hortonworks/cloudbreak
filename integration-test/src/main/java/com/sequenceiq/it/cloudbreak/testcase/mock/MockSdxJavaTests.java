package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MockSdxJavaTests extends AbstractMockTest {

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX Custom Create request is sent",
            then = "set default java version to 17"
    )
    public void testSetDefaultJavaVersion(MockedTestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();

        testContext
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
                .given(sdxInternal, SdxInternalTestDto.class)
                .withJavaVersion(8)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .enableVerification()
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .resetCalls()
                .when(sdxTestClient.setDefaultJavaVersion("17", true, true), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .mockSalt().run().post().bodyContains(Set.of("fun=cmd.run",
                        URLEncoder.encode("rm /var/log/set-default-java-version-executed", StandardCharsets.UTF_8)), 1).times(1).verify()
                .mockSalt().run().post().bodyContains("state.highstate", 1).times(1).verify()
                .mockSalt().run().post().bodyContains(Set.of("state.apply", "cloudera.manager.restart"), 1).times(1).verify()
                .then((testContext1, testDto, client) -> {
                    if (testDto.getResponse().getStackV4Response().getJavaVersion() != 17) {
                        throw new TestFailException("Java version is not 17");
                    }
                    return testDto;
                })
                .validate();
    }
}
