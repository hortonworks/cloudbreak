package com.sequenceiq.it.cloudbreak.testcase.e2e.distrox;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.assertion.distrox.AwsAvailabilityZoneAssertion;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DistroXTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.SdxTestClient;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.testcase.e2e.AbstractE2ETest;
import com.sequenceiq.it.cloudbreak.util.spot.UseSpotInstances;

public class DistroXCustomTest extends AbstractE2ETest {

    @Inject
    private DistroXTestClient distroXTestClient;

    @Inject
    private TestParameter testParameter;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private SdxTestClient sdxTestClient;

    @Override
    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        initializeDefaultBlueprints(testContext);
    }

    @Test(dataProvider = TEST_CONTEXT)
    @UseSpotInstances
    @Description(
            given = "there is a running cloudbreak and existin environment, distrox, datalake",
            when = "a valid DistroX",
            then = "DistroX cluster is mutliaz")
    public void testForUserToRunExistingResources(TestContext testContext) {

        testContext
                .given(CredentialTestDto.class).withName("aws-test-ee4d66dfa2")
                .when(credentialTestClient.get())
                .given(EnvironmentTestDto.class).withName("aws-test-b81917ea675a4b5bb08")
                .when(environmentTestClient.describe())
                .given(SdxInternalTestDto.class).withName("aws-test-7366cd93a9")
                .when(sdxTestClient.describeInternal())
                .given(DistroXTestDto.class).withName("aws-test-770038")
                .when(distroXTestClient.get())
                .then(new AwsAvailabilityZoneAssertion())
                .validate();
    }
}
