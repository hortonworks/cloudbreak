package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedPayload;

import jakarta.inject.Inject;
import jakarta.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;

public class ClouderaManagerStackCreationTest extends AbstractClouderaManagerTest {

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster with Cloudera Manager is created",
            then = "the cluster should be available")
    public void testCreateNewRegularCluster(MockedTestContext testContext) {
        String name = testContext.get(BlueprintTestDto.class).getRequest().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(clouderaManager, DistroXClouderaManagerTestDto.class)
                .given(cluster, DistroXClusterTestDto.class)
                .withBlueprintName(name)
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(clouderaManager)
                .given(DistroXTestDto.class).withCluster(cluster)
                .when(distroXTestClient.create())
                .awaitForFlow()
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "a cluster with incomplete product posted",
            then = "validation error")
    public void testCreateClusterWithIncompleteProduct(MockedTestContext testContext) {
        String name = testContext.get(BlueprintTestDto.class).getRequest().getName();
        String clouderaManager = "cm";
        String cluster = "cmcluster";
        String partialProduct = "partialProduct";

        String nameValidation = "\\[\\{\"field\":\"post\\.request\\.cluster\\.cm.products\\[0\\]\\.name\",\"result\":\"must not be null\"\\}\\]";

        testContext
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(partialProduct, DistroXClouderaManagerProductTestDto.class)
                .withVersion("7.0.0.0")
                .withParcel("http://cdh/parcel")
                .given(clouderaManager, DistroXClouderaManagerTestDto.class)
                .withClouderaManagerProduct(partialProduct)
                .given(cluster, DistroXClusterTestDto.class)
                .withBlueprintName(name)
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(clouderaManager)
                .given(DistroXTestDto.class).withCluster(cluster)
                .whenException(distroXTestClient.create(), BadRequestException.class, expectedPayload(nameValidation))
                .validate();
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }
}
