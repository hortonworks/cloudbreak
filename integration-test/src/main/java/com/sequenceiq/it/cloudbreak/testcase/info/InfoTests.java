package com.sequenceiq.it.cloudbreak.testcase.info;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.assertion.info.InfoTestAssertion;
import com.sequenceiq.it.cloudbreak.client.CloudbreakInfoTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.info.CloudbreakInfoTestDto;
import com.sequenceiq.it.cloudbreak.testcase.AbstractIntegrationTest;

public class InfoTests extends AbstractIntegrationTest {

    @Inject
    private CloudbreakInfoTestClient cloudbreakInfoTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "there is a running cloudbreak",
            when = "calling the info endpoint",
            then = "the service has to report the status")
    public void testCloudbreakRunning(TestContext testContext) {

        testContext
                .given(CloudbreakInfoTestDto.class)
                .then((tc, testDto, cc) -> cloudbreakInfoTestClient.get().action(tc, testDto, cc))
                .then(InfoTestAssertion.infoContainsProperties("cloudbreak"))
                .validate();
    }
}
