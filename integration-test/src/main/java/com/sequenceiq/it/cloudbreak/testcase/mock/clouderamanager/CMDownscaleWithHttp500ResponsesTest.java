package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiParcel;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.config.server.ServerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.util.cleanup.ParcelGeneratorUtil;
import com.sequenceiq.it.util.cleanup.ParcelMockActivatorUtil;

public class CMDownscaleWithHttp500ResponsesTest extends AbstractClouderaManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(CMDownscaleWithHttp500ResponsesTest.class);

    private static final Duration POLLING_INTERVAL = Duration.of(10000, ChronoUnit.MILLIS);

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private ParcelMockActivatorUtil parcelMockActivatorUtil;

    @Inject
    private ParcelGeneratorUtil parcelGeneratorUtil;

    @Inject
    private ServerProperties serverProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultDatalake(testContext);
        createCmBlueprint(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a stack with upscale",
            when = "upscale to 15 it downscale to 6",
            then = "stack is running")
    public void testDownscale(MockedTestContext testContext) {
        ApiParcel parcel = parcelGeneratorUtil.getActivatedCDHParcel();
        String clusterName = resourcePropertyProvider().getName();
        parcelMockActivatorUtil.mockActivateWithDefaultParcels(testContext, clusterName, parcel);
        testContext
                .given("cmpkey", DistroXClouderaManagerProductTestDto.class)
                .withParcel("https://" + serverProperties.getMockImageCatalogAddr() + "/mock-parcel/someParcel")
                .withName(parcel.getProduct())
                .withVersion(parcel.getVersion())
                .given("cmanager", DistroXClouderaManagerTestDto.class)
                .withClouderaManagerProduct("cmpkey")
                .given("cmpclusterkey", DistroXClusterTestDto.class)
                .withClouderaManager("cmanager")
                .given(clusterName, DistroXTestDto.class)
                .withName(clusterName)
                .withCluster("cmpclusterkey")
                .when(distroXTestClient.create(), key(clusterName))
                .mockCm().profile(PROFILE_RETURN_HTTP_500, 1)
                .await(STACK_AVAILABLE, key(clusterName).withIgnoredStatues(Set.of(Status.UNREACHABLE)))
                .when(distroXTestClient.scale("worker", 15))
                .await(STACK_AVAILABLE, key(clusterName).withPollingInterval(POLLING_INTERVAL).withIgnoredStatues(Set.of(Status.UNREACHABLE)))
                .when(distroXTestClient.scale("worker", 6))
                .await(STACK_AVAILABLE, key(clusterName).withPollingInterval(POLLING_INTERVAL).withIgnoredStatues(Set.of(Status.UNREACHABLE)))
                .validate();
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }
}
