package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceGroupResponse;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.verticalscale.VerticalScalingTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.testcase.e2e.sdx.PreconditionSdxE2ETest;

public class FreeIpaRollingVerticalScaleTest extends PreconditionSdxE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaRollingVerticalScaleTest.class);

    private static final String FREEIPA_VERTICAL_SCALE_KEY = "freeipaVerticalScaleKey";

    private static final String FREEIPA_ROLLING_VERTICAL_SCALE_KEY = "freeipaRollingVerticalScaleKey";

    private static final int HA_FREEIPA_INSTANCE_COUNT = 2;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Override
    protected int getFreeIpaInstanceCountByProvider(TestContext testContext) {
        return HA_FREEIPA_INSTANCE_COUNT;
    }

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with an HA FreeIPA (2 nodes)",
            when = "the environment is stopped and a vertical scale request is sent, then the environment is started "
                    + "and a vertical scale request back to the original instance type is sent with orchestratorType=ALL_AT_ONCE on an entitled account",
            then = "the stopped FreeIPA is resized without being started and the environment stays stopped; "
                    + "then each FreeIPA instance is resized one by one without cluster downtime (entitled ALL_AT_ONCE routes to rolling), "
                    + "the environment ends AVAILABLE, and all nodes have the original instance type as confirmed by the cloud provider"
    )
    public void testFreeIpaRollingVerticalScale(TestContext testContext) {
        testContext
                .given(FREEIPA_VERTICAL_SCALE_KEY, VerticalScalingTestDto.class)
                    .withFreeipaVerticalScale()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .validate();

        VerticalScalingTestDto stoppedVerticalScale = testContext.get(FREEIPA_VERTICAL_SCALE_KEY);
        String group = stoppedVerticalScale.getGroupName();
        String targetInstanceType = stoppedVerticalScale.getInstanceType();
        String originalInstanceType = getInstanceGroup(testContext.get(FreeIpaTestDto.class), group).getInstanceTemplate().getInstanceType();
        if (originalInstanceType.equalsIgnoreCase(targetInstanceType)) {
            throw new TestFailException(String.format("FreeIPA group '%s' is already on the target instance type '%s', the test can't verify the resize",
                    group, originalInstanceType));
        }
        LOGGER.info("FreeIPA group '{}' starts on instance type '{}', the stopped vertical scale target is '{}'", group, originalInstanceType,
                targetInstanceType);

        testContext
                .given(FREEIPA_ROLLING_VERTICAL_SCALE_KEY, VerticalScalingTestDto.class)
                    .withGroup(group)
                    .withInstanceType(originalInstanceType)
                // stopped environment: the FreeIPA is resized without being started, everything stays stopped
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.stop())
                .await(EnvironmentStatus.ENV_STOPPED)
                .when(environmentTestClient.verticalScale(FREEIPA_VERTICAL_SCALE_KEY))
                .awaitForFlow()
                .await(EnvironmentStatus.ENV_STOPPED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(Status.STOPPED)
                .then((tc, testDto, client) -> validateInstanceTypesOnProvider(tc, testDto, group, targetInstanceType))
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.start())
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                // running environment: entitled ALL_AT_ONCE routes to rolling, resizes back to the original instance type
                .useAlternativeServiceEndpointIfConfigured()
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.verticalScale(FREEIPA_ROLLING_VERTICAL_SCALE_KEY))
                .awaitForFlow()
                .await(EnvironmentStatus.AVAILABLE)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> validateInstanceTypesOnProvider(tc, testDto, group, originalInstanceType))
                .validate();
    }

    private InstanceGroupResponse getInstanceGroup(FreeIpaTestDto testDto, String group) {
        return testDto.getResponse().getInstanceGroups().stream()
                .filter(ig -> ig.getName().equals(group))
                .findFirst()
                .orElseThrow(() -> new TestFailException("Instance group '" + group + "' not found in FreeIPA describe response"));
    }

    private FreeIpaTestDto validateInstanceTypesOnProvider(TestContext testContext, FreeIpaTestDto testDto, String expectedGroup,
            String expectedInstanceType) {
        LOGGER.info("Validating on cloud provider that all instances in group '{}' have instance type '{}'",
                expectedGroup, expectedInstanceType);

        List<String> instanceIds = getInstanceGroup(testDto, expectedGroup).getMetaData().stream()
                .map(InstanceMetaDataResponse::getInstanceId)
                .toList();

        if (instanceIds.isEmpty()) {
            throw new TestFailException("No instances found in group '" + expectedGroup + "' after vertical scale");
        }

        List<String> actualTypes = testContext.getCloudProvider().getCloudFunctionality()
                .listInstanceTypes(testDto.getResponse().getName(), instanceIds);

        Assertions.assertThat(actualTypes)
                .withFailMessage("Expected cloud provider to report instance type for all %d instances in group '%s', but got %d results",
                        instanceIds.size(), expectedGroup, actualTypes.size())
                .hasSize(instanceIds.size());

        actualTypes.forEach(actualType ->
                Assertions.assertThat(actualType.toLowerCase(Locale.ROOT))
                        .withFailMessage("Vertical scale did not update instance type on provider for group '%s': expected '%s' but found '%s'",
                                expectedGroup, expectedInstanceType.toLowerCase(Locale.ROOT), actualType)
                        .isEqualTo(expectedInstanceType.toLowerCase(Locale.ROOT)));
        return testDto;
    }
}
