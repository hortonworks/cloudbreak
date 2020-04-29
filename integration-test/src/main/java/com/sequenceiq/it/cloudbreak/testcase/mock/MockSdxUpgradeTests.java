package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.assertion.datalake.SdxUpgradeTestAssertion;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.RedbeamsDatabaseServerTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.ImageSettingsTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.redbeams.api.model.common.Status;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class MockSdxUpgradeTests extends AbstractIntegrationTest {

    private static final String TEMPLATE_JSON = "classpath:/templates/sdx-cluster-template.json";

    @Inject
    private WaitUtil waitUtil;

    @Inject
    private RedbeamsDatabaseServerTestClient redbeamsDatabaseServerTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running Cloudbreak",
            when = "start an sdx cluster",
            then = "Upgrade criteria is not met"
    )
    public void testSdxUpgradeCriteriaNotMetTest(MockedTestContext testContext) {
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        String imageSettings = "imageSettingsUpgrade";
        String networkKey = "someOtherNetwork";
        String envKey = "sdxEnvKey";
        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withClouderaManager(clouderaManager)
                .given(imageSettings, ImageSettingsTestDto.class)
                .withImageId("aaa778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(upgradeImageCatalogName)
                .given(stack, StackTestDto.class)
                .withCluster(cluster)
                .withImageSettings(imageSettings)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then(SdxUpgradeTestAssertion
                        .validateReasonContains("Action is only supported if Cloudera Manager state is stored in external Database."))
                .then(SdxUpgradeTestAssertion.validateReasonContains("Cloudera Manager server failure with embedded Database cannot be repaired!"))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running CloudSdxInternalTestDtobreak",
            when = "start an sdx cluster",
            then = "Upgrade option should be presented"
    )
    public void testSdxUpgradeSuccessful(MockedTestContext testContext) {
        String upgradeImageCatalogName = resourcePropertyProvider().getName();
        createImageCatalogForOsUpgrade(testContext, upgradeImageCatalogName);
        String sdxInternal = resourcePropertyProvider().getName();
        String stack = resourcePropertyProvider().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        String imageSettings = "imageSettingsUpgrade";
        String networkKey = "someOtherNetwork";
        testContext
                .given(networkKey, EnvironmentNetworkTestDto.class)
                .withMock(new EnvironmentNetworkMockParams())
                .given(EnvironmentTestDto.class)
                .withNetwork(networkKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .withName(resourcePropertyProvider().getEnvironmentName())
                .when(getEnvironmentTestClient().create())
                .await(EnvironmentStatus.AVAILABLE)
                .given(RedbeamsDatabaseServerTestDto.class)
                .withEnvironmentCrn(testContext.get(EnvironmentTestDto.class).getResponse().getCrn())
                .when(redbeamsDatabaseServerTestClient.createV4())
                .await(Status.AVAILABLE)
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withClouderaManager(clouderaManager)
                .withExternalDatabaseCrn()
                .given(imageSettings, ImageSettingsTestDto.class)
                .withImageId("aaa778fc-7f17-4535-9021-515351df3691")
                .withImageCatalog(upgradeImageCatalogName)
                .given(stack, StackTestDto.class)
                .withCluster(cluster)
                .withImageSettings(imageSettings)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .given(sdxInternal, SdxInternalTestDto.class)
                .withStackRequest(key(cluster), key(stack))
                .when(sdxTestClient.createInternal(), key(sdxInternal))
                .awaitForFlow(key(sdxInternal))
                .await(SdxClusterStatusResponse.RUNNING)
                .then(SdxUpgradeTestAssertion.validateSucessfulUpgrade())
                .validate();
    }

    protected ImageCatalogTestDto createImageCatalogForOsUpgrade(TestContext testContext, String name) {
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;
        return testContext
                .given(ImageCatalogTestDto.class)
                .withName(name)
                .withUrl(mockedTestContext.getImageCatalogMockServerSetup().getUpgradeImageCatalogUrl())
                .when(imageCatalogTestClient.createV4(), key(name));
    }
}
