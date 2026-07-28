package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import java.util.List;
import java.util.Locale;

import jakarta.inject.Inject;

import org.assertj.core.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetaDataResponse;
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

    private static final int HA_FREEIPA_INSTANCE_COUNT = 2;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected int getFreeIpaInstanceCountByProvider(TestContext testContext) {
        return HA_FREEIPA_INSTANCE_COUNT;
    }

    @Test(dataProvider = TEST_CONTEXT, timeOut = 9000000)
    @Description(
            given = "there is an available environment with an HA FreeIPA (2 nodes)",
            when = "a vertical scale request is sent with orchestratorType=ONE_BY_ONE",
            then = "each FreeIPA instance is resized one by one without cluster downtime, " +
                    "and the cluster returns to AVAILABLE with the new instance type on all nodes as confirmed by the cloud provider"
    )
    public void testFreeIpaRollingVerticalScale(TestContext testContext) {
        testContext
                .given(FREEIPA_VERTICAL_SCALE_KEY, VerticalScalingTestDto.class)
                    .withFreeipaVerticalScale()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .given(EnvironmentTestDto.class)
                .when(freeIpaTestClient.rollingVerticalScale(FREEIPA_VERTICAL_SCALE_KEY))
                .awaitForFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(Status.AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> {
                    validateInstanceTypesOnProvider(tc, testDto);
                    return testDto;
                })
                .validate();
    }

    private void validateInstanceTypesOnProvider(TestContext testContext, FreeIpaTestDto testDto) {
        VerticalScalingTestDto verticalScalingTestDto = testContext.get(FREEIPA_VERTICAL_SCALE_KEY);
        String expectedInstanceType = verticalScalingTestDto.getInstanceType();
        String expectedGroup = verticalScalingTestDto.getGroupName();

        LOGGER.info("Validating on cloud provider that all instances in group '{}' have instance type '{}'",
                expectedGroup, expectedInstanceType);

        List<String> instanceIds = testDto.getResponse().getInstanceGroups().stream()
                .filter(ig -> ig.getName().equals(expectedGroup))
                .findFirst()
                .orElseThrow(() -> new TestFailException(
                        "Instance group '" + expectedGroup + "' not found in FreeIPA describe response"))
                .getMetaData().stream()
                .map(InstanceMetaDataResponse::getInstanceId)
                .toList();

        if (instanceIds.isEmpty()) {
            throw new TestFailException("No instances found in group '" + expectedGroup + "' after rolling vertical scale");
        }

        List<String> actualTypes = testContext.getCloudProvider().getCloudFunctionality()
                .listInstanceTypes(testDto.getResponse().getName(), instanceIds);

        Assertions.assertThat(actualTypes)
                .withFailMessage("Expected cloud provider to report instance type for all %d instances in group '%s', but got %d results",
                        instanceIds.size(), expectedGroup, actualTypes.size())
                .hasSize(instanceIds.size());

        actualTypes.forEach(actualType ->
                Assertions.assertThat(actualType.toLowerCase(Locale.ROOT))
                        .withFailMessage("Rolling vertical scale did not update instance type on provider for group '%s': expected '%s' but found '%s'",
                                expectedGroup, expectedInstanceType.toLowerCase(Locale.ROOT), actualType)
                        .isEqualTo(expectedInstanceType.toLowerCase(Locale.ROOT)));
    }
}
