package com.sequenceiq.it.cloudbreak.testcase;

import static com.sequenceiq.it.cloudbreak.context.MeasuredTestContext.createMeasuredTestContext;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.key;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.DataProvider;

import com.google.common.base.Strings;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.environment.api.v1.environment.model.EnvironmentNetworkMockParams;
import com.sequenceiq.environment.api.v1.environment.model.request.EnvironmentNetworkRequest;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.cloudbreak.ResourcePropertyProvider;
import com.sequenceiq.it.cloudbreak.action.v4.imagecatalog.ImageCatalogCreateRetryAction;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.client.BlueprintTestClient;
import com.sequenceiq.it.cloudbreak.client.CredentialTestClient;
import com.sequenceiq.it.cloudbreak.client.DatabaseTestClient;
import com.sequenceiq.it.cloudbreak.client.EnvironmentTestClient;
import com.sequenceiq.it.cloudbreak.client.FreeIPATestClient;
import com.sequenceiq.it.cloudbreak.client.ImageCatalogTestClient;
import com.sequenceiq.it.cloudbreak.client.KerberosTestClient;
import com.sequenceiq.it.cloudbreak.client.LdapTestClient;
import com.sequenceiq.it.cloudbreak.client.ProxyTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.context.Description;
import com.sequenceiq.it.cloudbreak.context.MeasuredTestContext;
import com.sequenceiq.it.cloudbreak.context.MockedTestContext;
import com.sequenceiq.it.cloudbreak.context.PurgeGarbageService;
import com.sequenceiq.it.cloudbreak.context.SparklessTestContext;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription;
import com.sequenceiq.it.cloudbreak.context.TestCaseDescription.TestCaseDescriptionBuilder;
import com.sequenceiq.it.cloudbreak.context.TestContext;
import com.sequenceiq.it.cloudbreak.dto.blueprint.BlueprintTestDto;
import com.sequenceiq.it.cloudbreak.dto.credential.CredentialTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.DatabaseTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentNetworkTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.imagecatalog.ImageCatalogTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.ActiveDirectoryKerberosDescriptorTestDto;
import com.sequenceiq.it.cloudbreak.dto.kerberos.KerberosTestDto;
import com.sequenceiq.it.cloudbreak.dto.ldap.LdapTestDto;
import com.sequenceiq.it.cloudbreak.dto.telemetry.TelemetryTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestCaseDescriptionMissingException;
import com.sequenceiq.it.cloudbreak.mock.ITResponse;
import com.sequenceiq.it.cloudbreak.mock.ThreadLocalProfiles;
import com.sequenceiq.it.cloudbreak.mock.freeipa.FreeIpaRouteHandler;
import com.sequenceiq.it.cloudbreak.spark.DynamicRouteStack;
import com.sequenceiq.it.cloudbreak.util.azure.azurecloudblob.AzureCloudBlobUtil;
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
    private FreeIPATestClient freeIPATestClient;

    @Inject
    private FreeIpaRouteHandler freeIpaRouteHandler;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Value("${integrationtest.cleanup.purge:false}")
    private boolean purge;

    @Inject
    private AzureCloudBlobUtil azureCloudBlobUtil;

    @BeforeSuite
    public void beforeSuite(ITestContext testngContext) {
        MDC.put("testlabel", "init of " + getClass().getSimpleName());
    }

    @BeforeClass
    public void createSharedObjects() {
        if (purge) {
            String testClassName = getClass().getSimpleName();
            MDC.put("testlabel", "Purge: " + testClassName);
            // TODO: refactor this mechanism
            testProfiles().forEach(ThreadLocalProfiles::setProfile);
            applicationContext.getBean(PurgeGarbageService.class).purge();
        } else {
            LOGGER.info("Purge disabled for {}", getClass().getSimpleName());
        }
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
        createDefaultEnvironmentWithNetwork(testContext);
        createDefaultFreeIPA(testContext);
        createDefaultImageCatalog(testContext);
        initializeDefaultBlueprints(testContext);
    }

    public EnvironmentTestClient getEnvironmentTestClient() {
        return environmentTestClient;
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
        ThreadLocalProfiles.clearProfiles();
    }

    @DataProvider(name = TEST_CONTEXT_WITH_MOCK)
    public Object[][] testContextWithMock() {
        MeasuredTestContext tc = createMeasuredTestContext(getBean(MockedTestContext.class));
        return new Object[][]{{tc}};
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

    protected void createDefaultFreeIPA(TestContext testContext) {
        MockedTestContext mockedTestContext = (MockedTestContext) testContext;
        setUpFreeIpaRouteStubbing(mockedTestContext);
        testContext
                .given(FreeIPATestDto.class).withCatalog(mockedTestContext.getImageCatalogMockServerSetup().getFreeIpaImageCatalogUrl())
                .when(freeIPATestClient.create())
                .await(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.AVAILABLE)
                .validate();
    }

    protected void setUpFreeIpaRouteStubbing(MockedTestContext mockedTestContext) {
        DynamicRouteStack dynamicRouteStack = mockedTestContext.getModel().getClouderaManagerMock().getDynamicRouteStack();
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/login_password", (request, response) -> {
            response.cookie("ipa_session", "dummysession");
            return "";
        });
        dynamicRouteStack.post(ITResponse.FREEIPA_ROOT + "/session/json", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/session/json", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/user_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/user_mod", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/role_add_member", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/cert_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/host_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/service_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/dnszone_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/dnsrecord_find", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/host_del", freeIpaRouteHandler);
        dynamicRouteStack.get(ITResponse.FREEIPA_ROOT + "/role_find", freeIpaRouteHandler);
    }

    protected void createDefaultEnvironment(TestContext testContext) {
        testContext.given(EnvironmentTestDto.class)
                .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe());
    }

    protected void createDefaultEnvironmentWithNetwork(TestContext testContext) {
        testContext
                .given(EnvironmentNetworkTestDto.class)
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.FALSE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe());
    }

    protected void createDefaultCredential(TestContext testContext) {
        testContext.given(CredentialTestDto.class)
                .when(credentialTestClient.create());
    }

    protected void createDefaultImageCatalog(TestContext testContext) {
        testContext
                .given(ImageCatalogTestDto.class)
                .when(new ImageCatalogCreateRetryAction());
    }

    protected Set<String> createDefaultLdapConfig(TestContext testContext) {
        testContext
                .given(LdapTestDto.class)
                .when(ldapTestClient.createIfNotExistV1());
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
                .when(kerberosTestClient.createV1());
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

    protected void createEnvironmentWithNetworkAndFreeIPA(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.TRUE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe());
    }

    protected void createEnvironmentWithoutTelemetry(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withCreateFreeIpa(Boolean.TRUE)
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe());
    }

    protected void createEnvironmentForSdx(TestContext testContext) {
        testContext
                .given("telemetry", TelemetryTestDto.class)
                .withLogging()
                .given(EnvironmentTestDto.class)
                .withNetwork()
                .withTelemetry("telemetry")
                .withCreateFreeIpa(Boolean.TRUE)
                .withS3Guard()
                .when(environmentTestClient.create())
                .await(EnvironmentStatus.AVAILABLE)
                .when(environmentTestClient.describe());
    }

    protected void createImageCatalog(TestContext testContext, String name) {
        testContext
                .given(ImageCatalogTestDto.class)
                .withName(name)
                .when(imageCatalogTestClient.createV4(), key(name));
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

    protected void initializeAzureCloudStorage(TestContext testContext) {
        azureCloudBlobUtil.createContainerIfNotExist();
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
        LOGGER.info("Getting for test bean: {}", requiredType.getName());
        if (applicationContext != null) {
            try {
                T bean = applicationContext.getBean(requiredType);
                LOGGER.info("bean created. ref: {}", bean);
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

    protected EnvironmentNetworkRequest environmentNetwork() {
        EnvironmentNetworkRequest networkReq = new EnvironmentNetworkRequest();
        networkReq.setNetworkCidr("0.0.0.0/0");
        EnvironmentNetworkMockParams mockReq = new EnvironmentNetworkMockParams();
        mockReq.setVpcId("vepeceajdi");
        mockReq.setInternetGatewayId("1.1.1.1");
        networkReq.setMock(mockReq);
        networkReq.setSubnetIds(Set.of("net1", "net2"));
        return networkReq;
    }

    protected List<String> testProfiles() {
        return Collections.emptyList();
    }
}
