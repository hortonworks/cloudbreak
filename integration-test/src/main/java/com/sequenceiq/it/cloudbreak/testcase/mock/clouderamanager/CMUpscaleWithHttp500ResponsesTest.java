package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;


import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

import javax.inject.Inject;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.action.v4.stack.StackScalePostAction;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;

public class CMUpscaleWithHttp500ResponsesTest extends AbstractClouderaManagerTest {

    private static final String CLOUDERA_MANAGER_KEY = "cm";

    private static final String CLUSTER_KEY = "cmcluster";

    private static final Duration POLLING_INTERVAL = Duration.of(3000, ChronoUnit.MILLIS);

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private StackTestClient stackTestClient;

    private Integer originalWorkerCount;

    private Integer desiredWorkerCount;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironmentWithNetwork(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createCmBlueprint(testContext);
    }

    @BeforeMethod
    public void setUp() {
        originalWorkerCount = 3;
        desiredWorkerCount = 15;
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a stack with upscale",
            when = "upscale to 15",
            then = "stack is running")
    public void testUpscale(MockedTestContext testContext) {
        String blueprintName = testContext.get(BlueprintTestDto.class).getRequest().getName();
        String clusterName = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        int addedNodes = desiredWorkerCount - originalWorkerCount;
        testContext
                .given(CLOUDERA_MANAGER_KEY, ClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, ClusterTestDto.class)
                .withBlueprintName(blueprintName)
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(CLOUDERA_MANAGER_KEY)
                .given(stack, StackTestDto.class).withCluster(CLUSTER_KEY)
                .withName(clusterName)
                .when(stackTestClient.createV4(), key(stack))
                .mockCm().profile(PROFILE_RETURN_HTTP_500, 1)
                .await(STACK_AVAILABLE, key(stack))
                .when(StackScalePostAction.valid().withDesiredCount(desiredWorkerCount).withForced(Boolean.FALSE), key(stack))
                .await(StackTestDto.class, STACK_AVAILABLE, key(stack), POLLING_INTERVAL)
                .mockSpi().cloudInstanceStatuses().post().atLeast(1).verify()
                .mockSpi().cloudMetadataStatuses().post().bodyContains("CREATE_REQUESTED", addedNodes).times(1).verify()
                .mockSalt().health().get().atLeast(1).verify()
                .mockSalt().saltActionDistribute().post().atLeast(1).verify()
                .mockSalt().run().post().bodyContains("fun=network.ipaddrs", 1).atLeast(1).verify()
                .mockSalt().run().post().bodyContains("fun=saltutil.sync_all", 1).atLeast(1).verify()
                .mockSalt().run().post().bodyContains("fun=mine.update", 1).atLeast(1).verify()
                .mockSalt().run().post().bodyContains("fun=state.highstate", 1).atLeast(1).verify()
                .mockSalt().run().post().bodyContains("fun=grains.remove", 1).atLeast(1).verify()

                .mockCm().clustersByClusterNameHost().get().pathVariable("clusterName", clusterName).times(2).verify()
                .mockCm().hosts().get().atLeast(3).verify()
                .validate();
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }

}
