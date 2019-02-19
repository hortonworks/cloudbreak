package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;

import org.springframework.http.HttpMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class AmbariSetupTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        minimalSetupForClusterCreation((MockedTestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void verifyCallsAgainstAmbariUserCreation(TestContext testContext) {
        testContext
                // create stack
                .given(StackTestDto.class)
                .when(Stack.postV4(), key("stack-post"))
                .await(STACK_AVAILABLE)
                .then(MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users").exactTimes(2).bodyContains("\"Users/active\": true"))
                .then(MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users").exactTimes(2).bodyContains("\"Users/admin\": true"))
                .then(MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users").exactTimes(1).bodyContains("\"Users/user_name\": \"cloudbreak\""))
                .then(MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users").exactTimes(1).bodyContains("\"Users/user_name\": \"dpapps\""))
                .then(MockVerification.verify(HttpMethod.POST, AMBARI_API_ROOT + "/users").atLeast(1).bodyContains("Users/password"))
                .validate();
    }
}
