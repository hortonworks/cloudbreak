package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import java.io.IOException;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AmbariTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.util.ShowBlueprintUtil;

public class ClouderaManagerShowBlueprintTest extends AbstractClouderaManagerTest {

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak with a not alive cluster",
            when = "the generated blueprint is requested",
            then = "the valid future blueprint is returned")
    public void testGetBlueprintWhenClusterIsNotAliveThenShouldReturnWithBlueprint(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        String blueprintName = testContext.get(BlueprintTestDto.class).getRequest().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String cm = resourcePropertyProvider().getName();

        testContext
                .given(cm, AmbariTestDto.class)
                .given(cmcluster, ClusterTestDto.class)
                .withAmbari(cm)
                .withBlueprintName(blueprintName)
                .withValidateBlueprint(Boolean.FALSE)
                .given(StackTestDto.class)
                .withCluster(cmcluster)
                .withName(clusterName)
                .when(stackTestClient.blueprintRequestV4())
                .then(ShowBlueprintUtil::checkFutureBlueprint)
                .then(this::checkValidFutureClouderaManagerTemplate)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak with an alive cluster",
            when = "the generated blueprint is requested",
            then = "the valid generated blueprint is returned")
    public void testGetBlueprintWhenClusterIsAliveThenShouldReturnWithBlueprint(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        String blueprintName = testContext.get(BlueprintTestDto.class).getRequest().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String cm = resourcePropertyProvider().getName();
        testContext
                .given(cm, ClouderaManagerTestDto.class)
                .given(cmcluster, ClusterTestDto.class)
                .withClouderaManager(cm)
                .withBlueprintName(blueprintName)
                .withValidateBlueprint(Boolean.FALSE)
                .given(StackTestDto.class).withCluster(cmcluster)
                .withName(clusterName)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.getV4())
                .then(ShowBlueprintUtil::checkGeneratedBlueprint)
                .then(this::checkValidClouderaManagerTemplate)
                .validate();
    }

    private StackTestDto checkValidFutureClouderaManagerTemplate(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedBlueprintText = stackTestDto.getGeneratedBlueprint().getBlueprintText();
        ApiClusterTemplate cmTemplate = parseCmTemplate(extendedBlueprintText);
        checkHostNumberNullOrZero(cmTemplate);
        return stackTestDto;
    }

    private StackTestDto checkValidClouderaManagerTemplate(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedBlueprintText = stackTestDto.getResponse().getCluster().getExtendedBlueprintText();
        ApiClusterTemplate cmTemplate = parseCmTemplate(extendedBlueprintText);
        checkHostNumberGreaterThanZero(cmTemplate);
        return stackTestDto;
    }

    private void checkHostNumberGreaterThanZero(ApiClusterTemplate cmTemplate) {
        if (cmTemplate.getInstantiator().getHosts() == null || cmTemplate.getInstantiator().getHosts().size() == 0) {
            throw new TestFailException("Hosts should be generated into the cluster template");
        }
    }

    private void checkHostNumberNullOrZero(ApiClusterTemplate cmTemplate) {
        if (cmTemplate.getInstantiator().getHosts() != null && cmTemplate.getInstantiator().getHosts().size() > 0) {
            throw new TestFailException("Hosts should not be generated into the cluster template");
        }
    }

    private ApiClusterTemplate parseCmTemplate(String blueprintText) {
        try {
            return JsonUtil.readValue(blueprintText, ApiClusterTemplate.class);
        } catch (IOException e) {
            throw new TestFailException("Error during parsing the generated cluster template");
        }
    }
}

