package com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager;

import java.io.IOException;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.DistroXClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.cluster.clouderamanager.DistroXClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.util.ShowBlueprintUtil;

public class ClouderaManagerShowBlueprintTest extends AbstractClouderaManagerTest {

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private DistroXTestClient distroXTestClient;

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
                .given(cm, DistroXClouderaManagerTestDto.class)
                .given(cmcluster, DistroXClusterTestDto.class)
                .withClouderaManager(cm)
                .withBlueprintName(blueprintName)
                .withValidateBlueprint(Boolean.FALSE)
                .given(DistroXTestDto.class)
                .withCluster(cmcluster)
                .withName(clusterName)
                .when(distroXTestClient.blueprintRequest())
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
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.createInternal())
                .awaitForFlow()
                .given(cm, DistroXClouderaManagerTestDto.class)
                .given(cmcluster, DistroXClusterTestDto.class)
                .withClouderaManager(cm)
                .withBlueprintName(blueprintName)
                .withValidateBlueprint(Boolean.FALSE)
                .given(DistroXTestDto.class).withCluster(cmcluster)
                .withName(clusterName)
                .when(distroXTestClient.create())
                .awaitForFlow()
                .when(distroXTestClient.get())
                .then(ShowBlueprintUtil::checkGeneratedBlueprint)
                .then(this::checkValidClouderaManagerTemplate)
                .validate();
    }

    private DistroXTestDto checkValidFutureClouderaManagerTemplate(TestContext testContext, DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        String extendedBlueprintText = distroXTestDto.getGeneratedBlueprint().getBlueprintText();
        ApiClusterTemplate cmTemplate = parseCmTemplate(extendedBlueprintText);
        checkHostNumberNullOrZero(cmTemplate);
        return distroXTestDto;
    }

    private DistroXTestDto checkValidClouderaManagerTemplate(TestContext testContext, DistroXTestDto distroXTestDto, CloudbreakClient cloudbreakClient) {
        String extendedBlueprintText = distroXTestDto.getResponse().getCluster().getExtendedBlueprintText();
        ApiClusterTemplate cmTemplate = parseCmTemplate(extendedBlueprintText);
        checkHostNumberGreaterThanZero(cmTemplate);
        return distroXTestDto;
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

