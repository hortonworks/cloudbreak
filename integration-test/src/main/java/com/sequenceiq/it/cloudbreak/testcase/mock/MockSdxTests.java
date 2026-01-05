package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.assertion.CBAssertion.assertEquals;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.io.IOException;

import jakarta.inject.Inject;

import org.json.JSONObject;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.InstanceGroupV4Response;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.util.ResourceUtil;
import com.sequenceiq.sdx.api.model.SdxClusterDetailResponse;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MockSdxTests extends AbstractMockTest {

    private static final String TEMPLATE_JSON = "classpath:/templates/sdx-cluster-template.json";

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

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
            then = "SDX should be available AND deletable"
    )
    public void testCustomSDXCanBeCreatedThenDeletedSuccessfully(MockedTestContext testContext) {
        String sdxCustom = resourcePropertyProvider().getName();

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
                .given(sdxCustom, SdxInternalTestDto.class)
                .withInstanceType("xlarge")
                .when(sdxTestClient.createInternal(), key(sdxCustom))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxCustom))
                .then(this::validateInstanceGroupInstanceTypeIsModified)
                .when(sdxTestClient.deleteInternal())
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
        JSONObject jsonObject = ResourceUtil.readResourceAsJson(applicationContext, TEMPLATE_JSON);

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
                .withTemplate(jsonObject)
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING, key(sdxInternal))
                .then((tc, testDto, client) -> sdxTestClient.deleteInternal().action(tc, testDto, client))
                .await(SdxClusterStatusResponse.DELETED, key(sdxInternal))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
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
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.create())
                .awaitForCreationFlow()
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
            when = "an invalid SDX Create request is sent with wrong custom instance type",
            then = "SDX create should be failed"
    )
    public void testSDXCreateWithInvalidCustomInstanceTypesShouldFail(MockedTestContext testContext) {
        String sdx = resourcePropertyProvider().getName();

        testContext
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(sdx, SdxTestDto.class)
                .withCustomInstanceGroup("master", "small")
                .when(sdxTestClient.create(), key(sdx))
                .await(SdxClusterStatusResponse.PROVISIONING_FAILED, key(sdx).withWaitForFlowFail())
                .then(this::validateSdxStatusReason)
                .validate();
    }

    private SdxInternalTestDto validateInstanceGroupInstanceTypeIsModified(TestContext testContext, SdxInternalTestDto testDto, SdxClient client) {
        InstanceGroupV4Response instanceGroupResponse = testDto.getResponse().getStackV4Response().getInstanceGroups().stream()
                .filter(instanceGroup -> "master".equals(instanceGroup.getName()))
                .findAny().orElseThrow(() -> new TestFailException("Could not find master instance group."));
        if (instanceGroupResponse.getTemplate() == null || !"xlarge".equals(instanceGroupResponse.getTemplate().getInstanceType())) {
            throw new TestFailException("Instance type of master instance group is not xlarge!");
        }
        return testDto;
    }

    private SdxTestDto validateSdxStatusReason(TestContext testContext, SdxTestDto testDto, SdxClient client) {
        SdxClusterStatusResponse sdxStatus = testDto.getResponse().getStatus();
        if (!SdxClusterStatusResponse.PROVISIONING_FAILED.equals(sdxStatus)) {
            throw new TestFailException("Sdx status is not PROVISIONING_FAILED, current status: " + sdxStatus);
        }
        String statusReason = testDto.getResponse().getStatusReason();
        if (!"Datalake creation failed. Invalid custom instance type for instance group: master - small".equals(statusReason)) {
            throw new TestFailException("Sdx status reason is invalid: " + statusReason);
        }
        return testDto;
    }
}
