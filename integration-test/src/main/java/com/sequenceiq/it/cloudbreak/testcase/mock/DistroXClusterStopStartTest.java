package com.sequenceiq.it.cloudbreak.testcase.mock;

import javax.inject.Inject;

import org.testng.ITestContext;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.image.DistroXImageTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.instancegroup.DistroXNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager.AbstractClouderaManagerTest;

public class DistroXClusterStopStartTest extends AbstractClouderaManagerTest {

    private static final String IMAGE_CATALOG_ID = "f6e778fc-7f17-4535-9021-515351df3691";

    private static final String MOCK_HOSTNAME = "mockrdshost";

    private static final String CM_FOR_DISTRO_X = "cm4dstrx";

    private static final String CLUSTER_KEY = "cmdistrox";

    private static final String DIX_IMG_KEY = "dixImg";

    private static final String DIX_NET_KEY = "dixNet";

    private static final String DISTRO_X_STACK = "distroxstack";

    private static final String HOST_TEMPLATE_REF_NAME_FORMAT = "\"hostTemplateRefName\":\"%s\"";

    private static final String ENVIRONMENT_LOCATION = "London";

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a DistroX with Cloudera Manager is created",
            then = "the cluster should be available")
    public void testCreateNewRegularDistroXCluster(MockedTestContext testContext, ITestContext testNgContext) {
        DistroXStartStopTestParameters params = new DistroXStartStopTestParameters(testNgContext.getCurrentXmlTest().getAllParameters());
        int step = params.getStep();
        int current = step;
        DistroXTestDto currentContext = testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog(getImageCatalogName(testContext))
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(DistroXTestDto.class)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .withCluster(CLUSTER_KEY)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create())
                .await(STACK_AVAILABLE);
        for (int i = 0; i < params.getTimes(); i++, current += step) {
            currentContext = currentContext
                    .when(distroXClient.stop())
                    .await(STACK_STOPPED)
                    .when(distroXClient.start())
                    .await(STACK_AVAILABLE)
                    .when(distroXClient.scale(params.getHostgroup(), current))
                    .await(STACK_AVAILABLE);
        }

        currentContext
                .validate();
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }

    private String getImageCatalogName(TestContext testContext) {
        return testContext.get(ImageCatalogTestDto.class).getRequest().getName();
    }
}