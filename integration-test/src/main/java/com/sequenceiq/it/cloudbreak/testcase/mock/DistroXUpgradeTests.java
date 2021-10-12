package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXUpgradeTestDto;

public class DistroXUpgradeTests extends AbstractMockTest {

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
        createDefaultEnvironment(testContext);
        createDatalake(testContext);
        createDefaultFreeIpa(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "there is a running Cloudbreak, and an environment with SDX and DistroX cluster in available state",
            when = "upgrade called on the DistroX cluster",
            then = "DistroX upgrade should be successful, the cluster should be up and running")
    public void testDistroXUpgrade(TestContext testContext) {

        String distroXName = resourcePropertyProvider().getName();
        String targetRuntimeVersion = getNextRuntimeVersion(commonClusterManagerProperties.getRuntimeVersion());
        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.create(), key(distroXName))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .given(DistroXUpgradeTestDto.class)
                .withRuntime(targetRuntimeVersion)
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.upgrade(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .validate();
    }

    private String getNextRuntimeVersion(String runtime) {
        String[] split = runtime.split("\\.");
        int last = Integer.parseInt(split[split.length - 1]);
        List<String> elements = new ArrayList<>(Arrays.asList(split).subList(0, split.length - 1));
        elements.add(String.valueOf(last + 1));
        return String.join(".", elements);
    }
}
