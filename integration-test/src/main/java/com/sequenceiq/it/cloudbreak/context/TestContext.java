package com.sequenceiq.it.cloudbreak.context;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.sequenceiq.cloudbreak.auth.crn.Crn;
import com.sequenceiq.cloudbreak.auth.crn.RegionAwareInternalCrnGeneratorFactory;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.UmsClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakActor;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.finder.Attribute;
import com.sequenceiq.it.cloudbreak.finder.Capture;
import com.sequenceiq.it.cloudbreak.finder.Finder;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.util.ErrorLogMessageProvider;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.FlowUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.ResourceAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceAwait;

import io.opentracing.Tracer;

public abstract class TestContext implements ApplicationContextAware {

    public static final String OUTPUT_FAILURE_TYPE = "outputFailureType";

    public static final String OUTPUT_FAILURE = "outputFailure";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

    private static final String DESCRIPTION = "DESCRIPTION";

    private static final String TEST_METHOD_NAME = "TEST_METHOD_NAME";

    private ApplicationContext applicationContext;

    private final Map<String, CloudbreakTestDto> resourceNames = new LinkedHashMap<>();

    private final Map<String, CloudbreakTestDto> resourceCrns = new LinkedHashMap<>();

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

    @Value("#{'${integrationtest.cloudProvider}'.equals('MOCK') ? 3 : ${integrationtest.testsuite.maxRetryCount:3}}")
    private int maxRetryCount;

    @Value("${integrationtest.ums.host:localhost}")
    private String umsHost;

    @Inject
    private CloudProviderProxy cloudProvider;

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
    private Tracer tracer;

    @Inject
    private CloudbreakActor cloudbreakActor;

    @Inject
    private RegionAwareInternalCrnGeneratorFactory regionAwareInternalCrnGeneratorFactory;

    private boolean validated;

    private boolean initialized;

    private CloudbreakUser actingUser;

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
     *  - The initialization of the real UMS user store is happening automatically.
     *  - If the 'api-credentials.json' is mistakenly present at 'ums-users' folder.
     *    When a microservice client or an action is intended to use admin user then
     *    a real UMS admin is going to be provided from the initialized user store.
     * By setting 'useUmsUserCache' to 'true' we can define the usage of real UMS user
     * store. Then and only then tests are running with 'useRealUmsUser' the real UMS
     * users are going to be provided from the initialized user store.
     *
     * So we can rest assured MOCK or E2E Cloudbreak tests are going to be run with
     * mock and default test users even the 'ums-users/api-credentials.json' is present
     * and real UMS user store is initialized.
     *
     * @param useUmsUserCache   'true' if user store has been selected for providing
     *                          users for tests
     */
    public void useUmsUserCache(boolean useUmsUserCache) {
        this.useUmsUserCache = useUmsUserCache;
    }

    /**
     * Returning 'true' if tests are running with real UMS users.
     *
     * @return                  'true' if real UMS users are used for tests.
     */
    public boolean umsUserCacheInUse() {
        return useUmsUserCache;
    }

