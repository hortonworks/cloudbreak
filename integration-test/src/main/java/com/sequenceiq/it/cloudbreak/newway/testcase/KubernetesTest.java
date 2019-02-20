package com.sequenceiq.it.cloudbreak.newway.testcase;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.expectedMessage;
import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.key;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.sequenceiq.it.cloudbreak.newway.action.kubernetes.KubernetesTestAction;
import com.sequenceiq.it.cloudbreak.newway.assertion.kubernetes.KubernetesTestAssertion;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.entity.kubernetes.KubernetesTestDto;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

public class KubernetesTest extends AbstractIntegrationTest {

    private static final String BAD_REQUEST_KEY = "badRequest";

    private static final String INVALID_ATTRIBUTE_PROVIDER = "kubernetesInvalidAttirbutesTestProvider";

    private static final String KUBERNETES_CONTENT = "content";

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @BeforeMethod
    public void beforeMethod(Object[] data) {
        TestContext testContext = (TestContext) data[0];
        createDefaultUser(testContext);
    }

    @AfterMethod(alwaysRun = true)
    public void tear(Object[] data) {
        ((TestContext) data[0]).cleanupTestContextEntity();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testKubernetesCreationWithCorrectRequest(MockedTestContext testContext) {
        String kubernetesName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .when(KubernetesTestAction::create)
                .when(KubernetesTestAction::list)
                .then(KubernetesTestAssertion.listContains(kubernetesName, 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateDeleteCreate(TestContext testContext) {
        String kubernetesName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .when(KubernetesTestAction::create)
                .when(KubernetesTestAction::list)
                .then(KubernetesTestAssertion.listContains(kubernetesName, 1))
                .withName(kubernetesName)
                .when(KubernetesTestAction::delete)
                .when(KubernetesTestAction::list)
                .then(KubernetesTestAssertion.listContains(kubernetesName, 0))
                .withName(kubernetesName)
                .when(KubernetesTestAction::create)
                .when(KubernetesTestAction::list)
                .then(KubernetesTestAssertion.listContains(kubernetesName, 1))
                .validate();
    }

    @Test(dataProvider = TEST_CONTEXT_WITH_MOCK)
    public void testCreateTwice(TestContext testContext) {
        String kubernetesName = getNameGenerator().getRandomNameForMock();
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .when(KubernetesTestAction::create)
                .when(KubernetesTestAction::list)
                .then(KubernetesTestAssertion.listContains(kubernetesName, 1))
                .when(KubernetesTestAction::create, key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, key(BAD_REQUEST_KEY))
                .validate();
    }

    @Test(dataProvider = INVALID_ATTRIBUTE_PROVIDER)
    public void testCreatKubernetesWithInvalidAttribute(TestContext testContext,
                                                        String kubernetesName,
                                                        String content,
                                                        String expectedErrorMessage) {
        testContext
                .given(KubernetesTestDto.class)
                .withName(kubernetesName)
                .withContent(content)
                .when(KubernetesTestAction::create, key(BAD_REQUEST_KEY))
                .expect(BadRequestException.class, expectedMessage(expectedErrorMessage).withKey(BAD_REQUEST_KEY))
                .validate();
    }

    @DataProvider(name = INVALID_ATTRIBUTE_PROVIDER)
    public Object[][] provideInvalidAttributes() {
        return new Object[][]{
                {applicationContext.getBean(TestContext.class), longStringGeneratorUtil.stringGenerator(101), KUBERNETES_CONTENT,
                        " The length of the config's name has to be in range of 5 to 100"},
                {applicationContext.getBean(TestContext.class), "abc", KUBERNETES_CONTENT,
                        " The length of the config's name has to be in range of 5 to 100"},
                {applicationContext.getBean(TestContext.class), "a-@#$%|:&*;", KUBERNETES_CONTENT,
                        " The config's name can only contain lowercase alphanumeric characters and hyphens and has start with an alphanumeric character"},
                {applicationContext.getBean(TestContext.class), getNameGenerator().getRandomNameForMock(), null,
                        "post.arg1.content: null, error: must not be null"}
        };
    }
}
