package com.sequenceiq.it.cloudbreak.testcase.mock;

import static com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus.ENV_STOPPED;

import java.util.Set;

import jakarta.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.StackV4Response;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.DescribeFreeIpaResponse;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.testcase.mock.clouderamanager.AbstractClouderaManagerTest;

public class FullVerticalScaleTest extends AbstractClouderaManagerTest {

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private DistroXTestClient distroXClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
        createCmBlueprint(testContext);
        createDefaultDatahub(testContext);
    }

    @Override
    protected void createDefaultEnvironment(TestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .withCreateFreeIpa(Boolean.FALSE)
                .withNewNetwork()
                .when(environmentTestClient.create())
                .awaitForCreationFlow()
                .when(environmentTestClient.describe())
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "an Environment with Data lake and Data hub created",
            then = "the cluster should be vertical scale successfully")
    public void testCreateNewRegularDistroXCluster(MockedTestContext testContext) {

        String freeipaVerticalScaleKey = "freeipaVerticalScaleKey";
        String sdxVerticalScaleKey = "sdxVerticalScaleKey";
        String distroxVerticalScaleKey = "distroxVerticalScaleKey";
        String xlargeInstanceType = "xlarge";
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop())
                .await(ENV_STOPPED)
                .given(freeipaVerticalScaleKey, VerticalScalingTestDto.class)
                .withGroup("master")
                .withInstanceType(xlargeInstanceType)
                .given(sdxVerticalScaleKey, VerticalScalingTestDto.class)
                .withGroup("master")
                .withInstanceType(xlargeInstanceType)
                .given(distroxVerticalScaleKey, VerticalScalingTestDto.class)
                .withGroup("master")
                .withInstanceType(xlargeInstanceType)
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.verticalScale(freeipaVerticalScaleKey))
                .awaitForFlow()
                .given(FreeIpaTestDto.class)
                .then((tc, dto, client) -> {
                    String environmentCrn = tc.get(EnvironmentTestDto.class).getCrn();
                    DescribeFreeIpaResponse freeIpaResponse = client.getDefaultClient().getFreeIpaV1Endpoint().describe(environmentCrn);
                    String instanceType = freeIpaResponse.getInstanceGroups().getFirst().getInstanceTemplate().getInstanceType();
                    if (!instanceType.equals(xlargeInstanceType)) {
                        throw new TestFailException("Vertical scaled instance type should be the same, freeipa instance type " + instanceType);
                    }
                    return dto;
                })
                .given(DistroXTestDto.class)
                .when(distroXClient.verticalScale(distroxVerticalScaleKey))
                .awaitForFlow()
                .then((tc, dto, client) -> {
                    StackV4Response stackV4Response = client.getDefaultClient().distroXV1Endpoint().getByName(dto.getName(), Set.of());
                    String instanceType = stackV4Response.getInstanceGroups().stream().filter(ig -> ig.getName().equals("master"))
                            .findFirst().get().getTemplate().getInstanceType();
                    if (!instanceType.equals(xlargeInstanceType)) {
                        throw new TestFailException("Vertical scaled instance type should be the same: " + instanceType);
                    }
                    return dto;
                })
                .given(SdxInternalTestDto.class)
                .when(sdxTestClient.verticalScale(distroxVerticalScaleKey))
                .awaitForFlow()
                .then((tc, dto, client) -> {
                    CloudbreakClient cbClient = tc.getMicroserviceClient(CloudbreakClient.class);
                    StackV4Response stackV4Response = cbClient.getDefaultClient().stackV4Endpoint().getByCrn(0L, dto.getCrn(), Set.of());
                    String instanceType = stackV4Response.getInstanceGroups().stream().filter(ig -> ig.getName().equals("master"))
                            .findFirst().get().getTemplate().getInstanceType();
                    if (!instanceType.equals(xlargeInstanceType)) {
                        throw new TestFailException("Vertical scaled instance type should be the same: " + instanceType);
                    }
                    return dto;
                })
                .validate();
    }

    @Override
    protected BlueprintTestClient blueprintTestClient() {
        return blueprintTestClient;
    }
}