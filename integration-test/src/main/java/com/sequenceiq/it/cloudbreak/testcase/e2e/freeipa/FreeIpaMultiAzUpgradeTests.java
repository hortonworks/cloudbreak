package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

public class FreeIpaMultiAzUpgradeTests extends AbstractFreeipaE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaMultiAzUpgradeTests.class);

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid MultiAz stack create request is sent with 3 FreeIPA instances " +
                    "AND the MultiAz stack is upgraded one node at a time",
            then = "the MultiAz stack should be available AND deletable")
    public void testHAFreeIpaMultiAzInstanceUpgrade(TestContext testContext) {

        setUpEnvironmentTestDto(testContext, Boolean.TRUE, 3)
                .withEnableMultiAzFreeIpa()
                .withFreeIpaImage(testContext.getCloudProvider().getFreeIpaUpgradeImageCatalog(), testContext.getCloudProvider()
                        .getFreeIpaCentos7UpgradeImageId())
                .when(getEnvironmentTestClient().create())
                .awaitForCreationFlow()
                .useAlternativeServiceEndpointIfConfigured()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .when(freeIpaTestClient.upgrade())
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, testDto.getName(), tc, OperationType.UPGRADE);
                    return testDto;
                })
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .validate();
    }

}