    /**
     * Returning 'true' if real UMS users can be used for testing:
     *  - user store has been initialized successfully
     *  - user store has been selected for providing users by 'useUmsUserCache=true'
     *
     * @return                  'true' if real UMS user store has been initialized and
     *                          selected for use.
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
            CloudbreakClient cloudbreakClient = CloudbreakClient.createProxyCloudbreakClient(testParameter, cloudbreakUser,
                    regionAwareInternalCrnGeneratorFactory.iam());
            FreeIpaClient freeIpaClient = FreeIpaClient.createProxyFreeIpaClient(testParameter, cloudbreakUser,
                    regionAwareInternalCrnGeneratorFactory.iam());
            EnvironmentClient environmentClient = EnvironmentClient.createProxyEnvironmentClient(testParameter, cloudbreakUser,
                    regionAwareInternalCrnGeneratorFactory.iam());
            SdxClient sdxClient = SdxClient.createProxySdxClient(testParameter, cloudbreakUser);
            UmsClient umsClient = UmsClient.createProxyUmsClient(tracer, umsHost);
            RedbeamsClient redbeamsClient = RedbeamsClient.createProxyRedbeamsClient(testParameter, cloudbreakUser);
            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Map.of(CloudbreakClient.class, cloudbreakClient,
                    FreeIpaClient.class, freeIpaClient, EnvironmentClient.class, environmentClient, SdxClient.class, sdxClient,
                    RedbeamsClient.class, redbeamsClient,
                    UmsClient.class, umsClient);
            clients.put(cloudbreakUser.getAccessKey(), clientMap);
            cloudbreakClient.setWorkspaceId(0L);
        }
        return this;
    }

    private void initMicroserviceClientsForUMSAccountAdmin(CloudbreakUser accountAdmin) {
        if (clients.get(accountAdmin.getAccessKey()) == null) {
            CloudbreakClient cloudbreakClient = CloudbreakClient.createProxyCloudbreakClient(testParameter, accountAdmin,
                    regionAwareInternalCrnGeneratorFactory.iam());
            FreeIpaClient freeIpaClient = FreeIpaClient.createProxyFreeIpaClient(testParameter, accountAdmin,
                    regionAwareInternalCrnGeneratorFactory.iam());
            EnvironmentClient environmentClient = EnvironmentClient.createProxyEnvironmentClient(testParameter, accountAdmin,
                    regionAwareInternalCrnGeneratorFactory.iam());
            SdxClient sdxClient = SdxClient.createProxySdxClient(testParameter, accountAdmin);
            UmsClient umsClient = UmsClient.createProxyUmsClient(tracer, umsHost);
            RedbeamsClient redbeamsClient = RedbeamsClient.createProxyRedbeamsClient(testParameter, accountAdmin);
            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Map.of(CloudbreakClient.class, cloudbreakClient,
                    FreeIpaClient.class, freeIpaClient, EnvironmentClient.class, environmentClient, SdxClient.class, sdxClient,
                    RedbeamsClient.class, redbeamsClient,
                    UmsClient.class, umsClient);
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
            return testParameter.get(CloudbreakTest.ACCESS_KEY);
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
     *
     * Default Mock user details can be defined at:
     * - application parameter: integrationtest.user.crn
     * - in ~/.dp/config as "localhost" profile
     */
    private Optional<Crn> getMockUserCrn() {
        try {
            return Optional.ofNullable(Crn.fromString(new String(Base64.getDecoder().decode(getActingUserAccessKey()))));
        } catch (Exception e) {
            LOGGER.info("User CRN was not generated by local CB mock cause its not in Base64 format, falling back to configuration to determine user CRN.");
            return Optional.empty();
        }
    }

    /**
     * Returning the acting (actually used as actor) UMS user's Customer Reference Number (CRN).
     *
     * Default UMS user details are defined at ums-users/api-credentials.json and can be accessed
     * by `useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN)`
     */
    private Optional<Crn> getRealUMSUserCrn() {
        if (Crn.isCrn(getActingUser().getCrn())) {
            return Optional.ofNullable(Crn.fromString(getActingUser().getCrn()));
        }
        return Optional.empty();
    }

    /**
     * Returning the default Cloudbreak user's Customer Reference Number (CRN).
     *
     * Default Cloudbreak user details can be defined as:
     * - application parameter: integrationtest.user.crn
     * - environment variable: INTEGRATIONTEST_USER_CRN
     */
    private Optional<Crn> getUserParameterCrn() {
        if (StringUtils.isNotBlank(testParameter.get(CloudbreakTest.USER_CRN))) {
            return Optional.ofNullable(Crn.fromString(testParameter.get(CloudbreakTest.USER_CRN)));
        }
        return Optional.empty();
    }

    public String getActingUserName() {
        return getMockUserName()
                .or(this::getRealUMSUserName)
                .or(this::getUserParameterName)
                .orElseThrow(() -> new TestFailException(String.format("Cannot find acting user: '%s' - Name", getActingUserAccessKey())));
    }

    /**
     * Returning the default Mock user's name.
     *
     * Default Mock user details can be defined at:
     * - application parameter: integrationtest.user.crn
     * - in ~/.dp/config as "localhost" profile
     */
    private Optional<String> getMockUserName() {
        try {
            return Optional.of(Objects.requireNonNull(Crn.fromString(new String(Base64.getDecoder().decode(getActingUserAccessKey())))).getUserId());
        } catch (Exception e) {
            LOGGER.info("User name was not generated by local CB mock cause its not in base64 format, falling back to configuration to determine user name.");
            return Optional.empty();
        }
    }

