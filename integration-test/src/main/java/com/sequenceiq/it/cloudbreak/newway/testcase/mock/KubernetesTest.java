package com.sequenceiq.it.cloudbreak.newway.testcase.mock;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.assertion.kubernetes.KubernetesTestAssertion;
import com.sequenceiq.it.cloudbreak.newway.client.KubernetesTestClient;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.cloudbreak.newway.testcase.AbstractIntegrationTest;

public class KubernetesTest extends AbstractIntegrationTest {

    private static final String BAD_REQUEST_KEY = "badRequest";

    private static final String INVALID_ATTRIBUTE_PROVIDER = "kubernetesInvalidAttributesTestProvider";

    private static final String KUBERNETES_CONTENT = "content";

    @Inject
    private KubernetesTestClient kubernetesTestClient;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((TestContext) data[0]).cleanupTestContext();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a valid kubernetes config request",
            when = "calling create kubernetes config",
            then = "the kubernetes list contains the newly created entity")
    public void testKubernetesCreationWithCorrectRequest(MockedTestContext testContext) {
        String kubernetesName = resourcePropertyProvider().getName();
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .when(kubernetesTestClient.createV4())
                .when(kubernetesTestClient.listV4())
                .then(KubernetesTestAssertion.listContains(kubernetesName, 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created kubernetes config",
            when = "calling delete that specific kubernetes config",
            then = "the kubernetes list does not contain the entity")
    public void testCreateDeleteCreate(TestContext testContext) {
        String kubernetesName = resourcePropertyProvider().getName();
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .when(kubernetesTestClient.createV4())
                .when(kubernetesTestClient.listV4())
                .then(KubernetesTestAssertion.listContains(kubernetesName, 1))
                .withName(kubernetesName)
                .when(kubernetesTestClient.deleteV4())
                .when(kubernetesTestClient.listV4())
                .then(KubernetesTestAssertion.listContains(kubernetesName, 0))
                .withName(kubernetesName)
                .when(kubernetesTestClient.createV4())
                .when(kubernetesTestClient.listV4())
                .then(KubernetesTestAssertion.listContains(kubernetesName, 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    @Description(
            given = "a created kubernetes config",
            when = "calling create that kubernetes config again",
            then = "getting BadRequestException because two kubernetes config with same name should not exist")
    public void testCreateTwice(TestContext testContext) {
        String kubernetesName = resourcePropertyProvider().getName();
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .when(kubernetesTestClient.createV4())
                .when(kubernetesTestClient.listV4())
                .then(KubernetesTestAssertion.listContains(kubernetesName, 1))
                .when(kubernetesTestClient.createV4(), key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = INVALID_ATTRIBUTE_PROVIDER)
    public void testCreatKubernetesWithInvalidAttribute(
            TestContext testContext,
            String kubernetesName,
            String content,
            String expectedErrorMessage,
            @Description TestCaseDescription testCaseDescription) {
        String badRequestKey = resourcePropertyProvider().getName();
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .withContent(content)
                .when(kubernetesTestClient.createV4(), key(badRequestKey))
                .expect(BadRequestException.class, expectedMessage(expectedErrorMessage).withKey(badRequestKey))
                .validate();
    }

    @DataProvider(name = INVALID_ATTRIBUTE_PROVIDER)
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {
                        getBean(MockedTestContext.class),
                        getLongNameGenerator().stringGenerator(101),
                        KUBERNETES_CONTENT,
                        "The length of the config's name has to be in range of 5 to 100",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a kubernetes config with too long name")
                                .when("calling create kubernetes configuration")
                                .then("getting BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        "abc",
                        KUBERNETES_CONTENT,
                        "The length of the config's name has to be in range of 5 to 100",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a kubernetes config with too short name")
                                .when("calling create kubernetes configuration")
                                .then("getting BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        "a-@#$%|:&*;",
                        KUBERNETES_CONTENT,
                        "The config's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a kubernetes config with specific character in the name")
                                .when("calling create kubernetes configuration")
                                .then("getting BadRequestException")
                },
                {
                        getBean(MockedTestContext.class),
                        resourcePropertyProvider().getName(),
                        null,
                        "must not be null",
                        new TestCaseDescription.TestCaseDescriptionBuilder()
                                .given("a valid stack request and a Active Directory based kerberos configuration")
                                .when("calling calling create kerberos configuration and a cluster creation with that kerberos configuration")
                                .then("the cluster should be available")
                }
        };
    }
}
