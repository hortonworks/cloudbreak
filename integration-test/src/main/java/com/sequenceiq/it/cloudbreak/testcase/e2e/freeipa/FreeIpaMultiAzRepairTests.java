package com.sequenceiq.it.cloudbreak.testcase.e2e.freeipa;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.waitForFlow;

import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.instance.InstanceMetadataType;
import com.sequenceiq.freeipa.api.v1.operation.model.OperationType;
import com.sequenceiq.it.cloudbreak.assertion.selinux.SELinuxAssertions;
import com.sequenceiq.it.cloudbreak.client.FreeIpaTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;

public class FreeIpaMultiAzRepairTests extends AbstractFreeipaE2ETest {

    private static final Logger LOGGER = LoggerFactory.getLogger(FreeIpaMultiAzRepairTests.class);

    @Inject
    private FreeIpaTestClient freeIpaTestClient;

    @Inject
    private SELinuxAssertions selinuxAssertions;

    @Override
    protected void setupTest(TestContext testContext) {
        initializeTest(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @Description(
            given = "there is a running cloudbreak",
            when = "a valid MultiAz stack create request is sent with 3 FreeIPA instances " +
                    "AND the MultiAz stack is repaired",
            then = "the MultiAz stack should be available AND deletable")
    public void testHAFreeIpaMultiAzRepair(TestContext testContext) {

        setUpEnvironmentTestDto(testContext, Boolean.TRUE, 3)
                .withEnableMultiAzFreeIpa()
                .withFreeIpaSeLinux(SeLinux.ENFORCING)
                .when(getEnvironmentTestClient().create())
                .awaitForCreationFlow()
                .given(FreeIpaTestDto.class)
                .when(freeIpaTestClient.describe())
                .await(FREEIPA_AVAILABLE)
                .awaitForHealthyInstances()
                .then((tc, testDto, client) -> selinuxAssertions.validateAll(tc, testDto, false))
                .useAlternativeServiceEndpointIfConfigured()
                .when(freeIpaTestClient.repair(InstanceMetadataType.GATEWAY_PRIMARY))
                .await(FREEIPA_AVAILABLE)
                .then((tc, testDto, client) -> {
                    validateMultiAz(testDto.getRequest().getEnvironmentCrn(), client, testDto.getName(), tc, OperationType.REPAIR);
                    return testDto;
                })
                .then((tc, testDto, client) -> selinuxAssertions.validateAll(tc, testDto, false))
                .then((tc, testDto, client) -> freeIpaTestClient.delete().action(tc, testDto, client))
                .await(FREEIPA_DELETE_COMPLETED, waitForFlow().withWaitForFlow(Boolean.FALSE))
                .validate();
    }

}
