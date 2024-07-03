package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.inject.Inject;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Streams;
import com.sequenceiq.cloudbreak.common.json.JsonUtil;
import com.sequenceiq.cloudbreak.sdx.RdcConstants;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.it.cloudbreak.client.CdlTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.RedbeamsDatabaseServerTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.cdl.CdlTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.mock.Method;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.redbeams.api.endpoint.v4.databaseserver.responses.DatabaseServerV4Response;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;
import com.sequenceiq.sdx.api.model.SdxDatabaseAvailabilityType;
import com.sequenceiq.sdx.api.model.SdxDatabaseRequest;

public class DistroXTemplateGenerationTests extends AbstractMockTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistroXTemplateGenerationTests.class);

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private CdlTestClient cdlTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private RedbeamsDatabaseServerTestClient redbeamsDatabaseServerTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    private void createEnvironment(TestContext testContext, String environmentKey) {
        testContext.given(environmentKey, EnvironmentTestDto.class)
                .withName(environmentKey)
                .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create(), key(environmentKey))
                .await(EnvironmentStatus.AVAILABLE, key(environmentKey))
                .when(environmentTestClient.describe(), key(environmentKey))
                .given(FreeIpaTestDto.class)
                .withEnvironment(environmentKey)
                .when(freeIpaTestClient.create())
                .await(Status.AVAILABLE)
                .validate();
    }

    private void createVmBasedDl(TestContext testContext, String environmentKey) {
        SdxDatabaseRequest sdxDatabaseRequest = new SdxDatabaseRequest();
        sdxDatabaseRequest.setCreate(Boolean.TRUE);
        sdxDatabaseRequest.setAvailabilityType(SdxDatabaseAvailabilityType.NON_HA);
        testContext.given(SdxInternalTestDto.class)
                .withEnvironmentKey(key(environmentKey))
                .withCloudStorage(getCloudStorageRequest(testContext))
                .withEnableMultiAz()
                .withDatabase(sdxDatabaseRequest)
                .withTelemetry("telemetry")
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .when(sdxTestClient.describeInternal())
                .validate();
    }

    private void createAndSetupCdlMock(TestContext testContext, String vmDlEnvironmentKey, String cdlEnvironmentKey) {
        testContext.given(CdlTestDto.class)
                .withEnvironmentKey(key(cdlEnvironmentKey))
                .when(cdlTestClient.create())
                // REST call for CDL giving VM form DL based database information
                .given(RedbeamsDatabaseServerTestDto.class)
                .when(redbeamsDatabaseServerTestClient.describeByClusterCrn(
                        testContext.get(vmDlEnvironmentKey, EnvironmentTestDto.class).getResponse().getCrn(),
                        testContext.get(SdxInternalTestDto.class).getCrn()
                ))
                .then((tc, testDto, client) -> {
                    String cdlCrn = tc.get(CdlTestDto.class).getResponse().getCrn();
                    DatabaseServerV4Response vmFormDlDatabase = tc.get(RedbeamsDatabaseServerTestDto.class).getResponse();
                    Map<String, String> request = Map.of("crn", cdlCrn,
                            "databaseServerCrn", vmFormDlDatabase.getCrn(),
                            "hmsDatabaseHost", vmFormDlDatabase.getHost(),
                            "hmsDatabaseUser", "hive",
                            "hmsDatabasePassword", "hive",
                            "hmsDatabaseName", "hive",
                            "rangerFqdn", "rangerHost");
                    LOGGER.info("Posting map [{}] to thunderhead-mock regarding CDL {}", request, cdlCrn);
                    getMockCdlRestCallExecutor().executeMethod(Method.build("POST"), "/api/v1/cdl/addDatabaseConfig",
                            new HashMap<>(), Entity.entity(JsonUtil.writeValueAsString(request), MediaType.APPLICATION_JSON_TYPE), response -> { }, w -> w);
                    return testDto;
                })
                .validate();
    }

    private String createDistroxAndGetGeneratedTemplate(TestContext testContext, String environmentKey, String distroXKey, String templateLogMessage) {
        AtomicReference<String> generatedTemplate = new AtomicReference<>();
        testContext.given(distroXKey, DistroXTestDto.class)
                .withEnvironmentKey(environmentKey)
                .when(distroXTestClient.create(), key(distroXKey))
                .await(STACK_AVAILABLE, key(distroXKey))
                .when((context, dto, client) -> {
                    generatedTemplate.set(dto.getResponse().getCluster().getExtendedBlueprintText());
                    LOGGER.info("Generated template for DH regarding " + templateLogMessage + ": {}",
                            dto.getResponse().getCluster().getExtendedBlueprintText());
                    return dto;
                })
                .validate();
        return generatedTemplate.get();
    }

    private String findHmsHostFromGeneratedTemplate(ObjectMapper objectMapper, String generatedTemplate) throws JsonProcessingException {
        return Streams.stream(objectMapper.readTree(generatedTemplate).withArray("services").elements())
                .filter(serviceJsonNode -> StringUtils.equals(serviceJsonNode.get("serviceType").asText(), "HIVE"))
                .flatMap(serviceJsonNode -> Streams.stream(serviceJsonNode.withArray("serviceConfigs").elements()))
                .filter(serviceConfigJsonNode -> StringUtils.equals(serviceConfigJsonNode.get("name").asText(),
                        RdcConstants.HiveMetastoreDatabase.HIVE_METASTORE_DATABASE_HOST))
                .map(serviceConfigJsonNode -> serviceConfigJsonNode.get("value").asText())
                .findFirst()
                .orElse(null);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(given = "two running environments",
            when = "one environment got CDL and the other one got VM based DL",
            then = "datahub provisioned in each environment should have the same database in the generated template")
    public void compareCdlBasedAndVmFormDlBasedDhTemplate(TestContext testContext) throws JsonProcessingException {
        String vmFormDlBasedEnvName = resourcePropertyProvider().getName();
        String cdlBasedEnvName = resourcePropertyProvider().getName();
        String vmFormDlBasedDistroXName = resourcePropertyProvider().getName();
        String cdlBasedDistroXName = resourcePropertyProvider().getName();
        createEnvironment(testContext, vmFormDlBasedEnvName);
        createEnvironment(testContext, cdlBasedEnvName);
        createVmBasedDl(testContext, vmFormDlBasedEnvName);
        createAndSetupCdlMock(testContext, vmFormDlBasedEnvName, cdlBasedEnvName);
        String generatedTemplateForVmFormDlBasedDistroX =
                createDistroxAndGetGeneratedTemplate(testContext, vmFormDlBasedEnvName, vmFormDlBasedDistroXName, "VM based DL");
        String generatedTemplateForCdlBasedDistroX =
                createDistroxAndGetGeneratedTemplate(testContext, cdlBasedEnvName, cdlBasedDistroXName, "CDL");
        ObjectMapper objectMapper = new ObjectMapper();
        String hmsHostOfVmFormDlBasedDistroX = findHmsHostFromGeneratedTemplate(objectMapper, generatedTemplateForVmFormDlBasedDistroX);
        String hmsHostOfCdlBasedDistroX = findHmsHostFromGeneratedTemplate(objectMapper, generatedTemplateForCdlBasedDistroX);
        Assertions.assertEquals(hmsHostOfVmFormDlBasedDistroX, hmsHostOfCdlBasedDistroX);
    }
}
