package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.expectedPayload;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerProductTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.stack.StackTestDto;

public class ClouderaManagerStackCreationTest extends AbstractClouderaManagerTest {

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private StackTestClient stackTestClient;

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
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .given(cluster, ClusterTestDto.class)
                .withBlueprintName(name)
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(clouderaManager)
                .given(StackTestDto.class).withCluster(cluster)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
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
        String versionKey = "versionEx";
        String parcelKey = "parcelEx";
        String nameKey = "nameEx";

        String versionValidation = "\\[\\{\"field\":\"post\\.arg1\\.cluster\\.cm.products\\[0\\]\\.version\",\"result\":\"must not be empty\"\\}\\]";
        String parcelValidation = "\\[\\{\"field\":\"post\\.arg1\\.cluster\\.cm.products\\[0\\]\\.parcel\",\"result\":\"must not be empty\"\\}\\]";
        String nameValidation = "\\[\\{\"field\":\"post\\.arg1\\.cluster\\.cm.products\\[0\\]\\.name\",\"result\":\"must not be empty\"\\}\\]";

        testContext
                .given(partialProduct, ClouderaManagerProductTestDto.class)
                .withName("CDH")
                .withVersion("")
                .withParcel("http://cdh/parcel")
                .given(clouderaManager, ClouderaManagerTestDto.class)
                .withClouderaManagerProduct(partialProduct)
                .given(cluster, ClusterTestDto.class)
                .withBlueprintName(name)
                .withValidateBlueprint(Boolean.FALSE)
                .withClouderaManager(clouderaManager)
                .given(StackTestDto.class).withCluster(cluster)
                .whenException(stackTestClient.createV4(), BadRequestException.class, expectedPayload(versionValidation))
                .given(partialProduct, ClouderaManagerProductTestDto.class)
                .withName("CDH")
                .withVersion("7.0.0.0")
                .withParcel("")
                .given(StackTestDto.class)
                .whenException(stackTestClient.createV4(), BadRequestException.class, expectedPayload(parcelValidation))
                .given(partialProduct, ClouderaManagerProductTestDto.class)
                .withName("")
                .withVersion("7.0.0.0")
                .withParcel("http://cdh/parcel")
                .given(StackTestDto.class)
                .whenException(stackTestClient.createV4(), BadRequestException.class, expectedPayload(nameValidation))
                .validate();
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }
}
