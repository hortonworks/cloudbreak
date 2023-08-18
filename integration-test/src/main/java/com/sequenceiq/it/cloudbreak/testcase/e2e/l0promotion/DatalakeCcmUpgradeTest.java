package com.sequenceiq.it.cloudbreak.testcase.e2e.l0promotion;

import static java.lang.String.format;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationState;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaUserSyncTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.CloudFunctionality;
import com.sequenceiq.it.cloudbreak.util.EnvironmentUtil;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public class DatalakeCcmUpgradeTest extends AbstractE2ETest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatalakeCcmUpgradeTest.class);

    private static final String EXPORT_IS_CCM_V_2_JUMPGATE_ENABLED_TRUE = "export IS_CCM_V2_JUMPGATE_ENABLED=true";

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Inject
    private EnvironmentUtil environmentUtil;

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        testContext.getCloudProvider().getCloudFunctionality().cloudStorageInitialize();
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
        createDefaultCredential(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "There is a running environment with datalake connected via CCMv1",
            when = "CCM Upgrade called on the environment - CCMv1 to the latest (JUMPGATE)",
            then = "environment CCM Upgrade should be successful, along with datalake.")
    public void testCcmV1Upgrade(TestContext testContext) {
        createEnvironmentWithCcm(testContext, Tunnel.CCM);
        createSdxForEnvironment(testContext);
        upgradeCcmOnEnvironment(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "There is a running environment with datalake connected via CCMv2",
            when = "CCM Upgrade called on the environment - CCMv2 to the latest (JUMPGATE)",
            then = "environment CCM Upgrade should be successful, along with datalake.")
    public void testCcmV2Upgrade(TestContext testContext) {
        createEnvironmentWithCcm(testContext, Tunnel.CCMV2);
        createSdxForEnvironment(testContext);
        upgradeCcmOnEnvironment(testContext);
    }

    private void createEnvironmentWithCcm(TestContext testContext, Tunnel ccmVersion) {
        environmentUtil
                .createEnvironmentWithDefinedCcm(testContext, ccmVersion)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe())
                .given(FreeIpaUserSyncTestDto.class)
                .when(freeIpaTestClient.getLastSyncOperationStatus())
                .await(OperationState.COMPLETED)
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .validate();
    }

    private void createSdxForEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.describe())
                .given(SdxInternalTestDto.class)
                    .withEnvironment()
                    .withoutDatabase()
                .when(sdxTestClient.createInternal())
                .await(SdxClusterStatusResponse.RUNNING)
                .awaitForHealthyInstances()
                .validate();
    }

    private void upgradeCcmOnEnvironment(TestContext testContext) {
        testContext
                .given(EnvironmentTestDto.class)
                .when(environmentTestClient.upgradeCcm())
                .await(EnvironmentStatus.AVAILABLE)
                .then(validateCcmUpgradeOnEnvironment())
                .validate();
    }

    private Assertion<EnvironmentTestDto, EnvironmentClient> validateCcmUpgradeOnEnvironment() {
        return (testContext1, sdxInternalTestDto, environmentClient) -> {
            CloudFunctionality cloudFunctionality = testContext1.getCloudProvider().getCloudFunctionality();
            Map<String, String> launchTemplateUserData = cloudFunctionality.getLaunchTemplateUserData(sdxInternalTestDto.getName());
            boolean ccmV2Enabled = launchTemplateUserData.entrySet().stream().allMatch(ud -> {
                Pattern p = Pattern.compile(EXPORT_IS_CCM_V_2_JUMPGATE_ENABLED_TRUE);
                Matcher m = p.matcher(ud.getValue());

                boolean result = m.find();
                if (!result) {
                    Log.then(LOGGER,
                            format("the %s launch template user data does not contain %s ", ud.getKey(), EXPORT_IS_CCM_V_2_JUMPGATE_ENABLED_TRUE));
                }
                return result;
            });
            if (!ccmV2Enabled) {
                throw new TestFailException(format("user data is not updated by %s", EXPORT_IS_CCM_V_2_JUMPGATE_ENABLED_TRUE));
            }
            return sdxInternalTestDto;
        };
    }
}
