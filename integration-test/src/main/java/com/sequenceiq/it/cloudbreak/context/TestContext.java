package com.sequenceiq.it.cloudbreak.context;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;
import static java.lang.String.format;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.base64.Base64Util;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.cloudbreak.util.SanitizerUtil;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.client.UmsTestClient;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderAssertionProxy;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.finder.Attribute;
import com.sequenceiq.it.cloudbreak.finder.Capture;
import com.sequenceiq.it.cloudbreak.finder.Finder;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.AuthDistributorClient;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.microservice.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.microservice.PeriscopeClient;
import com.sequenceiq.it.cloudbreak.microservice.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxSaasItClient;
import com.sequenceiq.it.cloudbreak.microservice.UmsClient;
import com.sequenceiq.it.cloudbreak.util.ErrorLogMessageProvider;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.FlowUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.ResourceAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceAwait;
import com.sequenceiq.it.util.TestParameter;

public abstract class TestContext implements ApplicationContextAware {

    public static final String OUTPUT_FAILURE_TYPE = "outputFailureType";

    public static final String OUTPUT_FAILURE = "outputFailure";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

    private static final String DESCRIPTION = "DESCRIPTION";

    private static final String TEST_METHOD_NAME = "TEST_METHOD_NAME";

    private static final String MOCK_UMS_PASSWORD = "Password123!";

    private ApplicationContext applicationContext;

    private final Map<String, CloudbreakTestDto> resourceNames = new ConcurrentHashMap<>();

    private final Map<String, CloudbreakTestDto> resourceCrns = new ConcurrentHashMap<>();

    private final Map<String, Map<Class<? extends MicroserviceClient>, MicroserviceClient>> clients = new HashMap<>();

    private final Map<String, Exception> exceptionMap = new HashMap<>();

    private boolean shutdown;

    private boolean useUmsUserCache;

    private final Map<String, String> statuses = new HashMap<>();

    private final Map<String, Object> selections = new HashMap<>();

    private final Map<String, Capture> captures = new HashMap<>();

    private Map<String, Object> contextParameters = new HashMap<>();

    private TestContext testContext;

    @Inject
    private FlowUtil flowUtilSingleStatus;

    @Inject
    private TestParameter testParameter;

    @Value("${integrationtest.testsuite.cleanUpOnFailure:true}")
    private boolean cleanUpOnFailure;

    @Value("${integrationtest.testsuite.cleanUp:true}")
    private boolean cleanUp;

    @Value("#{'${integrationtest.cloudProvider}'.equals('MOCK') ? 300 : ${integrationtest.testsuite.maxRetry:2700}}")
    private int maxRetry;

    @Value("#{'${integrationtest.cloudProvider}'.equals('MOCK') ? 3 : ${integrationtest.testsuite.maxRetryCount:5}}")
    private int maxRetryCount;

    @Value("${integrationtest.ums.host:localhost}")
    private String umsHost;

    @Value("${integrationtest.ums.port:8982}")
    private int umsPort;

    @Value("${integrationtest.authdistributor.host:localhost}")
    private String authDistributorHost;

    @Value("${integrationtest.cloudbreak.server}")
    private String defaultServer;

    @Value("${integrationtest.user.workloadPassword:}")
    private String workloadPassword;

    @Inject
    private CloudProviderProxy cloudProvider;

    @Inject
    private CloudProviderAssertionProxy cloudProviderAssertion;

    @Inject
    private CommonCloudProperties commonCloudProperties;

    @Inject
    private CommonClusterManagerProperties commonClusterManagerProperties;

    @Inject
    private ResourceAwait resourceAwait;

    @Inject
    private InstanceAwait instanceAwait;

    @Inject
    private ErrorLogMessageProvider errorLogMessageProvider;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    @Inject
    private UmsTestClient umsTestClient;

    private boolean validated;

    private boolean initialized;

    private CloudbreakUser actingUser;

    public String getMockUmsPassword() {
        return MOCK_UMS_PASSWORD;
    }

    public TestParameter getTestParameter() {
        return testParameter;
    }

    public ITestResult getCurrentTestResult() {
        return Reporter.getCurrentTestResult();
    }

    public Date getTestStartTime() {
        Date startTime = getCurrentTestResult().getTestContext().getStartDate();
        if (startTime == null) {
            startTime = new Date(getCurrentTestResult().getStartMillis());
        }
        return startTime;
    }

    public Date getTestEndTime() {
        Date endTime = getCurrentTestResult().getTestContext().getEndDate();
        if (endTime == null) {
            endTime = new Date(getCurrentTestResult().getEndMillis());
        }
        LocalDateTime end = endTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime start = getTestStartTime().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        long duration = ChronoUnit.DAYS.between(start, end);
        if (duration < 0 || duration > 1) {
            endTime = Calendar.getInstance().getTime();
        }
        return endTime;
    }

    public Map<String, CloudbreakTestDto> getResourceNames() {
        return resourceNames;
    }

    public Map<String, CloudbreakTestDto> getResourceCrns() {
        return resourceCrns;
    }

    public Map<String, Map<Class<? extends MicroserviceClient>, MicroserviceClient>> getClients() {
        return clients;
    }

    public Map<String, Exception> getExceptionMap() {
        return exceptionMap;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
    }

    /**
     * We need to explicitly define the usage of real UMS users (automated via
     * 'useRealUmsUser' at 'AbstractIntegrationTest'). So on this way we can avoid
     * accidental usage:
     * - The initialization of the real UMS user store is happening automatically.
     * - If the 'api-credentials.json' is mistakenly present at 'ums-users' folder.
     * When a microservice client or an action is intended to use admin user then
     * a real UMS admin is going to be provided from the initialized user store.
     * By setting 'useUmsUserCache' to 'true' we can define the usage of real UMS user
     * store. Then and only then tests are running with 'useRealUmsUser' the real UMS
     * users are going to be provided from the initialized user store.
     * <p>
     * So we can rest assured MOCK or E2E Cloudbreak tests are going to be run with
     * mock and default test users even the 'ums-users/api-credentials.json' is present
     * and real UMS user store is initialized.
     *
     * @param useUmsUserCache 'true' if user store has been selected for providing
     *                        users for tests
     */
    public void useUmsUserCache(boolean useUmsUserCache) {
        this.useUmsUserCache = useUmsUserCache;
    }

    /**
     * Returning 'true' if tests are running with real UMS users.
     *
     * @return 'true' if real UMS users are used for tests.
     */
    public boolean umsUserCacheInUse() {
        return useUmsUserCache;
    }

    /**
     * Returning 'true' if real UMS users can be used for testing:
     * - user store has been initialized successfully
     * - user store has been selected for providing users by 'useUmsUserCache=true'
     *
     * @return 'true' if real UMS user store has been initialized and
     * selected for use.
     */
    public boolean realUmsUserCacheReadyToUse() {
        return cloudbreakActor.isInitialized() && umsUserCacheInUse();
    }

    public ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public CommonCloudProperties commonCloudProperties() {
        return commonCloudProperties;
    }

    public CommonClusterManagerProperties commonClusterManagerProperties() {
        return commonClusterManagerProperties;
    }

    public ResourceAwait getResourceAwait() {
        return resourceAwait;
    }

    public InstanceAwait getInstanceAwait() {
        return instanceAwait;
    }

