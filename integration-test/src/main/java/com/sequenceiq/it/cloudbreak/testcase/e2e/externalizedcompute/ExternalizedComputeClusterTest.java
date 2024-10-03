package com.sequenceiq.it.cloudbreak.testcase.e2e.externalizedcompute;

import java.util.List;
import java.util.Map;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.client.ApiKeyRequestFilter;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.config.server.ServerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class ExternalizedComputeClusterTest extends AbstractE2ETest {

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ServerProperties serverProperties;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an env with externalized compute service",
            then = "these should be available")
    public void testCreateExternalizedComputeClusterThenDelete(TestContext testContext) {
        String environmentName = resourcePropertyProvider().getName();
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentNetworkTestDto.class)
                .given(environmentName, EnvironmentTestDto.class)
                .withName(environmentName)
                .withExternalizedComputeCluster()
                .withNetwork()
                .withCreateFreeIpa(true)
                .withOneFreeIpaNode()
                .withTelemetry("telemetry")
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .then((tc, testDto, client) -> {
                    validateListClusterResponse(tc, environmentName);
                    return testDto;
                })
                .validate();
    }

    private void validateListClusterResponse(TestContext testContext, String environmentName) {
        Client client = RestClientUtil.get();
        WebTarget webTarget = client.target(serverProperties.getCloudbreak()).path("/api/v1/compute/listClusters");
        webTarget.register(new ApiKeyRequestFilter(testContext.getActingUserAccessKey(), testContext.getActingUser().getSecretKey()));
        String requestjson = String.format("{\"envNameOrCrn\": \"%s\"}", environmentName);
        String response = webTarget.request().post(Entity.json(requestjson)).readEntity(String.class);
        Map<String, Object> responseMap = new Json(response).getMap();
        Map<String, Object> defaultCluster = ((List<Map<String, Object>>) responseMap.get("clusters")).get(0);
        String status = (String) defaultCluster.get("status");
        if (!"RUNNING".equalsIgnoreCase(status)) {
            throw new TestFailException(String.format("Test failed. Default compute cluster status is not RUNNING. Status: '%s'\n" +
                    "Cluster response: %s", status, responseMap));
        }
    }

}
