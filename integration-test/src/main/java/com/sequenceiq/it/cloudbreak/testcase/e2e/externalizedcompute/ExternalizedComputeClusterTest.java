package com.sequenceiq.it.cloudbreak.testcase.e2e.externalizedcompute;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.WebTarget;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.client.ApiKeyRequestFilter;
import com.sequenceiq.cloudbreak.client.RestClientUtil;
import com.sequenceiq.cloudbreak.common.json.Json;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.config.server.ServerProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.externalizedcompute.ExternalizedComputeClusterTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;

public class ExternalizedComputeClusterTest extends AbstractE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalizedComputeClusterTest.class);

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private ServerProperties serverProperties;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private CloudbreakClient cloudbreakClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createExtendedCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        cloudbreakClient = new CloudbreakClient(testContext.getActingUser(), regionAwareInternalCrnGeneratorFactory.iam(),
                serverProperties.getCloudbreakAddress(), serverProperties.getCloudbreakInternalAddress(), serverProperties.getAlternativeCloudbreakAddress(),
                serverProperties.getAlternativeCloudbreakInternalAddress(), serverProperties.getAlternativeCloudbreak());
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an env with externalized compute service",
            then = "these should be available")
    public void testCreateExternalizedComputeCluster(TestContext testContext) {
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
                .awaitForCreationFlow()
                .then((tc, testDto, client) -> {
                    validateListClusterResponseAndGetClusterCrn(tc, environmentName);
                    return testDto;
                })
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "create an env without externalized compute service, then migrate it, then force reinitialize",
            then = "externalized cluster should be successfully created")
    public void testCreateEnvAndExtendWithComputeThenForceReinitializeAsPrivate(TestContext testContext) {
        String environmentName = resourcePropertyProvider().getName();
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .withReportClusterLogs()
                .given(EnvironmentNetworkTestDto.class)
                .given(environmentName, EnvironmentTestDto.class)
                .withName(environmentName)
                .withNetwork()
                .withCreateFreeIpa(true)
                .withOneFreeIpaNode()
                .withTelemetry("telemetry")
                .withTunnel(Tunnel.CCMV2_JUMPGATE)
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .when(environmentTestClient.createDefaultExternalizedComputeCluster())
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .given(ExternalizedComputeClusterTestDto.class)
                .then((tc, testDto, client) -> {
                    String clusterCrn = validateListClusterResponseAndGetClusterCrn(tc, environmentName);
                    testDto.setSavedLiftieCrn(clusterCrn);
                    return testDto;
                })
                .given(environmentName, EnvironmentTestDto.class)
                .withName(environmentName)
                .when(environmentTestClient.reInitializeDefaultExternalizedComputeCluster(true))
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .then((tc, t, c) -> {
                    boolean privateCluster = t.getResponse().getExternalizedComputeCluster().isPrivateCluster();
                    if (!privateCluster) {
                        throw new TestFailException("compute cluster is not a private cluster");
                    }
                    return t;
                })
                .given(ExternalizedComputeClusterTestDto.class)
                .then((tc, testDto, client) -> {
                    String firstLiftieCrn = testDto.getSavedLiftieCrn();
                    String newLiftieCrn = validateListClusterResponseAndGetClusterCrn(tc, environmentName);
                    if (Objects.equals(firstLiftieCrn, newLiftieCrn)) {
                        throw new TestFailException("The first liftie crn should not be the same as the new one");
                    }
                    return testDto;
                })
                .validate();
    }

    private String validateListClusterResponseAndGetClusterCrn(TestContext testContext, String environmentName) {
        Client client = RestClientUtil.get();
        WebTarget webTarget = cloudbreakClient.getRawClient(testContext).path("/v1/compute/listClusters");
        webTarget.register(new ApiKeyRequestFilter(testContext.getActingUserAccessKey(), testContext.getActingUser().getSecretKey()));
        String requestjson = String.format("{\"envNameOrCrn\": \"%s\"}", environmentName);
        String response = webTarget.request().post(Entity.json(requestjson)).readEntity(String.class);
        LOGGER.info("ListClusters response from Liftie service: {}", response);
        Map<String, Object> responseMap = new Json(response).getMap();
        Optional<Map<String, Object>> defaultCluster = Optional.ofNullable(responseMap)
                .map(resp -> resp.get("clusters"))
                .map(resp -> (List<Map<String, Object>>) resp)
                .map(List::stream)
                .flatMap(Stream::findFirst);
        defaultCluster.ifPresentOrElse(def -> {
                    String status = (String) def.get("status");
                    if (!"RUNNING".equalsIgnoreCase(status)) {
                        throw new TestFailException(String.format("Test failed. Default compute cluster status is not RUNNING. Status: '%s'\n" +
                                "Cluster response: %s", status, responseMap));
                    }

                }, () -> {
                    throw new TestFailException(String.format("Test failed. Reading cluster list from liftie was not successful. Response: %s", response));
                }
        );
        return (String) defaultCluster.get().get("clusterCrn");
    }

}
