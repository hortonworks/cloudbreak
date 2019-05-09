package com.sequenceiq.it.cloudbreak.newway.testcase.info;

import static com.sequenceiq.it.cloudbreak.newway.assertion.info.InfoTestAssertion.infoContainsProperties;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.client.CloudbreakInfoTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.info.CloudbreakInfoTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

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
                .then(infoContainsProperties("cloudbreak"))
                .validate();
    }
}
