package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import java.io.IOException;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.cloudera.api.swagger.model.ApiClusterTemplate;
import com.sequenceiq.cloudbreak.util.JsonUtil;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.client.ClusterDefinitionTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AmbariTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClouderaManagerTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ClusterTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.clusterdefinition.ClusterDefinitionTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.util.ShowClusterDefinitionUtil;

public class ClouderaManagerShowClusterDefinitionTest extends AbstractClouderaManagerTest {

    @Inject
    private ClusterDefinitionTestClient clusterDefinitionTestClient;

    @Inject
    private StackTestClient stackTestClient;

    @Override
    protected ClusterDefinitionTestClient clusterDefinitionTestClient() {
        return clusterDefinitionTestClient;
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak with a not alive cluster",
            when = "the generated cluster definition is requested",
            then = "the valid future cluster definition is returned")
    public void testGetClusterDefinitionWhenClusterIsNotAliveThenShouldReturnWithClusterDefinition(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        String clusterDefinitionName = testContext.get(ClusterDefinitionTestDto.class).getRequest().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String cm = resourcePropertyProvider().getName();

        testContext
                .given(cm, AmbariTestDto.class)
                .given(cmcluster, ClusterTestDto.class)
                .withAmbari(cm)
                .withClusterDefinitionName(clusterDefinitionName)
                .withValidateClusterDefinition(Boolean.FALSE)
                .given(StackTestDto.class)
                .withCluster(cmcluster)
                .withName(clusterName)
                .when(stackTestClient.clusterDefinitionRequestV4())
                .then(ShowClusterDefinitionUtil::checkFutureClusterDefinition)
                .then(this::checkValidFutureClouderaManagerTemplate)
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak with an alive cluster",
            when = "the generated cluster definition is requested",
            then = "the valid generated cluster definition is returned")
    public void testGetClusterDefinitionWhenClusterIsAliveThenShouldReturnWithClusterDefinition(MockedTestContext testContext) {
        String clusterName = resourcePropertyProvider().getName();
        String clusterDefinitionName = testContext.get(ClusterDefinitionTestDto.class).getRequest().getName();
        String cmcluster = resourcePropertyProvider().getName();
        String cm = resourcePropertyProvider().getName();
        testContext
                .given(cm, ClouderaManagerTestDto.class)
                .given(cmcluster, ClusterTestDto.class)
                .withClouderaManager(cm)
                .withClusterDefinitionName(clusterDefinitionName)
                .withValidateClusterDefinition(Boolean.FALSE)
                .given(StackTestDto.class).withCluster(cmcluster)
                .withName(clusterName)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .when(stackTestClient.getV4())
                .then(ShowClusterDefinitionUtil::checkGeneratedClusterDefinition)
                .then(this::checkValidClouderaManagerTemplate)
                .validate();
    }

    private StackTestDto checkValidFutureClouderaManagerTemplate(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedClusterDefinitionText = stackTestDto.getGeneratedClusterDefinition().getClusterDefinitionText();
        ApiClusterTemplate cmTemplate = parseCmTemplate(extendedClusterDefinitionText);
        checkHostNumberNullOrZero(cmTemplate);
        return stackTestDto;
    }

    private StackTestDto checkValidClouderaManagerTemplate(TestContext testContext, StackTestDto stackTestDto, CloudbreakClient cloudbreakClient) {
        String extendedClusterDefinitionText = stackTestDto.getResponse().getCluster().getExtendedClusterDefinitionText();
        ApiClusterTemplate cmTemplate = parseCmTemplate(extendedClusterDefinitionText);
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

    private ApiClusterTemplate parseCmTemplate(String clusterDefinitionText) {
        try {
            return JsonUtil.readValue(clusterDefinitionText, ApiClusterTemplate.class);
        } catch (IOException e) {
            throw new TestFailException("Error during parsing the generated cluster template");
        }
    }
}

