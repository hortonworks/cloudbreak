package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.subscription.SubscriptionTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.newway.assertion.subscription.SubscriptionAssertion;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.subscription.SubscriptionTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class SubscriptionTest extends AbstractIntegrationTest {

    private static final String DATA_PROVIDER_FOR_VALID_SUBSCRIPTION_TEST = "contextAndValidUrl";

    private static final String DATA_PROVIDER_FOR_INVALID_SUBSCRIPTION_TEST = "contextAndInvalidUrl";

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        createDefaultUser((TestContext) data[0]);
    }

    @Test(dataProvider = DATA_PROVIDER_FOR_VALID_SUBSCRIPTION_TEST)
    public void testGetSubscription(MockedTestContext testContext, String endpointUrl) {
        testContext
                .given(SubscriptionTestDto.class)
                .withEndpointUrl(endpointUrl)
                .when(SubscriptionTestAction::getSubscribe)
                .then(CommonAssert::responseExists)
                .then(SubscriptionAssertion::idExists)
                .validate();
    }

    @Test(dataProvider = DATA_PROVIDER_FOR_INVALID_SUBSCRIPTION_TEST)
    public void testGetSubscriptionWithInvalidData(MockedTestContext testContext, String endpointUrl) {
        testContext
                .given(SubscriptionTestDto.class)
                .withEndpointUrl(endpointUrl)
                .when(SubscriptionTestAction::getSubscribe, key("badRequest"))
                .expect(BadRequestException.class, key("badRequest"))
                .validate();
    }

    @DataProvider(name = DATA_PROVIDER_FOR_VALID_SUBSCRIPTION_TEST)
    public Object[][] provideValidAttributes() {
        var testContext = getBean(MockedTestContext.class);
        return new Object[][] {
                {testContext, "https://localhost//"},
                {testContext, "https://1.1.1.1//"},
                {testContext, "https://1.1.1.1:11111//"}
        };
    }

    @DataProvider(name = DATA_PROVIDER_FOR_INVALID_SUBSCRIPTION_TEST)
    public Object[][] provideInvalidAttributes() {
        var testContext = getBean(MockedTestContext.class);
        return new Object[][] {
                {testContext, "localhost"},
                {testContext, "https://1.1.1.1"},
                {testContext, "https://1.1.1.1://"}
        };
    }

}
