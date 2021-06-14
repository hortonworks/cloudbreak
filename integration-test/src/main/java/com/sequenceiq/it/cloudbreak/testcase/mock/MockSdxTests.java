package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.IDBROKER;
import static com.sequenceiq.it.cloudbreak.cloud.HostGroupType.MASTER;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.customdomain.CustomDomainSettingsV4Request;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.cloud.HostGroupType;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCustomTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.util.SdxUtil;
import com.sequenceiq.it.util.ResourceUtil;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class MockSdxTests extends AbstractMockTest {

    private static final String TEMPLATE_JSON = "classpath:/templates/sdx-cluster-template.json";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private SdxUtil sdxUtil;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

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
            when = "a valid SDX Custom Create request is sent",
            then = "SDX should be available AND deletable"
    )
    public void testCustomSDXCanBeCreatedThenDeletedSuccessfully(MockedTestContext testContext) {
        String sdxCustom = resourcePropertyProvider().getName();
        String envKey = "sdxEnvKey";
        String networkKey = "someNetwork";

        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(envKey, EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create(), key(envKey))
                .await(EnvironmentStatus.AVAILABLE, key(envKey))
                .given(sdxCustom, SdxCustomTestDto.class)
                .withEnvironmentKey(key(envKey))
                .when(sdxTestClient.createCustom(), key(sdxCustom))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxCustom))
                .then((tc, testDto, client) -> sdxTestClient.deleteCustom().action(tc, testDto, client))
                .await(SdxClusterStatusResponse.DELETED, key(sdxCustom))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "a valid SDX Internal Create request is sent",
            then = "SDX should be available AND deletable"
    )
    public void testDefaultSDXCanBeCreatedThenDeletedSuccessfully(MockedTestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String envKey = "sdxEnvKey";
        String networkKey = "someNetwork";

        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(envKey, EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create(), key(envKey))
                .await(EnvironmentStatus.AVAILABLE, key(envKey))
                .given(sdxInternal, SdxInternalTestDto.class)
                .withEnvironmentKey(key(envKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> sdxTestClient.deleteInternal().action(tc, testDto, client))
                .await(SdxClusterStatusResponse.DELETED, key(sdxInternal))
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
                .when(getEnvironmentTestClient().create(), key(envKey))
                .await(EnvironmentStatus.AVAILABLE, key(envKey))
                .given(sdxInternal, SdxInternalTestDto.class)
                .withTemplate(jsonObject)
                .withEnvironmentKey(key(envKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> sdxTestClient.deleteInternal().action(tc, testDto, client))
                .await(SdxClusterStatusResponse.DELETED, key(sdxInternal))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running CloudSdxInternalTestDtobreak",
            when = "start an sdx cluster with different CM and CDH versions",
            then = "versions should be correct"
    )
    public void testSdxCreateWithDifferentCmAndCdhVersions(MockedTestContext testContext) {
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        String imageSettings = "imageSettingsUpgrade";

        String cmVersion = "7.3.0";
        String cdhVersion = "7.2.7";

        testContext
                .given(ImageCatalogTestDto.class)
                .withName(upgradeImageCatalogName)
                .withUrl(getImageCatalogMockServerSetup().getPreWarmedImageCatalogUrlWithCmAndCdhVersions(cmVersion, cdhVersion))
                .when(imageCatalogTestClient.createV4(), key(upgradeImageCatalogName))
                .given(EnvironmentTestDto.class)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withClouderaManager(clouderaManager)
                .given(imageSettings, ImageSettingsTestDto.class)
                .withImageId("f6e778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(upgradeImageCatalogName)
                .given(stack, StackTestDto.class)
                .withCluster(cluster)
                .withImageSettings(imageSettings)
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then((tc, testDto, client) -> {
                    SdxClusterDetailResponse sdx = testDto.getResponse();
                    assertEquals(sdx.getRuntime(), cdhVersion);
                    assertEquals(sdx.getStackV4Response().getCluster().getCm().getRepository().getVersion(), cmVersion);
                    return testDto;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "start an sdx cluster",
            then = "SDX should be available"
    )
    public void testSdxStopStart(MockedTestContext testContext) {
        String sdxInternal = resourcePropertyProvider().getName();
        String networkKey = "someOtherNetwork";
        String envKey = "sdxEnvKey";

        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(envKey, EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create(), key(envKey))
                .await(EnvironmentStatus.AVAILABLE, key(envKey))
                .given(sdxInternal, SdxInternalTestDto.class)
                .withEnvironmentKey(key(envKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .when(sdxTestClient.stopInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.STOPPED, key(sdxInternal))
                .when(sdxTestClient.startInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "terminate instances and repair an sdx cluster",
            then = "SDX should be available"
    )
    public void repairTerminatedMasterAndIdbroker(MockedTestContext testContext) {
        testRepair(testContext,
                List.of(MASTER, IDBROKER),
                instanceBasePath -> getExecuteQueryToMockInfrastructure().call(instanceBasePath + "/terminate", w -> w),
                SdxClusterStatusResponse.DELETED_ON_PROVIDER_SIDE);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "terminate instances and repair an sdx cluster",
            then = "SDX should be available"
    )
    public void repairTerminatedMaster(MockedTestContext testContext) {
        testRepair(testContext,
                List.of(MASTER),
                instanceBasePath -> getExecuteQueryToMockInfrastructure().call(instanceBasePath + "/terminate", w -> w),
                SdxClusterStatusResponse.CLUSTER_AMBIGUOUS);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "stop and repair an sdx cluster",
            then = "SDX should be available"
    )
    public void repairStoppedMasterAndIdbroker(MockedTestContext testContext) {
        testRepair(testContext,
                List.of(MASTER, IDBROKER),
                instanceBasePath -> getExecuteQueryToMockInfrastructure().call(instanceBasePath + "/stop", w -> w),
                SdxClusterStatusResponse.STOPPED);
    }

    public void testRepair(MockedTestContext testContext,
            List<HostGroupType> hostGroups,
            Consumer<String> actionOnNode,
            SdxClusterStatusResponse stateBeforeRepair
    ) {
        String sdxInternal = resourcePropertyProvider().getName();
        String networkKey = "someOtherNetwork";
        String envKey = "sdxEnvKey";

        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        CustomDomainSettingsV4Request customDomain = new CustomDomainSettingsV4Request();
        customDomain.setDomainName("dummydomainname");
        customDomain.setHostname("dummyhostname");
        customDomain.setClusterNameAsSubdomain(true);
        customDomain.setHostgroupNameAsHostname(true);
        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(envKey, EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create(), key(envKey))
                .await(EnvironmentStatus.AVAILABLE, key(envKey))
                .given(sdxInternal, SdxInternalTestDto.class)
                .withDatabase(sdxDatabaseRequest)
                .withCustomDomain(customDomain)
                .withEnvironmentKey(key(envKey))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> {
                    List<String> instancesToDelete = new ArrayList<>();
                    for (HostGroupType hostGroupType : hostGroups) {
                        instancesToDelete.addAll(sdxUtil.getInstanceIds(testDto, client, hostGroupType.getName()));
                    }
                    instancesToDelete.forEach(instanceId -> actionOnNode.accept("/" + testDto.getCrn() + "/spi/" + instanceId));
                    return testDto;
                })
                .await(stateBeforeRepair)
                .when(sdxTestClient.repairInternal(hostGroups.stream().map(HostGroupType::getName).toArray(String[]::new)), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .validate();
    }
}
