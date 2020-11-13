package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;


import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;

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

    public static final String PROFILE_RETURN_HTTP_500 = "cmHttp500";

    private static final BigDecimal DEPLOY_CLIENT_CONFIG_COMMAND_ID = new BigDecimal(100);

    private static final BigDecimal APPLY_HOST_TEMPLATE_COMMAND_ID = new BigDecimal(200);

    private static final String CLOUDERA_MANAGER_KEY = "cm";

    private static final String CLUSTER_KEY = "cmcluster";

    private static final Duration POLLING_INTERVAL = Duration.of(3000, ChronoUnit.MILLIS);

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private StackTestClient stackTestClient;

//    private Integer originalWorkerCount;

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
//        originalWorkerCount = 3;
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

        testContext
                .given(CLOUDERA_MANAGER_KEY, ClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, ClusterTestDto.class)
                .withBlueprintName(blueprintName)
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(CLOUDERA_MANAGER_KEY)
                .given(stack, StackTestDto.class).withCluster(CLUSTER_KEY)
                .withName(clusterName)
                .when(stackTestClient.createV4(), key(stack))
                .awaitForFlow(key(stack))
                .await(STACK_AVAILABLE, key(stack))
                .when(StackScalePostAction.valid().withDesiredCount(desiredWorkerCount).withForced(Boolean.FALSE), key(stack))
                .await(StackTestDto.class, STACK_AVAILABLE, key(stack), POLLING_INTERVAL)
                // TODO Please don't remove, depends on CB-9111
//                .then(MockVerification.verify(POST, ITResponse.MOCK_ROOT + "/cloud_instance_statuses").atLeast(1), key(stack))
//                .then(MockVerification.verify(POST, ITResponse.MOCK_ROOT + "/cloud_metadata_statuses")
//                        .bodyContains("CREATE_REQUESTED", addedNodes).exactTimes(1), key(stack))
//                .then(MockVerification.verify(GET, ITResponse.SALT_BOOT_ROOT + "/health").atLeast(1), key(stack))
//                .then(MockVerification.verify(POST, ITResponse.SALT_BOOT_ROOT + "/salt/action/distribute").atLeast(1), key(stack))
//                .then(MockVerification.verify(POST, ITResponse.SALT_API_ROOT + "/run").bodyContains("fun=network.ipaddrs").atLeast(1), key(stack))
//                .then(MockVerification.verify(POST, ITResponse.SALT_API_ROOT + "/run").bodyContains("fun=saltutil.sync_all").atLeast(1), key(stack))
//                .then(MockVerification.verify(POST, ITResponse.SALT_API_ROOT + "/run").bodyContains("fun=mine.update").atLeast(1), key(stack))
//                .then(MockVerification.verify(POST, ITResponse.SALT_API_ROOT + "/run").bodyContains("fun=state.highstate").atLeast(1), key(stack))
//                .then(MockVerification.verify(POST, ITResponse.SALT_API_ROOT + "/run").bodyContains("fun=grains.remove").atLeast(1), key(stack))
//                .then(MockVerification.verify(GET,
//                        new ClouderaManagerPathResolver(LIST_HOSTS)
//                                .pathVariableMapping(":clusterName", clusterName)
//                                .resolve())
//                        .exactTimes(1), key(stack))
//                .then(MockVerification.verify(GET, READ_HOSTS).atLeast(4), key(stack))
//                .then(MockVerification.verify(POST, new ClouderaManagerPathResolver(ADD_HOSTS)
//                        .pathVariableMapping(":clusterName", clusterName)
//                        .resolve())
//                        .exactTimes(1), key(stack))
//                .then(MockVerification.verify(POST, new ClouderaManagerPathResolver(DEPLOY_CLIENT_CONFIG)
//                        .pathVariableMapping(":clusterName", clusterName)
//                        .resolve())
//                        .exactTimes(1), key(stack))
//                .then(MockVerification.verify(POST, new ClouderaManagerPathResolver(APPLY_HOST_TEMPLATE)
//                        .pathVariableMapping(":clusterName", clusterName)
//                        .pathVariableMapping(":hostTemplateName", "worker")
//                        .resolve())
//                        .exactTimes(1), key(stack))
//                .then(MockVerification.verify(GET, new ClouderaManagerPathResolver(READ_COMMAND)
//                        .pathVariableMapping(":commandId", APPLY_HOST_TEMPLATE_COMMAND_ID.toString())
//                        .resolve())
//                        .exactTimes(1), key(stack))
                .validate();
    }

    @Override
    protected List<String> testProfiles() {
        return List.of(PROFILE_RETURN_HTTP_500);
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }

}
