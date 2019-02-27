package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;
import static com.sequenceiq.it.spark.ITResponse.AMBARI_API_ROOT;

import org.springframework.http.HttpMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.Environment;
import com.sequenceiq.it.cloudbreak.newway.EnvironmentEntity;
import com.sequenceiq.it.cloudbreak.newway.Stack;
import com.sequenceiq.it.cloudbreak.newway.assertion.MockVerification;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.stack.StackTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class AmbariSetupTest extends AbstractIntegrationTest {

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        minimalSetupForClusterCreation((MockedTestContext) data[0]);
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a working environment",
            when = "a stack is created",
            then = "Ambari user endpoints should be invoked with the proper requests")
    public void verifyCallsAgainstAmbariUserCreation(TestContext testContext) {
        String generatedKey = getNameGenerator().getRandomNameForResource();
        String envName = getNameGenerator().getRandomNameForResource();

        testContext
                .given(EnvironmentEntity.class)
                .withName(envName)
                .then(Environment::post)
                .given(StackTestDto.class)
                .when(Stack.postV4())
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