    /**
     * Returning the acting (actually used as actor) UMS user's name.
     *
     * Default UMS user details are defined at ums-users/api-credentials.json and can be accessed
     * by `useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN)`
     */
    private Optional<String> getRealUMSUserName() {
        if (getRealUMSUserCrn().isPresent()) {
            return Optional.of(getActingUser().getDisplayName());
        }
        return Optional.empty();
    }

    /**
     * Returning the default Cloudbreak user's name.
     *
     * Default Cloudbreak user details can be defined as:
     * - application parameter: integrationtest.user.name
     * - environment variable: INTEGRATIONTEST_USER_NAME
     */
    private Optional<String> getUserParameterName() {
        if (StringUtils.isNotBlank(testParameter.get(CloudbreakTest.USER_NAME))) {
            return Optional.of(testParameter.get(CloudbreakTest.USER_NAME));
        }
        return Optional.empty();
    }

    /**
     * Updates the acting user with the provided one.
     *
     * @param actingUser         Provided acting user (CloudbreakUser)
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
     * @param runningParameter   Running parameter with acting user. Sample: RunningParameter.who(cloudbreakActor
     *                           .getRealUmsUser(AuthUserKeys.ENV_CREATOR_A))
     * @return                   Returns with the acting user (CloudbreakUser)
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
     *
     * Default Cloudbreak user details can be defined as:
     * - application parameter: integrationtest.user.accesskey and integrationtest.user.secretkey
     * - environment variable: INTEGRATIONTEST_USER_ACCESSKEY and INTEGRATIONTEST_USER_SECRETKEYOR
     *
     * @return                   Returns with the acting user (CloudbreakUser)
     */
    public CloudbreakUser getActingUser() {
        if (actingUser == null) {
            LOGGER.info(" Requested acting user is NULL. So we are falling back to Default user with \nACCESS_KEY: {} \nSECRET_KEY: {}",
                    testParameter.get(CloudbreakTest.ACCESS_KEY), testParameter.get(CloudbreakTest.SECRET_KEY));
            setActingUser(cloudbreakActor.defaultUser());
        } else {
            LOGGER.info(" Found acting user is present with details:: \nDisplay Name: {} \nAccess Key: {} \nSecret Key: {} \nCRN: {} \nAdmin: {}" +
                            " \nDescription: {} ", actingUser.getDisplayName(), actingUser.getAccessKey(), actingUser.getSecretKey(), actingUser.getCrn(),
                    actingUser.getAdmin(), actingUser.getDescription());
        }
        return actingUser;
    }

    /**
     * Request a real UMS user by AuthUserKeys from the fetched ums-users/api-credentials.json
     *
     * @param userKey            Key with UMS user's display name. Sample: AuthUserKeys.ACCOUNT_ADMIN
     * @return                   Returns with the UMS user (CloudbreakUser)
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
     * @return                   Returns with the UMS admin user (CloudbreakUser)
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
        return String.valueOf(new Date().getTime());
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
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Cloudbreak await should be skipped because of previous error. await [%s]", desiredStatuses));
            return entity;
        }
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

        resourceAwait.await(awaitEntity, desiredStatuses, getTestContext(), runningParameter,
                flowUtilSingleStatus.getPollingDurationOrTheDefault(runningParameter), maxRetry, maxRetryCount, client);
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
            try {
                MicroserviceClient msClient = getAdminMicroserviceClient(awaitEntity.getClass(), Objects.requireNonNull(Crn.fromString(awaitEntity.getCrn()))
                        .getAccountId());
                flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, msClient, runningParameter);
            } catch (Exception e) {
                if (runningParameter.isLogError()) {
                    LOGGER.error("Cloudbreak await for flow '{}' is failed for: '{}', because of {}", awaitEntity, awaitEntity.getName(), e.getMessage(), e);
                    Log.await(LOGGER, String.format(" Cloudbreak await for flow '%s' is failed for '%s', because of %s",
                            awaitEntity, awaitEntity.getName(), e.getMessage()));
                }
                getExceptionMap().put(String.format("Cloudbreak await for flow %s", awaitEntity), e);
            }
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

            ITestResult testResult = Reporter.getCurrentTestResult();
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

    public void shutdown() {
        setShutdown(true);
    }

    public CloudProviderProxy getCloudProvider() {
        return cloudProvider;
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{clients: " + clients + ", entities: " + resourceNames + "}";
    }
}
