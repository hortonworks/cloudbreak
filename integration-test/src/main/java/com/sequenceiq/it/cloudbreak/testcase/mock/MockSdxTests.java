package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.io.IOException;

import javax.inject.Inject;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.util.ResourceUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MockSdxTests extends AbstractIntegrationTest {

    private static final String TEMPLATE_JSON = "classpath:/templates/sdx-cluster-template.json";

    @Inject
    private SdxTestClient sdxTestClient;

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironmentWithNetwork(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX Internal Create request is sent",
            then = "SDX should be available AND deletable"
    )
    public void testDefaultSDXCanBeCreatedThenDeletedSuccessfully(MockedTestContext testContext) throws IOException {
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String envKey = "sdxEnvKey";
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        String networkKey = "someNetwork";

        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(envKey, EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class).withCluster(cluster)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .withEnvironmentKey(key(envKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> sdxTestClient.deleteInternal().action(tc, testDto, client))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.DELETED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX Internal Create request is sent with Cluster Template",
            then = "SDX should be available AND deletable"
    )
    public void testSDXFromTemplateCanBeCreatedThenDeletedSuccessfully(MockedTestContext testContext) throws IOException {
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        String networkKey = "someOtherNetwork";
        String envKey = "sdxEnvKey";
        JSONObject jsonObject = ResourceUtil.readResourceAsJson(applicationContext, TEMPLATE_JSON);

        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(envKey, EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class).withCluster(cluster)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .given(sdxInternal, SdxInternalTestDto.class)
                .withTemplate(jsonObject)
                .withEnvironmentKey(key(envKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> sdxTestClient.deleteInternal().action(tc, testDto, client))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.DELETED)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "start an sdx cluster",
            then = "SDX should be available"
    )
    public void testSdxStopStart(MockedTestContext testContext) throws IOException {
        createSdx(testContext)
                .when(sdxTestClient.stopInternal())
                .awaitForFlow(key(resourcePropertyProvider().getName()))
                .await(SdxClusterStatusResponse.STOPPED)
                .when(sdxTestClient.startInternal())
                .awaitForFlow(key(resourcePropertyProvider().getName()))
                .await(SdxClusterStatusResponse.RUNNING)
                .validate();
    }

    private SdxInternalTestDto createSdx(MockedTestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        String networkKey = "someOtherNetwork";
        String envKey = "sdxEnvKey";
        return testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(envKey, EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withClouderaManager(clouderaManager)
                .given(stack, StackTestDto.class).withCluster(cluster)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .withEnvironmentKey(key(envKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING);
    }
}
