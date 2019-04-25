package com.sequenceiq.it.cloudbreak.newway.testcase;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.it.cloudbreak.exception.TestCaseDescriptionMissingException;
import com.sequenceiq.it.cloudbreak.newway.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.newway.actor.Actor;
import com.sequenceiq.it.cloudbreak.newway.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.newway.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.newway.cloud.v2.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.newway.context.Description;
import com.sequenceiq.it.cloudbreak.newway.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.PurgeGarbageService;
import com.sequenceiq.it.cloudbreak.newway.context.SparklessTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.newway.context.TestCaseDescription.TestCaseDescriptionBuilder;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.kerberos.ActiveDirectoryKerberosDescriptorTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.newway.dto.proxy.ProxyTestDto;
import com.sequenceiq.it.config.IntegrationTestConfiguration;
import com.sequenceiq.it.util.LongStringGeneratorUtil;

@ContextConfiguration(classes = {IntegrationTestConfiguration.class}, initializers = ConfigFileApplicationContextInitializer.class)
public abstract class AbstractIntegrationTest extends AbstractTestNGSpringContextTests {

    public static final Map<String, Status> STACK_DELETED = Map.of("status", Status.DELETE_COMPLETED);

    protected static final Map<String, Status> STACK_AVAILABLE = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.AVAILABLE);

    protected static final Map<String, Status> STACK_FAILED = Map.of("status", Status.AVAILABLE, "clusterStatus", Status.CREATE_FAILED);

    protected static final Map<String, Status> STACK_STOPPED = Map.of("status", Status.STOPPED, "clusterStatus", Status.STOPPED);

    protected static final String TEST_CONTEXT_WITH_MOCK = "testContextWithMock";

    protected static final String TEST_CONTEXT = "testContextWithoutMock";

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractIntegrationTest.class);

    @Inject
    private ResourcePropertyProvider resourcePropertyProvider;

    @Inject
    private LongStringGeneratorUtil longStringGeneratorUtil;

    @Inject
    private CredentialTestClient credentialTestClient;

    @Inject
    private EnvironmentTestClient environmentTestClient;

    @Inject
    private ImageCatalogTestClient imageCatalogTestClient;

    @Inject
    private ProxyTestClient proxyTestClient;

    @Inject
    private BlueprintTestClient blueprintTestClient;

    @Inject
    private LdapTestClient ldapTestClient;

    @Inject
    private KerberosTestClient kerberosTestClient;

    @Inject
    private DatabaseTestClient databaseTestClient;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    private final List<AutoCloseable> closableBeans = new CopyOnWriteArrayList<>();

    @BeforeSuite
    public void beforeSuite(ITestContext testngContext) {
        MDC.put("testlabel", "init of " + getClass().getSimpleName());
    }

    @BeforeClass
    public void createSharedObjects() {
        String testClassName = getClass().getSimpleName();
        MDC.put("testlabel", testClassName);
        applicationContext.getBean(PurgeGarbageService.class).purge();
    }

    @BeforeMethod
    public void beforeTest(Method method, Object[] params) {
        MDC.put("testlabel", method.getDeclaringClass().getSimpleName() + '.' + method.getName());
        collectTestCaseDescription(method, params);
    }

    @BeforeMethod
    public final void minimalSetupForClusterCreation(Object[] data) {
        setupTest((TestContext) data[0]);
    }

    protected void setupTest(TestContext testContext) {
        createDefaultUser(testContext);
        createDefaultCredential(testContext);
        createDefaultEnvironment(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    private TestCaseDescription collectTestCaseDescription(Method method, Object[] params) {
        Description declaredAnnotation = method.getDeclaredAnnotation(Description.class);
        TestCaseDescription testCaseDescription = null;
        if (declaredAnnotation != null) {
            testCaseDescription = new TestCaseDescriptionBuilder()
                    .given(declaredAnnotation.given())
                    .when(declaredAnnotation.when())
                    .then(declaredAnnotation.then());
            ((TestContext) params[0]).addDescription(testCaseDescription);
        } else if (method.getParameters().length == params.length) {
            Parameter[] parameters = method.getParameters();
            for (int i = 1; i < parameters.length; i++) {
                if (parameters[i].getAnnotation(Description.class) != null) {
                    Object param = params[i];
                    if (!(param instanceof TestCaseDescription)) {
                        throw new IllegalArgumentException("The param annotated with @Description but the type is should be "
                                + TestCaseDescription.class.getSimpleName());
                    }
                    testCaseDescription = (TestCaseDescription) param;
                    ((TestContext) params[0]).addDescription(testCaseDescription);
                    break;
                }
            }
        }
        return Optional.ofNullable(testCaseDescription)
                .filter(d -> !Strings.isNullOrEmpty(d.getValue()))
                .orElseThrow(() -> new TestCaseDescriptionMissingException(method.getName()));
    }

    @AfterMethod
    public void afterMethod(Method method, ITestResult testResult) {
        MDC.put("testlabel", null);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Object[] data) {
        ((TestContext) data[0]).cleanupTestContext();
    }

    @AfterClass(alwaysRun = true)
    public void cleanSharedObjects() {
        closableBeans.forEach(b -> {
            try {
                b.close();
            } catch (Exception e) {
                LOGGER.error("Unable to close bean: " + b.getClass().getCanonicalName(), e);
            }
        });
    }

    @AfterSuite
    public void afterSuite() {

    }

    @DataProvider(name = TEST_CONTEXT_WITH_MOCK)
    public Object[][] testContextWithMock() {
        MockedTestContext bean = getBean(MockedTestContext.class);
        return new Object[][]{{bean}};
    }

    @DataProvider(name = TEST_CONTEXT)
    public Object[][] testContextWithoutMock() {
        return new Object[][]{{getBean(SparklessTestContext.class)}};
    }

    public ResourcePropertyProvider resourcePropertyProvider() {
        return resourcePropertyProvider;
    }

    public LongStringGeneratorUtil getLongNameGenerator() {
        return longStringGeneratorUtil;
    }

    protected void createDefaultEnvironment(TestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .when(environmentTestClient.createV4());
    }

    protected void createDefaultCredential(TestContext testContext) {
        testContext.given(CredentialTestDto.class)
                .when(credentialTestClient.createV4());
    }

    protected void createDefaultImageCatalog(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(imageCatalogTestClient.createV4());
    }

    protected Set<String> createDefaultProxyConfig(TestContext testContext) {
        testContext
                .given(ProxyTestDto.class)
                .when(proxyTestClient.createIfNotExistV4());
        Set<String> validProxy = new HashSet<>();
        validProxy.add(testContext.get(ProxyTestDto.class).getName());
        return validProxy;
    }

    protected Set<String> createDefaultLdapConfig(TestContext testContext) {
        testContext
                .given(LdapTestDto.class)
                .when(ldapTestClient.createIfNotExistV4());
        Set<String> validLdap = new HashSet<>();
        validLdap.add(testContext.get(LdapTestDto.class).getName());
        return validLdap;
    }

    protected Set<String> createDefaultKerberosConfig(TestContext testContext) {
        testContext
                .given(ActiveDirectoryKerberosDescriptorTestDto.class)
                .withDomain(null)
                .withRealm("realm.addomain.com")
                .given(KerberosTestDto.class)
                .withActiveDirectoryDescriptor()
                .when(kerberosTestClient.createV4());
        Set<String> validKerberos = new HashSet<>();
        validKerberos.add(testContext.get(KerberosTestDto.class).getName());
        return validKerberos;
    }

    protected Set<String> createDefaultRdsConfig(TestContext testContext) {
        testContext
                .given(DatabaseTestDto.class)
                .when(databaseTestClient.createIfNotExistV4());
        Set<String> validRds = new HashSet<>();
        validRds.add(testContext.get(DatabaseTestDto.class).getName());
        return validRds;
    }

    protected void createDefaultUser(TestContext testContext) {
        testContext.as();
    }

    protected void createSecondUser(TestContext testContext) {
        testContext.as(Actor::secondUser);
    }

    protected void initializeDefaultBlueprints(TestContext testContext) {
        testContext
                .init(BlueprintTestDto.class)
                .when(blueprintTestClient.listV4());
    }

    /**
     * Obtains bean from the application context for the given type if both the bean and the application context exists
     *
     * @param requiredType the class of the expected bean
     * @param <T>          generic for the type of the expected bean
     * @return extracted instance from the application context
     * @throws IllegalStateException if no application context exists or bean could not be created
     */
    protected <T> T getBean(Class<T> requiredType) {
        if (applicationContext != null) {
            try {
                T bean = applicationContext.getBean(requiredType);
                if (bean instanceof AutoCloseable) {
                    closableBeans.add((AutoCloseable) bean);
                }
                return bean;
            } catch (BeansException be) {
                throw new IllegalStateException("No bean found!", be);
            }
        }
        throw new IllegalStateException("No application context found!");
    }

    protected CommonCloudProperties commonCloudProperties() {
        return commonCloudProperties;
    }
}
