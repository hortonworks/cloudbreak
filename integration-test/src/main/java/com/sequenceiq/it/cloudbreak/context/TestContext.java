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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import jakarta.inject.Inject;

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
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.common.api.type.Tunnel;
import com.sequenceiq.flow.api.FlowPublicEndpoint;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.await.Await;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderAssertionProxy;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.config.user.TestUserSelectors;
import com.sequenceiq.it.cloudbreak.config.user.TestUsers;
import com.sequenceiq.it.cloudbreak.dto.AbstractTestDto;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDtoBase;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.finder.Attribute;
import com.sequenceiq.it.cloudbreak.finder.Capture;
import com.sequenceiq.it.cloudbreak.finder.Finder;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.microservice.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.microservice.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.microservice.SdxClient;
import com.sequenceiq.it.cloudbreak.microservice.TestClients;
import com.sequenceiq.it.cloudbreak.util.ErrorLogMessageProvider;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.UmsUtil;
import com.sequenceiq.it.cloudbreak.util.wait.FlowUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.ResourceAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceAwait;

public abstract class TestContext implements ApplicationContextAware {

    public static final String OUTPUT_FAILURE_TYPE = "outputFailureType";

    public static final String OUTPUT_FAILURE = "outputFailure";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

    private static final String DESCRIPTION = "DESCRIPTION";

    private static final String TEST_METHOD_NAME = "TEST_METHOD_NAME";

    private final Map<String, CloudbreakTestDto> resourceNames = new ConcurrentHashMap<>();

    private final Map<String, CloudbreakTestDto> resourceCrns = new ConcurrentHashMap<>();

    private final Map<String, Exception> exceptionMap = new HashMap<>();

    private final Map<Class<? extends CloudbreakTestDto>, String> existingResourceNames = new ConcurrentHashMap<>();

    private final Map<String, String> statuses = new HashMap<>();

    private final Map<String, Object> selections = new HashMap<>();

    private final Map<String, Capture> captures = new HashMap<>();

    private ApplicationContext applicationContext;

    private boolean shutdown;

    private volatile Map<String, Object> contextParameters = new HashMap<>();

    private TestContext testContext;

    @Inject
    private FlowUtil flowUtil;

    @Inject
    private TestClients testClients;

    @Value("${integrationtest.testsuite.cleanUpOnFailure:true}")
    private boolean cleanUpOnFailure;

    @Value("${integrationtest.testsuite.cleanUp:true}")
    private boolean cleanUp;

    @Value("${integrationtest.testsuite.pollingInterval:1000}")
    private long pollingInterval;

    @Value("#{'${integrationtest.cloudProvider}'.equals('MOCK') ? 300 : ${integrationtest.testsuite.maxRetry:2700}}")
    private int maxRetry;

    @Value("#{'${integrationtest.cloudProvider}'.equals('MOCK') ? 3 : ${integrationtest.testsuite.maxRetryCount:5}}")
    private int maxRetryCount;

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
    private TestUsers testUsers;

    private boolean validated;

    private boolean initialized;

    private boolean safeLogicValidation = true;

    @Value("${integrationtest.selinux.validate:false}")
    private boolean validateSelinux;

    public int getMaxRetry() {
        return maxRetry;
    }

    public int getMaxRetryCount() {
        return maxRetryCount;
    }

    public long getPollingInterval() {
        return getPollingDurationOrTheDefault(RunningParameter.emptyRunningParameter()).toMillis();
    }

    public Duration getPollingDurationOrTheDefault(RunningParameter runningParameter) {
        Duration pollingInterval = Optional.of(runningParameter)
                .or(() -> Optional.of(RunningParameter.emptyRunningParameter()))
                .map(RunningParameter::getPollingInterval)
                .orElse(Duration.of(this.pollingInterval, ChronoUnit.MILLIS));
        LOGGER.info("Polling interval is: '{}'", pollingInterval);
        return pollingInterval;
    }

    public TestUsers getTestUsers() {
        checkNonEmpty("integrationtest.cloudbreak.server", defaultServer);
        if ((StringUtils.containsIgnoreCase(defaultServer, "dps.mow")
                || StringUtils.containsIgnoreCase(defaultServer, "cdp.mow")
                || StringUtils.containsIgnoreCase(defaultServer, "cdp-priv.mow"))
                && this instanceof E2ETestContext) {
            testUsers.setSelector(TestUserSelectors.UMS_PREFERED);
        }
        return testUsers;
    }

