package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.Set;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;

public class CMStartStopWithHttp500ResponsesTest extends AbstractClouderaManagerTest {

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createEnvironmentWithFreeIpaAndDatalake(testContext);
        createCmBlueprint(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a Cloudera Manager cluster",
            when = "the Cloudera Manager responds with HTTP 500 for each API call for the first time, but the cluster is stopped and started",
            then = "the cluster should be available")
    public void testCreateStartStopWithHttp500ErrorsForEachApiCallForTheFirstTime(MockedTestContext testContext) {
        String name = testContext.get(BlueprintTestDto.class).getRequest().getName();
        String cm = resourcePropertyProvider().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();

        testContext
                .given(cm, DistroXClouderaManagerTestDto.class)
                .given(cmcluster, DistroXClusterTestDto.class)
                .withValidateBlueprint(Boolean.FALSE)
                .withBlueprintName(name)
                .withClouderaManager(cm)
                .given(stack, DistroXTestDto.class)
                .withCluster(cmcluster)
                .when(distroXTestClient.create(), key(stack))
                .mockCm().profile(PROFILE_RETURN_HTTP_500, 1)
                .await(STACK_AVAILABLE, key(stack))
                .when(distroXTestClient.stop(), key(stack))
                .await(STACK_STOPPED, key(stack).withIgnoredStatues(Set.of(Status.UNREACHABLE)))
                .when(distroXTestClient.start(), key(stack))
                .await(STACK_AVAILABLE, key(stack).withIgnoredStatues(Set.of(Status.UNREACHABLE)))
                .validate();
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }
}