    public <E extends Exception, T extends CloudbreakTestDto, U extends MicroserviceClient> T whenException(Class<T> entityClass,
            Class<? extends MicroserviceClient> clientClass, Action<T, U> action, Class<E> expectedException) {
        return whenException(entityClass, clientClass, action, expectedException, emptyRunningParameter());
    }

    public <E extends Exception, T extends CloudbreakTestDto, U extends MicroserviceClient> T whenException(Class<T> entityClass,
            Class<? extends MicroserviceClient> clientClass, Action<T, U> action, Class<E> expectedException, RunningParameter runningParameter) {
        return whenException(getEntityFromEntityClass(entityClass, runningParameter), clientClass, action, expectedException, runningParameter);
    }

    public <E extends Exception, T extends CloudbreakTestDto, U extends MicroserviceClient> T whenException(T entity,
            Class<? extends MicroserviceClient> clientClass, Action<T, U> action, Class<E> expectedException) {
        return whenException(entity, clientClass, action, expectedException, emptyRunningParameter());
    }

    public <E extends Exception, T extends CloudbreakTestDto, U extends MicroserviceClient> T whenException(T entity,
            Class<? extends MicroserviceClient> clientClass, Action<T, U> action, Class<E> expectedException, RunningParameter runningParameter) {
        checkShutdown();
        String key = runningParameter.getKey();
        if (StringUtils.isBlank(key)) {
            key = action.getClass().getSimpleName();
        }
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped because of previous error. when [{}]", key);
            return entity;
        }

        CloudbreakUser who = setActingUser(runningParameter);

        LOGGER.info("when exception {} action on {} by {}, name: {}", key, entity, who, entity.getName());
        Log.whenException(LOGGER, action.getClass().getSimpleName() + " action on " + entity + " by " + who);

