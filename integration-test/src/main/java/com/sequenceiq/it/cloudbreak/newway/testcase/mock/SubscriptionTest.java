package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.ws.rs.BadRequestException;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.subscription.SubscriptionTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.CommonAssert;
import com.sequenceiq.it.cloudbreak.newway.assertion.subscription.SubscriptionAssertion;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
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

    @Test(dataProvider = DATA_PROVIDER_FOR_VALID_SUBSCRIPTION_TEST, enabled = false)
    public void testGetSubscription(MockedTestContext testContext, String endpointUrl,
            @Description TestCaseDescription testCaseDescription) {
        testContext
                .given(SubscriptionTestDto.class)
                .withEndpointUrl(endpointUrl)
                .when(SubscriptionTestAction::getSubscribe)
                .then(CommonAssert::responseExists)
                .then(SubscriptionAssertion::idExists)
                .validate();
    }

    @Test(dataProvider = DATA_PROVIDER_FOR_INVALID_SUBSCRIPTION_TEST, enabled = false)
    public void testGetSubscriptionWithInvalidData(
            MockedTestContext testContext,
            String endpointUrl,
            @Description TestCaseDescription testCaseDescription) {
        String subscriptionKey = getNameGenerator().getRandomNameForResource();
        testContext
                .given(SubscriptionTestDto.class)
                .withEndpointUrl(endpointUrl)
                .when(SubscriptionTestAction::getSubscribe, key(subscriptionKey))
                .expect(BadRequestException.class, key(subscriptionKey))
                .validate();
    }

    @DataProvider(name = DATA_PROVIDER_FOR_VALID_SUBSCRIPTION_TEST)
    public Object[][] provideValidAttributes() {
        var testContext = getBean(MockedTestContext.class);
        return new Object[][]{
                {
                        testContext,
                        "https://localhost//",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("Using localhost as subscription address with valid url")
                                .when("calling subscribe endpoint")
                                .then("returns with valid record and the the subscribe happened")
                },
                {
                        testContext,
                        "https://1.1.1.1//",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("Using 1.1.1.1 as subscription address with valid url")
                                .when("calling subscribe endpoint")
                                .then("returns with valid record and the the subscribe happened")
                },
                {
                        testContext,
                        "https://1.1.1.1:11111//",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("Using 1.1.1.1:1111 as subscription address with valid url")
                                .when("calling subscribe endpoint")
                                .then("returns with valid record and the the subscribe happened")
                }
        };
    }

    @DataProvider(name = DATA_PROVIDER_FOR_INVALID_SUBSCRIPTION_TEST)
    public Object[][] provideInvalidAttributes() {
        var testContext = getBean(MockedTestContext.class);
        return new Object[][]{
                {
                        testContext,
                        "localhost",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("Using localhost as subscription address with invalid url without http://")
                                .when("calling subscribe endpoint")
                                .then("returns with BadRequestException because the request is invalid")
                },
                {
                        testContext,
                        "https://1.1.1.1",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("Using 1.1.1.1 as subscription address with invalid url")
                                .when("calling subscribe endpoint")
                                .then("returns with BadRequestException because the request is invalid")
                },
                {
                        testContext,
                        "https://1.1.1.1://",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("Using 1.1.1.1 as subscription address with invalid url where the end of url is ://")
                                .when("calling subscribe endpoint")
                                .then("returns with BadRequestException because the request is invalid")
                }
        };
    }

}
