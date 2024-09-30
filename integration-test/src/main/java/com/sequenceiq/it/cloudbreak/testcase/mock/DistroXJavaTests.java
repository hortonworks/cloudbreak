package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;

public class DistroXJavaTests extends AbstractMockTest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatalake(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "there is a running Cloudbreak, and an environment with SDX and DistroX cluster in available state",
            when = "set default Java version on the DistroX cluster",
            then = "DistroX default Java version should be set, the cluster should be up and running")
    public void testSetDefaultJavaVersion(TestContext testContext) {

        String distroXName = resourcePropertyProvider().getName();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .withJavaVersion(8)
                .when(distroXTestClient.create(), key(distroXName))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .resetCalls()
                .when(distroXTestClient.setDefaultJavaVersion("17", true), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .mockSalt().run().post().bodyContains(Set.of("fun=cmd.run",
                        URLEncoder.encode("rm /var/log/set-default-java-version-executed", StandardCharsets.UTF_8)), 1).times(1).verify()
                .mockSalt().run().post().bodyContains("state.highstate", 1).times(1).verify()
                .then((testContext1, testDto, client) -> {
                    if (testDto.getResponse().getJavaVersion() != 17) {
                        throw new TestFailException("Java version is not 17");
                    }
                    return testDto;
                })
                .validate();
    }
}