        try {
            String message = String.format("Expected exception with message (%s) has not been thrown at action (%s)!",
                    runningParameter.getExpectedMessage(), action);

            doAction(entity, clientClass, action, who.getAccessKey());

            getExceptionMap().put("whenException", new TestFailException(message));
            LOGGER.error(message);
            htmlLoggerForExceptionValidation(message, "whenException");
        } catch (Exception e) {
            exceptionValidation(expectedException, e, key, runningParameter, "whenException");
        }
        return entity;
    }

    public <T extends CloudbreakTestDto, U extends MicroserviceClient> T when(Class<T> entityClass, Class<? extends MicroserviceClient> clientClass,
            Action<T, U> action) {
        return when(entityClass, clientClass, action, emptyRunningParameter());
    }

    public <T extends CloudbreakTestDto, U extends MicroserviceClient> T when(Class<T> entityClass, Class<? extends MicroserviceClient> clientClass,
            Action<T, U> action, RunningParameter runningParameter) {
        return when(getEntityFromEntityClass(entityClass, runningParameter), clientClass, action, runningParameter);
    }

    public <T extends CloudbreakTestDto, U extends MicroserviceClient> T when(T entity, Class<? extends MicroserviceClient> clientClass, Action<T, U> action) {
        return when(entity, clientClass, action, emptyRunningParameter());
    }

    public <T extends CloudbreakTestDto, U extends MicroserviceClient> T when(T entity, Class<? extends MicroserviceClient> clientClass, Action<T, U> action,
            RunningParameter runningParameter) {
        checkShutdown();
        String key = runningParameter.getKey();
        if (StringUtils.isBlank(key)) {
            key = action.getClass().getSimpleName();
        }
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped because of previous error. when [{}]", key);
            return entity;
        }

        CloudbreakUser who = setActingUser(runningParameter);

        LOGGER.info("when {} action on {} by {}, name: {}", key, entity, who, entity.getName());
        Log.when(LOGGER, action.getClass().getSimpleName() + " action on " + entity + " by " + who);

        try {
            return doAction(entity, clientClass, action, who.getAccessKey());
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("when [{}] action is failed: {}, name: {}", key, ResponseUtil.getErrorMessage(e), entity.getName(), e);
                Log.when(null, action.getClass().getSimpleName() + " action is failed: " + ResponseUtil.getErrorMessage(e));
            }
            getExceptionMap().put(key, e);
        }
        return entity;
    }

    public TestContext getTestContext() {
        if (testContext == null) {
            return this;
        } else {
            return testContext;
        }
    }

    protected void setTestContext(TestContext testContext) {
        this.testContext = testContext;
    }

    protected <T extends CloudbreakTestDto, U extends MicroserviceClient>
    T doAction(T entity, Class<? extends MicroserviceClient> clientClass, Action<T, U> action, String who) throws Exception {
        return action.action(getTestContext(), entity, getMicroserviceClient(entity.getClass(), who));
    }

    public <T extends CloudbreakTestDto> T then(Class<T> entityClass, Class<? extends MicroserviceClient> clientClass,
            Assertion<T, ? extends MicroserviceClient> assertion) {
        return then(entityClass, clientClass, assertion, emptyRunningParameter());
    }

    public <T extends CloudbreakTestDto> T then(Class<T> entityClass, Class<? extends MicroserviceClient> clientClass,
            Assertion<T, ? extends MicroserviceClient> assertion, RunningParameter runningParameter) {
        return then(getEntityFromEntityClass(entityClass, runningParameter), clientClass, assertion, emptyRunningParameter());
    }

    public <T extends CloudbreakTestDto> T then(T entity, Class<? extends MicroserviceClient> clientClass,
            Assertion<T, ? extends MicroserviceClient> assertion) {
        return then(entity, clientClass, assertion, emptyRunningParameter());
    }

    public <T extends CloudbreakTestDto> T then(T entity, Class<? extends MicroserviceClient> clientClass,
            Assertion<T, ? extends MicroserviceClient> assertion, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKey(assertion.getClass(), runningParameter);

        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped because of previous error. when [{}]", key);
            return entity;
        }

        CloudbreakUser who = setActingUser(runningParameter);

        Log.then(LOGGER, assertion.getClass().getSimpleName() + " assertion on " + entity + " by " + who);
        try {
            CloudbreakTestDto cloudbreakTestDto = resourceNames.get(key);
            if (cloudbreakTestDto != null) {
                return assertion.doAssertion(this, (T) cloudbreakTestDto, getMicroserviceClient(entity.getClass(), who.getAccessKey()));
            } else {
                assertion.doAssertion(this, entity, getMicroserviceClient(entity.getClass(), who.getAccessKey()));
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("then [{}] assertion is failed: {}, name: {}", key, ResponseUtil.getErrorMessage(e), entity.getName(), e);
                Log.then(null, assertion.getClass().getSimpleName() + " assertion is failed: " + ResponseUtil.getErrorMessage(e));
            }
            getExceptionMap().put(key, e);
        } catch (Error e) {
            getExceptionMap().put(key, new TestFailException(e.getMessage(), e));
        }
        return entity;
    }

    public <E extends Exception, T extends CloudbreakTestDto> T thenException(T entity, Class<? extends MicroserviceClient> clientClass,
            Assertion<T, ? extends MicroserviceClient> assertion, Class<E> expectedException, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKey(assertion.getClass(), runningParameter);

        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped because of previous error. when [{}]", key);
            return entity;
        }

        CloudbreakUser who = setActingUser(runningParameter);

        LOGGER.info("then exception {} action on {} by {}, name: {}", key, entity, who, entity.getName());
        Log.thenException(LOGGER, assertion.getClass().getSimpleName() + " exception assertion on " + entity + " by " + who);
        try {
            String message = String.format("Expected exception with message (%s) has not been thrown!", runningParameter.getExpectedMessage());
            CloudbreakTestDto cloudbreakTestDto = resourceNames.get(key);
            if (cloudbreakTestDto != null) {
                assertion.doAssertion(this, (T) cloudbreakTestDto, getMicroserviceClient(entity.getClass(), who.getAccessKey()));
            } else {
                assertion.doAssertion(this, entity, getMicroserviceClient(entity.getClass(), who.getAccessKey()));
            }
            getExceptionMap().put("thenException", new TestFailException(message));
            LOGGER.error(message);
            htmlLoggerForExceptionValidation(message, "thenException");
        } catch (Exception e) {
            exceptionValidation(expectedException, e, key, runningParameter, "thenException");
        }
        return entity;
    }

    public TestContext as() {
        return as(getActingUser());
    }

    public TestContext as(CloudbreakUser cloudbreakUser) {
        checkShutdown();
        LOGGER.info(" Acting user as: \ndisplay name: {} \naccess key: {} \nsecret key: {} \ncrn: {} \nadmin: {} ", cloudbreakUser.getDisplayName(),
                cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey(), cloudbreakUser.getCrn(), cloudbreakUser.getAdmin());
        Log.as(LOGGER, cloudbreakUser.toString());
        setActingUser(cloudbreakUser);
        if (clients.get(cloudbreakUser.getAccessKey()) == null) {
            CloudbreakClient cloudbreakClient = CloudbreakClient.createCloudbreakClient(getTestParameter(), cloudbreakUser,
                    regionAwareInternalCrnGeneratorFactory.iam());
            FreeIpaClient freeIpaClient = FreeIpaClient.createProxyFreeIpaClient(getTestParameter(), cloudbreakUser,
                    regionAwareInternalCrnGeneratorFactory.iam());
            EnvironmentClient environmentClient = EnvironmentClient.createEnvironmentClient(getTestParameter(), cloudbreakUser,
                    regionAwareInternalCrnGeneratorFactory.iam());
            SdxClient sdxClient = SdxClient.createSdxClient(getTestParameter(), cloudbreakUser);
            UmsClient umsClient = UmsClient.createUmsClient(umsHost, umsPort, regionAwareInternalCrnGeneratorFactory);
            SdxSaasItClient sdxSaasItClient = SdxSaasItClient.createSdxSaasClient(umsHost, regionAwareInternalCrnGeneratorFactory);
            AuthDistributorClient authDistributorClient = AuthDistributorClient.createProxyAuthDistributorClient(
                    regionAwareInternalCrnGeneratorFactory, authDistributorHost);
            RedbeamsClient redbeamsClient = RedbeamsClient.createRedbeamsClient(getTestParameter(), cloudbreakUser);
            PeriscopeClient periscopeClient = PeriscopeClient.createPeriscopeClient(getTestParameter(), cloudbreakUser);
            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Map.of(
                    CloudbreakClient.class, cloudbreakClient,
                    FreeIpaClient.class, freeIpaClient,
                    EnvironmentClient.class, environmentClient,
                    SdxClient.class, sdxClient,
                    RedbeamsClient.class, redbeamsClient,
                    PeriscopeClient.class, periscopeClient,
                    UmsClient.class, umsClient,
                    SdxSaasItClient.class, sdxSaasItClient,
                    AuthDistributorClient.class, authDistributorClient);
            clients.put(cloudbreakUser.getAccessKey(), clientMap);
            cloudbreakClient.setWorkspaceId(0L);
        }
        return this;
    }

    private void initMicroserviceClientsForUMSAccountAdmin(CloudbreakUser accountAdmin) {
        if (clients.get(accountAdmin.getAccessKey()) == null) {
            CloudbreakClient cloudbreakClient = CloudbreakClient.createCloudbreakClient(getTestParameter(), accountAdmin,
                    regionAwareInternalCrnGeneratorFactory.iam());
            FreeIpaClient freeIpaClient = FreeIpaClient.createProxyFreeIpaClient(getTestParameter(), accountAdmin,
                    regionAwareInternalCrnGeneratorFactory.iam());
            EnvironmentClient environmentClient = EnvironmentClient.createEnvironmentClient(getTestParameter(), accountAdmin,
                    regionAwareInternalCrnGeneratorFactory.iam());
            SdxClient sdxClient = SdxClient.createSdxClient(getTestParameter(), accountAdmin);
            UmsClient umsClient = UmsClient.createUmsClient(umsHost, umsPort, regionAwareInternalCrnGeneratorFactory);
            SdxSaasItClient sdxSaasItClient = SdxSaasItClient.createSdxSaasClient(umsHost, regionAwareInternalCrnGeneratorFactory);
            AuthDistributorClient authDistributorClient = AuthDistributorClient.createProxyAuthDistributorClient(
                    regionAwareInternalCrnGeneratorFactory, authDistributorHost);
            RedbeamsClient redbeamsClient = RedbeamsClient.createRedbeamsClient(getTestParameter(), accountAdmin);
            PeriscopeClient periscopeClient = PeriscopeClient.createPeriscopeClient(getTestParameter(), accountAdmin);
            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Map.of(
                    CloudbreakClient.class, cloudbreakClient,
                    FreeIpaClient.class, freeIpaClient,
                    EnvironmentClient.class, environmentClient,
                    SdxClient.class, sdxClient,
                    RedbeamsClient.class, redbeamsClient,
                    PeriscopeClient.class, periscopeClient,
                    UmsClient.class, umsClient,
                    SdxSaasItClient.class, sdxSaasItClient,
                    AuthDistributorClient.class, authDistributorClient);
            clients.put(accountAdmin.getAccessKey(), clientMap);
        }
        LOGGER.info(" Microservice clients have been initialized successfully for UMS account admin:: \nDisplay name: {} \nAccess key: {} \nSecret key: {} " +
                "\nCrn: {} ", accountAdmin.getDisplayName(), accountAdmin.getAccessKey(), accountAdmin.getSecretKey(), accountAdmin.getCrn());
    }

    public TestContext addDescription(TestCaseDescription testCaseDesription) {
        this.contextParameters.put(DESCRIPTION, testCaseDesription);
        return this;
    }

    public Optional<TestCaseDescription> getDescription() {
        TestCaseDescription description = (TestCaseDescription) this.contextParameters.get(DESCRIPTION);
        if (description == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(description);
    }

    public TestContext setTestMethodName(String testMethodName) {
        this.contextParameters.put(TEST_METHOD_NAME, testMethodName);
        return this;
    }

    public Optional<String> getTestMethodName() {
        return Optional.ofNullable(this.contextParameters.get(TEST_METHOD_NAME))
                .map(Object::toString);
    }

    public String getActingUserAccessKey() {
        if (this.actingUser == null) {
            return getTestParameter().get(CloudbreakTest.ACCESS_KEY);
        }
        return actingUser.getAccessKey();
    }

    public Crn getActingUserCrn() {
        return getMockUserCrn()
                .or(this::getRealUMSUserCrn)
                .or(this::getUserParameterCrn)
                .orElseThrow(() -> new TestFailException(String.format("Cannot find acting user: '%s' - Crn", getActingUserAccessKey())));
    }

    /**
     * Returning the default Mock user's Customer Reference Number (CRN).
     * <p>
     * Default Mock user details can be defined at:
     * - application parameter: integrationtest.user.crn
     * - in ~/.dp/config as "localhost" profile
     */
    private Optional<Crn> getMockUserCrn() {
        try {
            return Optional.ofNullable(Crn.fromString(Base64Util.decode(getActingUserAccessKey())));
        } catch (Exception e) {
            LOGGER.info("User CRN was not generated by local CB mock cause its not in Base64 format, falling back to configuration to determine user CRN.");
            return Optional.empty();
        }
    }

    /**
     * Returning the acting (actually used as actor) UMS user's Customer Reference Number (CRN).
     * <p>
     * Default UMS user details are defined at ums-users/api-credentials.json and can be accessed
     * by `useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN)`
     */
    private Optional<Crn> getRealUMSUserCrn() {
        return Crn.isCrn(getActingUser().getCrn())
                ? Optional.ofNullable(Crn.fromString(getActingUser().getCrn()))
                : Optional.empty();
    }

    /**
     * Returning the default Cloudbreak user's Customer Reference Number (CRN).
     * <p>
     * Default Cloudbreak user details can be defined as:
     * - application parameter: integrationtest.user.crn
     * - environment variable: INTEGRATIONTEST_USER_CRN
     */
    private Optional<Crn> getUserParameterCrn() {
        return StringUtils.isNotBlank(getTestParameter().get(CloudbreakTest.USER_CRN))
                ? Optional.ofNullable(Crn.fromString(getTestParameter().get(CloudbreakTest.USER_CRN)))
                : Optional.empty();
    }

    public String getActingUserName() {
        return getMockUserName()
                .or(this::getRealUMSUserName)
                .or(this::getUserParameterName)
                .orElseThrow(() -> new TestFailException(format("Cannot find acting user: '%s' - Name", getActingUserAccessKey())));
    }

    /**
     * Returning the default Mock user's name.
     * <p>
     * Default Mock user details can be defined at:
     * - application parameter: integrationtest.user.crn
     * - in ~/.dp/config as "localhost" profile
     */
    private Optional<String> getMockUserName() {
        try {
            return Optional.of(Objects.requireNonNull(Crn.fromString(Base64Util.decode(getActingUserAccessKey()))).getUserId());
        } catch (Exception e) {
            LOGGER.info("User name was not generated by local CB mock cause its not in base64 format, falling back to configuration to determine user name.");
            return Optional.empty();
        }
    }

    /**
     * Returning the default Mock user's workload username.
     * <p>
     * Default Mock user details can be defined at:
     * - application parameter: integrationtest.user.crn
     * - in ~/.dp/config as "localhost" profile
     */
    private Optional<String> getMockWorkloadUserName() {
        return getMockUserName().isPresent()
                ? Optional.of(SanitizerUtil.sanitizeWorkloadUsername(getMockUserName().get()))
                : Optional.empty();
    }

    /**
     * Returning the acting (actually used as actor) UMS user's name.
     * <p>
     * Default UMS user details are defined at ums-users/api-credentials.json and can be accessed
     * by `useRealUmsUser`
     */
    private Optional<String> getRealUMSUserName() {
        return getRealUMSUserCrn().isPresent()
                ? Optional.of(getActingUser().getDisplayName())
                : Optional.empty();
    }

    /**
     * Returning the acting (actually used as actor) UMS user's workload username.
     * <p>
     * Default UMS user details are defined at ums-users/api-credentials.json and can be accessed
     * by `useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN)`
     */
    private Optional<String> getRealUMSWorkloadUserName() {
        return getRealUMSUserName().isPresent()
                ? Optional.of(getActingUser().getWorkloadUserName())
                : Optional.empty();
    }

    /**
     * Returning the default Cloudbreak user's name.
     * <p>
     * Default Cloudbreak username can be defined as:
     * - application parameter: integrationtest.user.name
     * - environment variable: INTEGRATIONTEST_USER_NAME
     */
    private Optional<String> getUserParameterName() {
        return StringUtils.isNotBlank(getTestParameter().get(CloudbreakTest.USER_NAME))
                ? Optional.of(getTestParameter().get(CloudbreakTest.USER_NAME))
                : Optional.empty();
    }

    /**
     * Returning the default Cloudbreak user's workload username.
     * <p>
     * Default Cloudbreak user workload username can be defined as:
     * - application parameter: integrationtest.user.workloadUserName
     * - environment variable: INTEGRATIONTEST_USER_WORKLOADUSERNAME
     */
    private Optional<String> getUserParameterWorkloadUserName() {
        return StringUtils.isNotBlank(getTestParameter().get(CloudbreakTest.WORKLOAD_USER_NAME))
                ? Optional.of(getTestParameter().get(CloudbreakTest.WORKLOAD_USER_NAME))
                : Optional.empty();
    }

    /**
     * Returning the acting (actually used as actor) user's workload username.
     */
    public String getWorkloadUserName() {
        return getMockWorkloadUserName()
                .or(this::getRealUMSWorkloadUserName)
                .or(this::getUserParameterWorkloadUserName)
                .orElseThrow(() -> new TestFailException(format("Cannot find acting user: '%s' - Workload Username", getActingUserAccessKey())));
    }

    /**
     * Returning the acting (actually used as actor) user's workload password.
     * <p>
     * Default user password can be defined as:
     * - application parameter: integrationtest.user.workloadPassword
     * - environment variable: INTEGRATIONTEST_USER_WORKLOADPASSWORD
     * - MOCK_UMS_PASSWORD
     */
    public String getWorkloadPassword() {
        return getRealUMSWorkloadUserName().isPresent()
                ? workloadPassword
                : getMockUmsPassword();
    }

    /**
     * Updates the acting user with the provided one.
     *
     * @param actingUser Provided acting user (CloudbreakUser)
     */
    public void setActingUser(CloudbreakUser actingUser) {
        LOGGER.info(" Acting user has been set:: \nDisplay Name: {} \nAccess Key: {} \nSecret Key: {} \nCRN: {} \nAdmin: {} \nDescription: {} ",
                actingUser.getDisplayName(), actingUser.getAccessKey(), actingUser.getSecretKey(), actingUser.getCrn(), actingUser.getAdmin(),
                actingUser.getDescription());
        this.actingUser = actingUser;
    }

    /**
     * If requested user is present, sets it as acting user then returns with it, otherwise returns the actual acting user.
     *
     * @param runningParameter Running parameter with acting user. Sample: RunningParameter.who(cloudbreakActor
     *                         .getRealUmsUser(AuthUserKeys.ENV_CREATOR_A))
     * @return Returns with the acting user (CloudbreakUser)
     */
    public CloudbreakUser setActingUser(RunningParameter runningParameter) {
        CloudbreakUser cloudbreakUser = runningParameter.getWho();
        if (cloudbreakUser == null) {
            cloudbreakUser = getActingUser();
            LOGGER.info(" Requested user for acting is NULL. So we are falling back to actual acting user:: \nDisplay Name: {} \nAccess Key: {}" +
                            " \nSecret Key: {} \nCRN: {} \nAdmin: {} \nDescription: {} ", cloudbreakUser.getDisplayName(), cloudbreakUser.getAccessKey(),
                    cloudbreakUser.getSecretKey(), cloudbreakUser.getCrn(), cloudbreakUser.getAdmin(), cloudbreakUser.getDescription());
        } else {
            if (!actingUser.getDisplayName().equalsIgnoreCase(cloudbreakUser.getDisplayName())) {
                setActingUser(cloudbreakUser);
            } else {
                LOGGER.info(" Requested user for acting is the same as actual acting user:: \nDisplay Name: {} \nAccess Key: {} \nSecret Key: {} \nCRN: {}" +
                                " \nAdmin: {} \nDescription: {} ", actingUser.getDisplayName(), actingUser.getAccessKey(), actingUser.getSecretKey(),
                        actingUser.getCrn(), actingUser.getAdmin(), actingUser.getDescription());
            }
        }
        return cloudbreakUser;
    }

    /**
     * If acting user is present, returns the user, otherwise returns the Default user.
     * <p>
     * Default Cloudbreak user details can be defined as:
     * - application parameter: integrationtest.user.accesskey and integrationtest.user.secretkey
     * - environment variable: INTEGRATIONTEST_USER_ACCESSKEY and INTEGRATIONTEST_USER_SECRETKEYOR
     *
     * @return Returns with the acting user (CloudbreakUser)
     */
    public CloudbreakUser getActingUser() {
        if (actingUser == null) {
            LOGGER.info(" Requested acting user is NULL. So we are falling back to Default user with \nACCESS_KEY: {} \nSECRET_KEY: {}",
                    getTestParameter().get(CloudbreakTest.ACCESS_KEY), getTestParameter().get(CloudbreakTest.SECRET_KEY));
            CloudbreakUser defaultRealUmsUser = findRealUmsUserByDisplayName(getTestParameter().get(CloudbreakTest.USER_NAME));
            actingUser = (defaultRealUmsUser != null) ? defaultRealUmsUser : cloudbreakActor.defaultUser();
        } else {
            LOGGER.info(" Found acting user is present in the fetched UMS user store file with details:: \nDisplay Name: {} \nAccess Key: {} \nSecret Key: {}" +
                            " \nCRN: {} \nAdmin: {} \nDescription: {} ", actingUser.getDisplayName(), actingUser.getAccessKey(), actingUser.getSecretKey(),
                    actingUser.getCrn(), actingUser.getAdmin(), actingUser.getDescription());
        }
        return actingUser;
    }

    private CloudbreakUser findRealUmsUserByDisplayName(String userName) {
        CloudbreakUser foundUser;
        if (StringUtils.isNotBlank(userName)) {
            try {
                foundUser = cloudbreakActor.useRealUmsUser(userName);
                useUmsUserCache(true);
                LOGGER.info(" The user is present in the fetched UMS user store file with:" +
                                " \nDisplay Name: {} \nAccess Key: {} \nSecret Key: {} \nCRN: {} \nAdmin: {} \nDescription: {} " +
                                "\nWorkload User Name: {}", foundUser.getDisplayName(), foundUser.getAccessKey(), foundUser.getSecretKey(),
                        foundUser.getCrn(), foundUser.getAdmin(), foundUser.getDescription(), foundUser.getWorkloadUserName());
                return foundUser;
            } catch (TestFailException e) {
                LOGGER.warn("User by '{}' name is not present in the fetched UMS user store file.", userName);
                return null;
            }
        } else {
            LOGGER.warn("Provided user name is null or empty! So we cannot check the user in the fetched UMS user store file.");
            return null;
        }
    }

    /**
     * Request a real UMS user by AuthUserKeys from the fetched ums-users/api-credentials.json
     *
     * @param userKey Key with UMS user's display name. Sample: AuthUserKeys.ACCOUNT_ADMIN
     * @return Returns with the UMS user (CloudbreakUser)
     */
    public CloudbreakUser getRealUmsUserByKey(String userKey) {
        CloudbreakUser requestedRealUmsUser;
        if (actingUser.getDisplayName().equalsIgnoreCase(userKey)) {
            LOGGER.info(" Requested real UMS user is the same as acting user:: \nDisplay Name: {} \nAccess Key: {} \nSecret Key: {} \nCRN: {} \nAdmin: {}" +
                            " \nDescription: {} ", actingUser.getDisplayName(), actingUser.getAccessKey(), actingUser.getSecretKey(), actingUser.getCrn(),
                    actingUser.getAdmin(), actingUser.getDescription());
            requestedRealUmsUser = actingUser;
        } else {
            requestedRealUmsUser = cloudbreakActor.useRealUmsUser(userKey);
            LOGGER.info(" Found real UMS user:: \nDisplay Name: {} \nWorkload username: {} \nAccess Key: {} \nSecret Key: {} \nCRN: {} \nAdmin: {}" +
                            " \nDescription: {} ", requestedRealUmsUser.getDisplayName(), requestedRealUmsUser.getWorkloadUserName(),
                    requestedRealUmsUser.getAccessKey(), requestedRealUmsUser.getSecretKey(), requestedRealUmsUser.getCrn(), requestedRealUmsUser.getAdmin(),
                    requestedRealUmsUser.getDescription());
        }
        return requestedRealUmsUser;
    }

    /**
     * Request the real UMS admin from the fetched ums-users json
     *
     * @return Returns with the UMS admin user (CloudbreakUser)
     */
    public CloudbreakUser getRealUmsAdmin() {
        String accountId = Objects.requireNonNull(Crn.fromString(actingUser.getCrn())).getAccountId();
        CloudbreakUser adminUser = cloudbreakActor.getAdminByAccountId(accountId);
        if (actingUser.getDisplayName().equalsIgnoreCase(adminUser.getDisplayName())) {
            LOGGER.info(" Requested real UMS user is the same as acting user:: \nDisplay Name: {} \nAccess Key: {} \nSecret Key: {} \nCRN: {} \nAdmin: {}" +
                            " \nDescription: {} ", actingUser.getDisplayName(), actingUser.getAccessKey(), actingUser.getSecretKey(), actingUser.getCrn(),
                    actingUser.getAdmin(), actingUser.getDescription());
        } else {
            LOGGER.info(" Found real UMS admin user:: \nDisplay Name: {} \nWorkload username: {} \nAccess Key: {} \nSecret Key: {} \nCRN: {} \nAdmin: {}" +
                            " \nDescription: {} ", adminUser.getDisplayName(), adminUser.getWorkloadUserName(),
                    adminUser.getAccessKey(), adminUser.getSecretKey(), adminUser.getCrn(), adminUser.getAdmin(),
                    adminUser.getDescription());
        }
        return adminUser;
    }

    public String getActingUserOwnerTag() {
        return getActingUserName().split("@")[0].toLowerCase().replaceAll("[^\\w]", "-");
    }

    public String getCreationTimestampTag() {
        return String.valueOf(Instant.now().getEpochSecond());
    }

    public <O extends CloudbreakTestDto> O init(Class<O> clss) {
        return init(clss, getCloudPlatform());
    }

    public <O extends CloudbreakTestDto> O init(Class<O> clss, CloudPlatform cloudPlatform) {
        checkShutdown();
        LOGGER.info("init " + clss.getSimpleName());
        O bean = applicationContext.getBean(clss, getTestContext());
        bean.setCloudPlatform(cloudPlatform);
        String key = bean.getClass().getSimpleName();
        initialized = true;
        try {
            bean.valid();
        } catch (Exception e) {
            LOGGER.error("init of [{}] bean is failed: {}, name: {}", key, ResponseUtil.getErrorMessage(e), bean.getName(), e);
            Log.when(null, key + " initialization is failed: " + ResponseUtil.getErrorMessage(e));
            getExceptionMap().put(key, e);
        }
        return bean;
    }

    public <O extends CloudbreakTestDto> O given(Class<O> clss) {
        return given(clss.getSimpleName(), clss);
    }

    public <O extends CloudbreakTestDto> O given(Class<O> clss, CloudPlatform cloudPlatform) {
        return given(clss.getSimpleName(), clss, cloudPlatform);
    }

    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss) {
        return given(key, clss, getCloudPlatform());
    }

    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss, CloudPlatform cloudPlatform) {
        checkShutdown();
        O cloudbreakEntity = (O) resourceNames.get(key);
        if (cloudbreakEntity == null) {
            cloudbreakEntity = init(clss, cloudPlatform);
            resourceNames.put(key, cloudbreakEntity);
            Log.given(LOGGER, cloudbreakEntity + " created");
        } else {
            Log.given(LOGGER, cloudbreakEntity + " retrieved");
        }
        return cloudbreakEntity;
    }

    public Map<String, String> getStatuses() {
        return statuses;
    }

    public void setStatuses(Map<String, String> statusMap) {
        statuses.putAll(statusMap);
    }

    public Map<String, Exception> getErrors() {
        return exceptionMap;
    }

    public <T extends CloudbreakTestDto> T get(String key) {
        if (!resourceNames.containsKey(key) || resourceNames.get(key) == null) {
            LOGGER.warn("Key: '{}' has been provided but it has no result in the Test Context's Resources map.", key);
        }
        return (T) resourceNames.get(key);
    }

    public <T extends CloudbreakTestDto> T get(Class<T> clss) {
        return get(clss.getSimpleName());
    }

    public <O> O getSelected(String key) {
        return (O) selections.get(key);
    }

    public <O> O getRequiredSelected(String key) {
        if (selections.get(key) == null) {
            throw new RuntimeException(String.format("Testcase was expecting a value to be selected with key: %s. %n"
                    + " Try to use: .select(e -> e.getProperty(), key(%s))", key, key));
        }
        return (O) selections.get(key);
    }

    public <O, T extends CloudbreakTestDto> T select(Class<T> entityClass, Attribute<T, O> attribute, Finder<O> finder, RunningParameter runningParameter) {
        return select(getEntityFromEntityClass(entityClass, runningParameter), attribute, finder, runningParameter);
    }

    public <O, T extends CloudbreakTestDto> T select(T entity, Attribute<T, O> attribute, Finder<O> finder, RunningParameter runningParameter) {
        checkShutdown();
        String key = runningParameter.getKey();
        if (StringUtils.isBlank(key)) {
            key = attribute.getClass().getSimpleName();
        }

        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped because of previous error. select: attr: [{}], finder: [{}]", attribute, finder);
            return entity;
        }
        LOGGER.info("try to select (attribute: [{}], finder: [{}]) with key={}, name: {}", attribute, finder, key, entity.getName());
        try {
            O attr = attribute.get(entity);
            O o = finder.find(attr);
            if (o != null) {
                selections.put(key, o);
                LOGGER.info("Selected object: {}", o);
            } else {
                LOGGER.warn("Cannot find the Object");
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("select (key={}, attribute: [{}], finder: [{}]) is failed: {}, name: {}",
                        key, attribute, finder, ResponseUtil.getErrorMessage(e), entity.getName());
            }
            getExceptionMap().put(key, e);
        }
        return entity;
    }

    public <O, T extends CloudbreakTestDto> T capture(T entity, Attribute<T, O> attribute, RunningParameter runningParameter) {
        checkShutdown();
        String key = runningParameter.getKey();
        if (StringUtils.isBlank(key)) {
            key = entity.getClass().getSimpleName();
        }

        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped because of previous error. capture [{}]", attribute);
            return entity;
        }
        LOGGER.info("try to capture (key={}) [{}], name: {}", key, attribute, entity.getName());
        try {
            O attr = attribute.get(entity);
            String captureKey = key;
            if (StringUtils.isBlank(key)) {
                captureKey = entity.getClass().getSimpleName();
            }
            captures.put(captureKey, new Capture(attr));
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("capture [{}] is failed: {}, name: {}", key, ResponseUtil.getErrorMessage(e), entity.getName());
            }
            getExceptionMap().put(key, e);
        }
        return entity;
    }

    public <O, T extends CloudbreakTestDto> T verify(T entity, Attribute<T, O> attribute, RunningParameter runningParameter) {
        checkShutdown();
        String key = runningParameter.getKey();
        if (StringUtils.isBlank(key)) {
            key = attribute.getClass().getSimpleName();
        }

        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped because of previous error. verify [{}]", attribute);
            return entity;
        }
        LOGGER.info("try to verify (key={}). attribute [{}], name: {}", key, attribute, entity.getName());
        try {
            O attr = attribute.get(entity);
            String captureKey = key;
            if (StringUtils.isBlank(key)) {
                captureKey = entity.getClass().getSimpleName();
            }
            Capture capture = captures.get(captureKey);
            if (capture == null) {
                throw new RuntimeException(String.format("The key [%s] is invalid capture is not verified", captureKey));
            } else {
                capture.verify(attr);
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("verify [key={}] is failed: {}, name: {}", key, ResponseUtil.getErrorMessage(e), entity.getName(), e);
            }
            getExceptionMap().put(key, e);
        }
        return entity;
    }

    public CloudbreakClient getCloudbreakClient(String who) {
        CloudbreakClient cloudbreakClient = (CloudbreakClient) clients.getOrDefault(who, Map.of()).get(CloudbreakClient.class);
        if (cloudbreakClient == null) {
            throw new IllegalStateException("Should create a client for this user: " + who);
        }
        return cloudbreakClient;
    }

    public SdxClient getSdxClient(String who) {
        SdxClient sdxClient = (SdxClient) clients.getOrDefault(who, Map.of()).get(SdxClient.class);
        if (sdxClient == null) {
            throw new IllegalStateException("Should create a client for this user: " + who);
        }
        return sdxClient;
    }

    public <U extends MicroserviceClient> U getAdminMicroserviceClient(Class<? extends CloudbreakTestDto> testDtoClass, String accountId) {
        String accessKey;
        if (realUmsUserCacheReadyToUse()) {
            accessKey = cloudbreakActor.getAdminByAccountId(accountId).getAccessKey();
            if (clients.get(accessKey) == null || clients.get(accessKey).isEmpty()) {
                initMicroserviceClientsForUMSAccountAdmin(cloudbreakActor.getAdminByAccountId(accountId));
            }
        } else {
            accessKey = getActingUserAccessKey();
        }
        U microserviceClient = getMicroserviceClient(testDtoClass, accessKey);
        if (microserviceClient == null) {
            throw new IllegalStateException("Should create an admin client for the acting user.");
        }
        return microserviceClient;
    }

    public <U extends MicroserviceClient> U getMicroserviceClient(Class<? extends CloudbreakTestDto> testDtoClass, String who) {

        if (clients.get(who) == null || clients.get(who).isEmpty()) {
            throw new IllegalStateException("Should create a client for this user: " + who);
        }

        List<MicroserviceClient> microserviceClients = clients.get(who).values()
                .stream()
                .filter(client -> client.supportedTestDtos().contains(testDtoClass.getSimpleName()))
                .collect(Collectors.toList());

        if (microserviceClients.isEmpty()) {
            throw new IllegalStateException("This Dto is not supported by any clients: " + testDtoClass.getSimpleName());
        }

        if (microserviceClients.size() > 1) {
            throw new IllegalStateException("This Dto is supported by more than one clients: " + testDtoClass.getSimpleName() + ", clients" +
                    microserviceClients);
        }

        return (U) microserviceClients.get(0);
    }

    public SdxClient getSdxClient() {
        return getSdxClient(getActingUserAccessKey());
    }

    public <U extends MicroserviceClient> U getMicroserviceClient(Class<U> msClientClass) {
        String who = getActingUserAccessKey();
        U microserviceClient = (U) clients.getOrDefault(who, Map.of()).get(msClientClass);
        if (microserviceClient == null) {
            throw new IllegalStateException("Should create a client for this user: " + who);
        }
        return microserviceClient;
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T await(Class<T> entityClass, Map<String, E> desiredStatuses) {
        return await(entityClass, desiredStatuses, emptyRunningParameter());
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T await(Class<T> entityClass, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        return await(getEntityFromEntityClass(entityClass, runningParameter), desiredStatuses, runningParameter);
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T await(T entity, Map<String, E> desiredStatuses) {
        return await(entity, desiredStatuses, emptyRunningParameter());
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T awaitWithClient(T entity, Map<String, E> desiredStatuses, MicroserviceClient client) {
        return awaitWithClient(entity, desiredStatuses, emptyRunningParameter(), client);
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        MicroserviceClient client = getTestContext().getMicroserviceClient(entity.getClass(), getTestContext().setActingUser(runningParameter)
                .getAccessKey());

        return awaitWithClient(entity, desiredStatuses, runningParameter, client);
    }

    private <T extends CloudbreakTestDto, E extends Enum<E>> T awaitWithClient(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            MicroserviceClient client) {
        checkShutdown();
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        CloudbreakTestDto awaitEntity = get(key);
        if (awaitEntity == null) {
            awaitEntity = entity;
        }
        if (runningParameter.isWaitForFlow()) {
            awaitForFlow(awaitEntity, runningParameter);
        }

        try {
            resourceCrns.put(awaitEntity.getCrn(), awaitEntity);
            LOGGER.info("Resource Crn: '{}' by '{}' has been put to resource Crns map.", awaitEntity.getCrn(), awaitEntity.getName());
        } catch (IllegalStateException | NullPointerException e) {
            LOGGER.info("Resource Crn is not available for: {}", awaitEntity.getName());
        }

        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Cloudbreak await should be skipped because of previous error. await [%s]", desiredStatuses));
        } else {
            resourceAwait.await(awaitEntity, desiredStatuses, getTestContext(), runningParameter,
                    flowUtilSingleStatus.getPollingDurationOrTheDefault(runningParameter), maxRetry, maxRetryCount, client);
        }
        return entity;
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T awaitForInstance(T entity, Map<List<String>, E> desiredStatuses,
            RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Cloudbreak await for instance should be skipped because of previous error. awaitforinstance [%s]",
                    desiredStatuses));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        CloudbreakTestDto awaitEntity = get(key);
        instanceAwait.await(awaitEntity, desiredStatuses, getTestContext(), runningParameter,
                flowUtilSingleStatus.getPollingDurationOrTheDefault(runningParameter), maxRetry);
        return entity;
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T awaitForInstancesToExist(T entity, RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "Cloudbreak await for instances to exist should be skipped because of previous error.");
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        CloudbreakTestDto awaitEntity = get(key);
        instanceAwait.awaitExistence(
                awaitEntity, getTestContext(), runningParameter,
                flowUtilSingleStatus.getPollingDurationOrTheDefault(runningParameter), maxRetry
        );
        return entity;
    }

    public <T extends CloudbreakTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        if (StringUtils.isBlank(key)) {
            key = entity.getClass().getSimpleName();
        }
        CloudbreakTestDto awaitEntity = get(key);
        if (awaitEntity == null) {
            awaitEntity = entity;
        }
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "Cloudbreak await for flow should be skipped because of previous error.");
        } else {
            LOGGER.info(String.format(" Cloudbreak await for flow on resource: %s at account: %s - for entity: %s ", awaitEntity.getCrn(),
                    Objects.requireNonNull(Crn.fromString(awaitEntity.getCrn())).getAccountId(), awaitEntity));
            Log.await(LOGGER, String.format(" Cloudbreak await for flow on resource: %s at account: %s - for entity: %s ", awaitEntity.getCrn(),
                    Objects.requireNonNull(Crn.fromString(awaitEntity.getCrn())).getAccountId(), awaitEntity));


            MicroserviceClient msClient = getAdminMicroserviceClient(awaitEntity.getClass(), Objects.requireNonNull(Crn.fromString(awaitEntity.getCrn()))
                    .getAccountId());

            flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, msClient, getTestContext(), runningParameter);
        }
        entity.setLastKnownFlowId(null);
        return entity;
    }

    public <E extends Exception, T extends CloudbreakTestDto> T expect(T entity, Class<E> expectedException, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKey(entity.getClass(), runningParameter);
        Exception exception = getExceptionMap().get(key);
        if (exception == null) {
            String message = "Expected an exception but cannot find with key: " + key;
            getExceptionMap().put("expect", new RuntimeException(message));
            Log.expect(LOGGER, message);
        } else {
            exceptionValidation(expectedException, exception, key, runningParameter, "expect");
        }
        return entity;
    }

    private <E extends Exception> void exceptionValidation(Class<E> expectedException, Exception actualException, String entityKey,
            RunningParameter runningParameter, String stepKey) {
        if (!actualException.getClass().equals(expectedException)) {
            String message = String.format("Expected exception (%s) does not match with the actual exception (%s).",
                    expectedException, actualException.getClass());
            getExceptionMap().put(stepKey, new TestFailException(message));
            LOGGER.error(message);
            htmlLoggerForExceptionValidation(message, stepKey);
        } else if (!isMessageEquals(actualException, runningParameter) || !isPayloadEquals(actualException, runningParameter)) {
            List<String> messages = new ArrayList<>();
            if (!isMessageEquals(actualException, runningParameter)) {
                messages.add(String.format("Expected exception message (%s) does not match with the actual exception message (%s).",
                        runningParameter.getExpectedMessage(), ResponseUtil.getErrorMessage(actualException)));
            }
            if (!isPayloadEquals(actualException, runningParameter)) {
                messages.add(String.format("Expected exception payload (%s) does not match with the actual exception payload (%s).",
                        runningParameter.getExpectedPayload(), ResponseUtil.getErrorPayload(actualException)));
            }
            String message = String.join("\n", messages);
            getExceptionMap().put(stepKey, new TestFailException(message));
            LOGGER.error(message);
            htmlLoggerForExceptionValidation(message, stepKey);
        } else {
            String message = String.format("Expected exception conditions have met, exception: %s, message: %s",
                    expectedException, runningParameter.getExpectedMessage());
            getExceptionMap().remove(entityKey);
            LOGGER.info(message);
            htmlLoggerForExceptionValidation(message, stepKey);
        }
    }

    private void htmlLoggerForExceptionValidation(String message, String stepKey) {
        if ("expect".equalsIgnoreCase(stepKey)) {
            Log.expect(LOGGER, message);
        } else if ("thenException".equalsIgnoreCase(stepKey)) {
            Log.thenException(LOGGER, message);
        } else {
            Log.whenException(LOGGER, message);
        }
    }

    private boolean isMessageEquals(Exception exception, RunningParameter runningParameter) {
        return StringUtils.isBlank(runningParameter.getExpectedMessage())
                || Pattern.compile(runningParameter.getExpectedMessage()).matcher(ResponseUtil.getErrorMessage(exception)).find();
    }

    private boolean isPayloadEquals(Exception exception, RunningParameter runningParameter) {
        return StringUtils.isBlank(runningParameter.getExpectedPayload())
                || Pattern.compile(runningParameter.getExpectedPayload()).matcher(ResponseUtil.getErrorPayload(exception)).find();
    }

    public void handleExceptionsDuringTest(TestErrorLog testErrorLog) {
        validated = true;
        checkShutdown();
        if (!exceptionMap.isEmpty()) {
            List<Clue> clues = resourceNames.values().stream()
                    .filter(Investigable.class::isInstance)
                    .peek(cloudbreakTestDto -> {
                        try {
                            cloudbreakTestDto.refresh();
                        } catch (Exception e) {
                            LOGGER.warn("Failed to refresh {}, continue with using its last known state.", cloudbreakTestDto, e);
                        }
                    })
                    .map(Investigable.class::cast)
                    .map(Investigable::investigate)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            String errorMessage = errorLogMessageProvider.getMessage(exceptionMap, clues);

            ITestResult testResult = getCurrentTestResult();
            Throwable testFailException = errorLogMessageProvider.getException(exceptionMap);

            if (testFailException != null) {
                testResult.setThrowable(testFailException);
                testResult.setTestName(getTestMethodName().orElse("undefinedMethod"));
                testResult.setStatus(ITestResult.FAILURE);

                String methodName = testResult.getName();
                String testFailureType = testResult.getThrowable().getCause() != null
                        ? testResult.getThrowable().getCause().getClass().getName()
                        : testResult.getThrowable().getClass().getName();

                String testName = String.join("_", commonCloudProperties.getCloudProvider().toLowerCase(), methodName);

                testResult.getTestContext().setAttribute(testName + OUTPUT_FAILURE_TYPE, testFailureType);
                testResult.getTestContext().setAttribute(testName + OUTPUT_FAILURE, testResult.getThrowable());
                LOGGER.info("Failed test result have been pushed to: Test Name: {} | Type Attribute: {} | Failure Attribute: {} | Failure Type: {} | Failure: ",
                        testName, testName + OUTPUT_FAILURE_TYPE, testName + OUTPUT_FAILURE, testFailureType, testResult.getThrowable());
            } else {
                LOGGER.error("Test Context TestFailException is null! So cannot get the correct test fail result!");
            }

            testErrorLog.report(LOGGER, errorMessage);
            exceptionMap.clear();
        }
    }

    protected <T extends CloudbreakTestDto> T getEntityFromEntityClass(Class<T> entityClass, RunningParameter runningParameter) {
        String key = getKey(entityClass, runningParameter);
        T entity = (T) resourceNames.get(key);
        if (entity == null) {
            LOGGER.warn("Cannot found in the resources [{}], run with the default", entityClass.getSimpleName());
            entity = init(entityClass);
        }
        return entity;
    }

    private <T> String getKeyForAwait(T entity, Class<? extends T> entityClass, RunningParameter runningParameter) {
        Optional<Map.Entry<String, CloudbreakTestDto>> foundEntry = resourceNames.entrySet().stream()
                .filter(entry -> entry.getValue() == entity)
                .findFirst();
        if (foundEntry.isPresent()) {
            return foundEntry.get().getKey();
        }
        return getKey(entityClass, runningParameter);
    }

    private <T> String getKey(Class<T> entityClass, RunningParameter runningParameter) {
        String key = runningParameter.getKey();
        if (StringUtils.isBlank(key)) {
            key = entityClass.getSimpleName();
        }
        return key;
    }

    protected void checkShutdown() {
        if (shutdown) {
            throw new IllegalStateException("Cannot access this TestContext anymore because of it is shutted down.");
        }
    }

    public void cleanupTestContext() {
        if (!cleanUp) {
            LOGGER.info("Clean up is skipped due to cleanUp paramater");
            return;
        }
        if (!validated && initialized) {
            throw new IllegalStateException(
                    "Test context should be validated! Maybe you forgot to call .validate() at the end of the test? See other tests as an example.");
        }

        checkShutdown();

        handleExceptionsDuringTest(TestErrorLog.IGNORE);

        if (!cleanUpOnFailure && !getExceptionMap().isEmpty()) {
            LOGGER.info("Cleanup skipped beacuse cleanupOnFail is false");
            return;
        }
        List<CloudbreakTestDto> testDtos = new ArrayList<>(getResourceNames().values());
        List<CloudbreakTestDto> orderedTestDtos = testDtos.stream().sorted(new CompareByOrder()).collect(Collectors.toList());
        for (CloudbreakTestDto testDto : orderedTestDtos) {
            try {
                LOGGER.info("Starting to clean up {} {}", testDto.getClass().getSimpleName(), testDto.getName());
                testDto.cleanUp();
            } catch (Exception e) {
                LOGGER.info("Cleaning up of tests context with {} resource is failing, because of: {}", testDto.getName(), e.getMessage());
            }
        }
        shutdown();
    }

    public boolean shouldCleanUp() {
        return getExceptionMap().isEmpty() ? cleanUp : cleanUpOnFailure;
    }

    public void shutdown() {
        setShutdown(true);
    }

    public CloudProviderProxy getCloudProvider() {
        return cloudProvider;
    }

    public CloudProviderAssertionProxy getCloudProviderAssertion() {
        return cloudProviderAssertion;
    }

    public CloudPlatform getCloudPlatform() {
        return CloudPlatform.valueOf(commonCloudProperties.getCloudProvider());
    }

    public void waitingFor(Duration duration, String interruptedMessage) {
        Duration waitDuration = duration == null ? Duration.ofMinutes(0) : duration;
        String intrMessage = interruptedMessage == null ? "Waiting has been interrupted:" : interruptedMessage;
        try {
            Thread.sleep(waitDuration.toMillis());
            LOGGER.info("Wait '{}' duration has been done.", duration.toString());
        } catch (InterruptedException e) {
            LOGGER.warn(StringUtils.join(intrMessage, e));
        }
    }

    public Tunnel getTunnel() {
        checkNonEmpty("integrationtest.cloudbreak.server", defaultServer);
        if (StringUtils.containsIgnoreCase(defaultServer, "usg-1.cdp.mow-dev")) {
            LOGGER.info(format("Tested environmet is GOV Dev at '%s'. So we are using CCM2 connection to the CDP Control Plane!", defaultServer));
            return Tunnel.CCMV2_JUMPGATE;
        } else {
            return Tunnel.CLUSTER_PROXY;
        }
    }

    public void checkNonEmpty(String name, String value) {
        if (StringUtils.isEmpty(value)) {
            throw new NullPointerException(format("Following variable must be set whether as environment variables or (test) application.yaml: %s",
                    name.replaceAll("\\.", "_").toUpperCase(Locale.ROOT)));
        }
    }

    /**
     * The SafeLogic CryptoComply for Java should be installed on Cloudbreak images.
     * This is validated on VM instances by default, it has been invoked in the E2E tearDown
     * (after the execution of each E2E test method).
     *
     * If the JAVA has been forced (re)installed or SDX, DistroX have been deleted during test execution,
     * the SafeLogic validation should be disabled for the test tearDown.
     */
    public void skipSafeLogicValidation() {
        MDC.put("safeLogicValidation", "false");
    }

    /**
     *
     * @return SafeLogic Validation value (true or false)
     */
    public String getSafeLogicValidation() {
        return MDC.get("safeLogicValidation");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{clients: " + clients + ", entities: " + resourceNames + "}";
    }
}