    public TestClients getTestClients() {
        return testClients;
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

    public Map<Class<? extends CloudbreakTestDto>, String> getExistingResourceNames() {
        return existingResourceNames;
    }

    public Map<String, Map<Class<? extends MicroserviceClient>, MicroserviceClient>> getClients() {
        return getTestClients().getClients();
    }

    public Map<String, Exception> getExceptionMap() {
        return exceptionMap;
    }

    public void setShutdown(boolean shutdown) {
        this.shutdown = shutdown;
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

        LOGGER.info("when {} action on {} bwho =y {}, name: {}", key, entity, who, entity.getName());
        Log.when(LOGGER, action.getClass().getSimpleName() + " action on " + entity + " by " + who);

        try {
            return doAction(entity, clientClass, action, who.getAccessKey());
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("when [{}] action is failed: {}, name: {}", key, ResponseUtil.getErrorMessage(e), entity.getName(), e);
                Log.when(null, action.getClass().getSimpleName() + " action is failed: " + ResponseUtil.getErrorMessage(e));
                getExceptionMap().put(key, e);
            }
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

    public TestContext as(String label) {
        getTestUsers().selectUserByLabel(label);
        getTestClients().createTestClients(getTestUsers().getActingUser());
        return this;
    }

    public TestContext as(CloudbreakUser cloudbreakUser) {
        checkShutdown();
        LOGGER.info(" Acting user as: \ndisplay name: {} \naccess key: {} \nsecret key: {} \ncrn: {} \nadmin: {} ", cloudbreakUser.getDisplayName(),
                cloudbreakUser.getAccessKey(), cloudbreakUser.getSecretKey(), cloudbreakUser.getCrn(), cloudbreakUser.getAdmin());
        Log.as(LOGGER, cloudbreakUser.toString());
        setActingUser(cloudbreakUser);
        getTestClients().createTestClients(cloudbreakUser);
        return this;
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

    public Optional<String> getTestMethodName() {
        return Optional.ofNullable(this.contextParameters.get(TEST_METHOD_NAME))
                .map(Object::toString);
    }

    public TestContext setTestMethodName(String testMethodName) {
        this.contextParameters.put(TEST_METHOD_NAME, testMethodName);
        return this;
    }

    public String getActingUserAccessKey() {
        return getTestUsers().getActingUser().getAccessKey();
    }

    public boolean isMockUms() {
        return UmsUtil.isMockUms(getActingUserAccessKey());
    }

    public Crn getActingUserCrn() {
        if (getTestUsers().getActingUser().getCrn() == null) {
            throw new TestFailException(format("Acting user crn is not available. {}", getActingUser().getDisplayName()));
        }
        return Crn.fromString(getTestUsers().getActingUser().getCrn());
    }

    public String getActingUserName() {
        return getTestUsers().getActingUser().getDisplayName();
    }

    /**
     * Returning the acting (actually used as actor) user's workload username.
     */
    public String getWorkloadUserName() {
        return getTestUsers().getActingUser().getWorkloadUserName();
    }

    /**
     * If requested user is present, sets it as acting user then returns with it, otherwise returns the actual acting user.
     *
     * @param runningParameter Running parameter with acting user. Sample: RunningParameter.who(cloudbreakActor
     *                         .getRealUmsUser(AuthUserKeys.ENV_CREATOR_A))
     * @return Returns with the acting user (CloudbreakUser)
     */
    public CloudbreakUser setActingUser(RunningParameter runningParameter) {
        CloudbreakUser cloudbreakUser = runningParameter.getWho() == null ? getActingUser() : runningParameter.getWho();
        if (cloudbreakUser == null) {
            cloudbreakUser = getActingUser();
            LOGGER.info(" Requested user for acting is NULL. So we are falling back to actual acting user:: \nDisplay Name: {} \nAccess Key: {}" +
                            " \nSecret Key: {} \nCRN: {} \nAdmin: {} \nDescription: {} ", cloudbreakUser.getDisplayName(), cloudbreakUser.getAccessKey(),
                    cloudbreakUser.getSecretKey(), cloudbreakUser.getCrn(), cloudbreakUser.getAdmin(), cloudbreakUser.getDescription());
        } else {
            if (!getActingUser().getDisplayName().equalsIgnoreCase(cloudbreakUser.getDisplayName())) {
                setActingUser(cloudbreakUser);
            } else {
                LOGGER.info(" Requested user for acting is the same as actual acting user:: \nDisplay Name: {} \nAccess Key: {} \nSecret Key: {} \nCRN: {}" +
                                " \nAdmin: {} \nDescription: {} ", getActingUser().getDisplayName(), getActingUser().getAccessKey(),
                        getActingUser().getSecretKey(), getActingUser().getCrn(),
                        getActingUser().getAdmin(), getActingUser().getDescription());
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
        return getTestUsers().getActingUser();
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
        getTestUsers().setActingUser(actingUser);
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

    public <T extends CloudbreakTestDto> T get(String key, Class<T> clss) {
        return get(key);
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

    public <T extends CloudbreakTestDto> T getInstanceOf(Class<T> clss) {
        return resourceNames.values().stream()
                .filter(clss::isInstance)
                .map(clss::cast)
                .filter(e -> e.getName() != null)
                .findFirst()
                .orElse(null);
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

    public SdxClient getSdxClient(String who) {
        return getTestClients().getSdxClient(who);
    }

    public CloudbreakClient getCloudbreakClient(String who) {
        return getTestClients().getCloudbreakClient(who);
    }

    public <U extends MicroserviceClient> U getAdminMicroserviceClient(Class<? extends CloudbreakTestDto> testDtoClass, String accountId) {
        CloudbreakUser testUser = getTestUsers().getAdminInAccount(accountId);
        String accessKey = testUser.getAccessKey();
        getTestClients().createTestClients(testUser);
        U microserviceClient = getMicroserviceClient(testDtoClass, accessKey);
        if (microserviceClient == null) {
            throw new IllegalStateException("Should create an admin client for the acting user.");
        }
        return microserviceClient;
    }

    public <U extends MicroserviceClient> U getMicroserviceClient(Class<? extends CloudbreakTestDto> testDtoClass, String who) {
        return getTestClients().getMicroserviceClient(testDtoClass, who);
    }

    public SdxClient getSdxClient() {
        return getSdxClient(getActingUserAccessKey());
    }

    public CloudbreakClient getCloudbreakClient() {
        return getCloudbreakClient(getActingUserAccessKey());
    }

    public <U extends MicroserviceClient> U getMicroserviceClient(Class<U> msClientClass) {
        return getTestClients().getMicroserviceClientByType(msClientClass, getActingUserAccessKey());
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
        return awaitWithClient(entity, desiredStatuses, emptyRunningParameter(), client, null);
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        MicroserviceClient client = getTestContext().getMicroserviceClient(entity.getClass(), getTestContext().setActingUser(runningParameter)
                .getAccessKey());

        return awaitWithClient(entity, desiredStatuses, runningParameter, client, null);
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>, U extends MicroserviceClient> T await(T entity, Map<String, E> desiredStatuses,
            RunningParameter runningParameter, Await<T, U> customAwait) {
        U client = getTestContext().getMicroserviceClient(entity.getClass(), getTestContext().setActingUser(runningParameter)
                .getAccessKey());
        return awaitWithClient(entity, desiredStatuses, runningParameter, client, customAwait);
    }

    private <T extends CloudbreakTestDto, E extends Enum<E>, U extends MicroserviceClient> T awaitWithClient(T entity, Map<String, E> desiredStatuses,
            RunningParameter runningParameter, U client, Await<T, U> customAwait) {
        checkShutdown();
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        T awaitEntity = get(key);
        if (awaitEntity == null) {
            awaitEntity = entity;
        }
        if (customAwait != null) {
            customAwait.await(getTestContext(), awaitEntity, client, runningParameter);
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
                    getPollingDurationOrTheDefault(runningParameter), maxRetry, maxRetryCount, client);
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
        instanceAwait.await(awaitEntity, desiredStatuses, getTestContext(), runningParameter);
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
        instanceAwait.awaitExistence(awaitEntity, getTestContext(), runningParameter);
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
            LOGGER.info(String.format("Cloudbreak await for flow on resource: %s at account: %s - for entity: %s ", awaitEntity.getCrn(),
                    Objects.requireNonNull(Crn.fromString(awaitEntity.getCrn())).getAccountId(), awaitEntity));
            Log.await(LOGGER, String.format("Cloudbreak await for flow on resource: %s at account: %s - for entity: %s ", awaitEntity.getCrn(),
                    Objects.requireNonNull(Crn.fromString(awaitEntity.getCrn())).getAccountId(), awaitEntity));
            MicroserviceClient msClient = getAdminMicroserviceClient(awaitEntity.getClass(), Objects.requireNonNull(Crn.fromString(awaitEntity.getCrn()))
                    .getAccountId());
            FlowPublicEndpoint flowPublicEndpoint = msClient.flowPublicEndpoint();
            if (flowPublicEndpoint != null) {
                flowUtil.waitForLastKnownFlow(awaitEntity, flowPublicEndpoint, getTestContext(), runningParameter);
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
            LOGGER.error(message, actualException);
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
            LOGGER.error(message, actualException);
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
                    .map(cloudbreakTestDto -> {
                        try {
                            LOGGER.info("Refreshing {} before collecting its clues", cloudbreakTestDto.getName());
                            return cloudbreakTestDto.refresh();
                        } catch (Exception e) {
                            LOGGER.warn("Failed to refresh {}, continue with using its last known state.", cloudbreakTestDto.getName(), e);
                            return cloudbreakTestDto;
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

    public synchronized void cleanupTestContext() {
        if (shutdown) {
            LOGGER.info("Cleanup already performed for this TestContext.");
            return;
        }
        if (!cleanUp) {
            LOGGER.info("Clean up is skipped due to cleanUp paramater");
            return;
        }
        if (!validated && initialized) {
            throw new IllegalStateException(
                    "Test context should be validated! Maybe you forgot to call .validate() at the end of the test? See other tests as an example.");
        }

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
                if (shouldRemoveMockInfrastructureTestCalls(testDto)) {
                    ((AbstractTestDto) testDto).resetCalls();
                }
            } catch (Exception e) {
                LOGGER.info("Cleaning up of tests context with {} resource is failing, because of: {}", testDto.getName(), e.getMessage());
            }
        }
        shutdown();
    }

    private boolean shouldRemoveMockInfrastructureTestCalls(CloudbreakTestDto testDto) {
        Set<String> testCallRemovableResources = Set.of(
                EnvironmentTestDto.ENVIRONMENT_RESOURCE_NAME,
                FreeIpaTestDto.FREEIPA_RESOURCE_NAME,
                SdxInternalTestDto.SDX_RESOURCE_NAME,
                DistroXTestDtoBase.DISTROX_RESOURCE_NAME);
        return CloudPlatform.MOCK.equals(testDto.getCloudPlatform()) && testDto.getResourceNameType() != null &&
                testCallRemovableResources.contains(testDto.getResourceNameType()) && testDto instanceof AbstractTestDto;
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

    public boolean isResourceEncryptionEnabled() {
        if (getCloudProvider().getGovCloud()) {
            LOGGER.info("Resource encryption is enabled, because the tested is environment is a GOV environment.");
            return true;
        }
        return false;
    }

    /**
     * The SafeLogic CryptoComply for Java should be installed on Cloudbreak images.
     * This is validated on VM instances by default, it has been invoked in the E2E tearDown
     * (after the execution of each E2E test method).
     * <p>
     * If the JAVA has been forced (re)installed or SDX, DistroX have been deleted during test execution,
     * the SafeLogic validation should be disabled for the test tearDown.
     */
    public void skipSafeLogicValidation() {
        safeLogicValidation = false;
    }

    /**
     * @return SafeLogic Validation value (true or false)
     */
    public boolean getSafeLogicValidation() {
        return safeLogicValidation;
    }

    public void setValidateSelinux(boolean validateSelinux) {
        this.validateSelinux = validateSelinux;
    }

    public boolean getSELinuxValidation() {
        return validateSelinux;
    }

    public boolean isSecretEncryptionEnabled() {
        EnvironmentTestDto environment = get(EnvironmentTestDto.class);
        if (environment == null || environment.getResponse() == null) {
            LOGGER.warn("Skipping secret encryption validation because the environment or its response is null.");
            return false;
        }
        return environment.getResponse().isEnableSecretEncryption();
    }

    public String getWorkloadPassword() {
        return getActingUser().getWorkloadPassword();
    }

    public boolean isMowTest() {
        return defaultServer != null && defaultServer.contains("mow");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{entities: " + resourceNames + "}";
    }
}
