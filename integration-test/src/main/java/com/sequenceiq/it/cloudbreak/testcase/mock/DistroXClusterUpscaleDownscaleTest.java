package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

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

public class DistroXClusterUpscaleDownscaleTest extends AbstractClouderaManagerTest {

    private static final int UPPER_NODE_COUNT = 10;

    private static final int LOWER_NODE_COUNT = 5;

    private static final String IMAGE_CATALOG_ID = "f6e778fc-7f17-4535-9021-515351df3691";

    private static final String CM_FOR_DISTRO_X = "cm4dstrx";

    private static final String CLUSTER_KEY = "cmdistrox";

    private static final String DIX_IMG_KEY = "dixImg";

    private static final String DIX_NET_KEY = "dixNet";

    private static final Duration POLLING_INTERVAL = Duration.of(3000, ChronoUnit.MILLIS);

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running DistroX cluster",
            when = "a scale, start stop called many times",
            then = "the cluster should be available")
    public void testScaleDownAndUp(MockedTestContext testContext, ITestContext testNgContext) {
        DistroXStartStopTestParameters params = new DistroXStartStopTestParameters(testNgContext.getCurrentXmlTest().getAllParameters());
        String stack = resourcePropertyProvider().getName();
        DistroXTestDto currentContext = testContext
                .given(DIX_NET_KEY, DistroXNetworkTestDto.class)
                .given(DIX_IMG_KEY, DistroXImageTestDto.class)
                .withImageCatalog(getImageCatalogName(testContext))
                .withImageId(IMAGE_CATALOG_ID)
                .given(CM_FOR_DISTRO_X, DistroXClouderaManagerTestDto.class)
                .given(CLUSTER_KEY, DistroXClusterTestDto.class)
                .withValidateBlueprint(false)
                .withClouderaManager(CM_FOR_DISTRO_X)
                .given(stack, DistroXTestDto.class)
                .withGatewayPort(testContext.getSparkServer().getPort())
                .withCluster(CLUSTER_KEY)
                .withName(stack)
                .withImageSettings(DIX_IMG_KEY)
                .withNetwork(DIX_NET_KEY)
                .when(distroXClient.create(), key(stack))
                .awaitForFlow(key(stack))
                .await(STACK_AVAILABLE, key(stack));

        for (int i = 0; i < 3; i++) {
            currentContext = currentContext
                    .when(distroXClient.scale(params.getHostgroup(), UPPER_NODE_COUNT))
                    .awaitForFlow(key(stack))
                    .await(DistroXTestDto.class, STACK_AVAILABLE, key(stack), POLLING_INTERVAL);

            currentContext = currentContext
                    .when(distroXClient.scale(params.getHostgroup(), LOWER_NODE_COUNT))
                    .awaitForFlow(key(stack))
                    .await(DistroXTestDto.class, STACK_AVAILABLE, key(stack), POLLING_INTERVAL);
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