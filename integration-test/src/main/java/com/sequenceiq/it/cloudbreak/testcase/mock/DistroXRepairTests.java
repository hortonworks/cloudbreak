package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.List;

import javax.inject.Inject;

import org.testng.annotations.Test;
import org.testng.collections.Lists;

import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.util.DistroxUtil;

public class DistroXRepairTests extends AbstractMockTest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private DistroxUtil distroxUtil;

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
            when = "repair called on the DistroX cluster",
            then = "DistroX repair should be successful, the cluster should be up and running")
    public void testDistroXInstanceRepair(TestContext testContext) {

        String distroXName = resourcePropertyProvider().getName();
        testContext
                .given(distroXName, DistroXTestDto.class)
                .when(distroXTestClient.create(), key(distroXName))
                .await(STACK_AVAILABLE)
                .awaitForHealthyInstances()
                .then((testContext1, testDto, client) -> {
                    List<String> workerInstanceIds = distroxUtil.getInstanceIds(testDto, client, "worker");
                    List<String> computeInstanceIds = distroxUtil.getInstanceIds(testDto, client, "compute");
                    testDto.setRepairableInstanceIds(Lists.merge(workerInstanceIds, computeInstanceIds));
                    return testDto;
                })
                .when(distroXTestClient.repairInstances(), key(distroXName))
                .await(STACK_AVAILABLE, key(distroXName))
                .awaitForHealthyInstances()
                .validate();
    }
}
