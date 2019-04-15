package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;

import javax.inject.Inject;

import org.springframework.http.HttpMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.StackTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class AmbariSetupTest extends AbstractIntegrationTest {

    @Inject
    private StackTestClient stackTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a working environment",
            when = "a stack is created",
            then = "Ambari user endpoints should be invoked with the proper requests")
    public void verifyCallsAgainstAmbariUserCreation(TestContext testContext) {
        String generatedKey = resourcePropertyProvider().getName();
        String envName = resourcePropertyProvider().getName();

        testContext
                .given(EnvironmentTestDto.class)
                .withName(envName)
                .when(environmentTestClient.createV4())
                .given(StackTestDto.class)
                .when(stackTestClient.createV4())
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users")
                        .exactTimes(2).bodyContains("\"Users/active\": true"), key(generatedKey))
                .then(MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users")
                        .exactTimes(2).bodyContains("\"Users/admin\": true"), key(generatedKey))
                .then(MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users")
                        .exactTimes(1).bodyContains("\"Users/user_name\": \"cloudbreak\""), key(generatedKey))
                .then(MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users")
                        .exactTimes(1).bodyContains("\"Users/user_name\": \"dpapps\""), key(generatedKey))
                .then(MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users")
                        .atLeast(1).bodyContains("Users/password"), key(generatedKey))
                .validate();
    }
}
