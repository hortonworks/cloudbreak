package com.sequenceiq.it.cloudbreak.context;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.auth.altus.CrnResourceDescriptor;
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
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUserCache;
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

    private static final String INTERNAL_ACTOR_ACCESS_KEY = Base64.getEncoder().encodeToString(INTERNAL_ACTOR_CRN.getBytes());

    private ApplicationContext applicationContext;

    private final Map<String, CloudbreakTestDto> resources = new LinkedHashMap<>();

    private final Map<String, Map<Class<? extends MicroserviceClient>, MicroserviceClient>> clients = new HashMap<>();

    private final Map<String, Exception> exceptionMap = new HashMap<>();

    private boolean shutdown;

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

    @Value("${integrationtest.testsuite.pollingInterval:1000}")
    private long pollingInterval;

    @Value("#{'${integrationtest.cloudProvider}'.equals('MOCK') ? 300 : ${integrationtest.testsuite.maxRetry:2700}}")
    private int maxRetry;

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

    private boolean validated;

    private boolean initialized;

    private CloudbreakUser actingUser;

    public Duration getPollingDurationInMills() {
        return Duration.of(pollingInterval, ChronoUnit.MILLIS);
    }

    public Map<String, CloudbreakTestDto> getResources() {
        return resources;
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
        if (StringUtils.isEmpty(key)) {
            key = action.getClass().getSimpleName();
        }

        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped because of previous error. when [{}]", key);
            return entity;
        }

        CloudbreakUser who = getWho(runningParameter);

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

        CloudbreakUser who = getWho(runningParameter);

        Log.then(LOGGER, assertion.getClass().getSimpleName() + " assertion on " + entity + " by " + who);
        try {
            CloudbreakTestDto cloudbreakTestDto = resources.get(key);
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
        }
        return entity;
    }

    public TestContext as() {
        return as(Actor::defaultUser);
    }

    public TestContext as(Actor actor) {
        checkShutdown();
        CloudbreakUser acting = actor.acting(testParameter);
        Log.as(LOGGER, acting.toString());
        setActingUser(acting);
        if (clients.get(acting.getAccessKey()) == null) {
            CloudbreakClient cloudbreakClient = CloudbreakClient.createProxyCloudbreakClient(testParameter, acting);
            FreeIpaClient freeIpaClient = FreeIpaClient.createProxyFreeIpaClient(testParameter, acting);
            EnvironmentClient environmentClient = EnvironmentClient.createProxyEnvironmentClient(testParameter, acting);
            SdxClient sdxClient = SdxClient.createProxySdxClient(testParameter, acting);
            UmsClient umsClient = UmsClient.createProxyUmsClient(tracer);
            RedbeamsClient redbeamsClient = RedbeamsClient.createProxyRedbeamsClient(testParameter, acting);
            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Map.of(CloudbreakClient.class, cloudbreakClient,
                    FreeIpaClient.class, freeIpaClient, EnvironmentClient.class, environmentClient, SdxClient.class, sdxClient,
                    RedbeamsClient.class, redbeamsClient,
                    UmsClient.class, umsClient);
            clients.put(acting.getAccessKey(), clientMap);
            cloudbreakClient.setWorkspaceId(0L);
            redbeamsClient.setEnvironmentCrn(Crn.builder(CrnResourceDescriptor.ENVIRONMENT)
                    .setAccountId("it")
                    .setResource("test-environment")
                    .build().toString());
        }
        return this;
    }

    private CloudbreakUser createInternalActorForAccountIfNotExists(String tenantName) {
        CloudbreakUser internalUser = Actor.create(tenantName, "__internal__actor__").acting(testParameter);
        if (clients.get(internalUser.getAccessKey()) == null) {
            CloudbreakClient cloudbreakClient = CloudbreakClient.createProxyCloudbreakClient(testParameter, internalUser);
            FreeIpaClient freeIpaClient = FreeIpaClient.createProxyFreeIpaClient(testParameter, internalUser);
            EnvironmentClient environmentClient = EnvironmentClient.createProxyEnvironmentClient(testParameter, internalUser);
            SdxClient sdxClient = SdxClient.createProxySdxClient(testParameter, internalUser);
            UmsClient umsClient = UmsClient.createProxyUmsClient(tracer);
            RedbeamsClient redbeamsClient = RedbeamsClient.createProxyRedbeamsClient(testParameter, internalUser);
            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Map.of(CloudbreakClient.class, cloudbreakClient,
                    FreeIpaClient.class, freeIpaClient, EnvironmentClient.class, environmentClient, SdxClient.class, sdxClient,
                    RedbeamsClient.class, redbeamsClient,
                    UmsClient.class, umsClient);
            clients.put(internalUser.getAccessKey(), clientMap);
        }
        return internalUser;
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

    protected String getActingUserAccessKey() {
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
     * Default application parameter:
     * integrationtest.user.crn or "localhost" in ~/.dp/config
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
     * Real UMS user:
     * useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
     */
    private Optional<Crn> getRealUMSUserCrn() {
        if (Crn.isCrn(getActingUser().getCrn())) {
            return Optional.ofNullable(Crn.fromString(getActingUser().getCrn()));
        }
        return Optional.empty();
    }

    /**
     * Application parameter:
     * integrationtest.user.crn
     */
    private Optional<Crn> getUserParameterCrn() {
        if (!testParameter.get(CloudbreakTest.USER_CRN).isEmpty() || testParameter.get(CloudbreakTest.USER_CRN) != null) {
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
     * Default application parameter:
     * integrationtest.user.crn or "localhost" in ~/.dp/config
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
     * Real UMS user:
     * useRealUmsUser(testContext, AuthUserKeys.ACCOUNT_ADMIN);
     */
    private Optional<String> getRealUMSUserName() {
        if (Crn.isCrn(getActingUser().getCrn())) {
            return Optional.of(Objects.requireNonNull(Crn.fromString(getActingUser().getCrn())).getUserId());
        }
        return Optional.empty();
    }

    /**
     * Application parameter:
     * integrationtest.user.name
     */
    private Optional<String> getUserParameterName() {
        if (!testParameter.get(CloudbreakTest.USER_NAME).isEmpty() || testParameter.get(CloudbreakTest.USER_NAME) != null) {
            return Optional.of(testParameter.get(CloudbreakTest.USER_NAME));
        }
        return Optional.empty();
    }

    protected void setActingUser(CloudbreakUser actingUser) {
        this.actingUser = actingUser;
    }

    protected CloudbreakUser getActingUser() {
        return actingUser;
    }

    public <O extends CloudbreakTestDto> O init(Class<O> clss) {
        return init(clss, CloudPlatform.valueOf(commonCloudProperties.getCloudProvider()));
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
        return given(key, clss, CloudPlatform.valueOf(commonCloudProperties.getCloudProvider()));
    }

    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss, CloudPlatform cloudPlatform) {
        checkShutdown();
        O cloudbreakEntity = (O) resources.get(key);
        if (cloudbreakEntity == null) {
            cloudbreakEntity = init(clss, cloudPlatform);
            resources.put(key, cloudbreakEntity);
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
        if (!resources.containsKey(key) || resources.get(key) == null) {
            LOGGER.warn("Key: '{}' has been provided but it has no result in the Test Context's Resources map.", key);
        }
        return (T) resources.get(key);
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
        if (StringUtils.isEmpty(key)) {
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
        if (StringUtils.isEmpty(key)) {
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
            if (StringUtils.isEmpty(key)) {
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
        if (StringUtils.isEmpty(key)) {
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
            if (StringUtils.isEmpty(key)) {
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
        if (CloudbreakUserCache.getInstance().isInitialized()) {
            accessKey = CloudbreakUserCache.getInstance().getAdminAccessKeyByAccountId(accountId);
        } else {
            CloudbreakUser internalActorForAccount = createInternalActorForAccountIfNotExists(accountId);
            accessKey = internalActorForAccount.getAccessKey();
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
        return await(entityClass, desiredStatuses, emptyRunningParameter(), getPollingDurationInMills());
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T await(Class<T> entityClass, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        return await(getEntityFromEntityClass(entityClass, runningParameter), desiredStatuses, runningParameter, getPollingDurationInMills());
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T await(Class<T> entityClass, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        return await(getEntityFromEntityClass(entityClass, runningParameter), desiredStatuses, runningParameter, pollingInterval);
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T await(T entity, Map<String, E> desiredStatuses) {
        return await(entity, desiredStatuses, emptyRunningParameter(), getPollingDurationInMills());
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        return await(entity, desiredStatuses, runningParameter, getPollingDurationInMills());
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
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
            awaitForFlow(awaitEntity, emptyRunningParameter());
        }
        resourceAwait.await(awaitEntity, desiredStatuses, getTestContext(), runningParameter, pollingInterval, maxRetry);
        return entity;
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T awaitForInstance(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        return awaitForInstance(entity, desiredStatuses, runningParameter, getPollingDurationInMills());
    }

    public <T extends CloudbreakTestDto, E extends Enum<E>> T awaitForInstance(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Cloudbreak await should be skipped because of previous error. await [%s]", desiredStatuses));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        CloudbreakTestDto awaitEntity = get(key);
        instanceAwait.await(awaitEntity, desiredStatuses, getTestContext(), runningParameter, pollingInterval, maxRetry);
        return entity;
    }

    public <T extends CloudbreakTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "Cloudbreak await for flow should be skipped because of previous error.");
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        CloudbreakTestDto awaitEntity = get(key);
        if (awaitEntity == null && runningParameter.getKey() == null) {
            throw new RuntimeException("Cloudbreak key provided but no result in resource map, key=" + key);
        }
        if (awaitEntity == null) {
            awaitEntity = entity;
        }
        Log.await(LOGGER, String.format(" Cloudbreak await for flow %s ", entity));
        try {
            MicroserviceClient msClient = getAdminMicroserviceClient(awaitEntity.getClass(), Crn.fromString(awaitEntity.getCrn()).getAccountId());
            flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, msClient);
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("Cloudbreak await for flow '{}' is failed for: '{}', because of {}", entity, entity.getName(), e.getMessage(), e);
                Log.await(LOGGER, String.format(" Cloudbreak await for flow '%s' is failed for '%s', because of %s",
                        entity, entity.getName(), e.getMessage()));
            }
            getExceptionMap().put("Cloudbreak await for flow " + entity, e);
        }
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
            if (!exception.getClass().equals(expectedException)) {
                String message = String.format("Expected exception (%s) does not match with the actual exception (%s).",
                        expectedException, exception.getClass());
                getExceptionMap().put("expect", new RuntimeException(message));
                Log.expect(LOGGER, message);
            } else if (!isMessageEquals(exception, runningParameter)) {
                String message = String.format("Expected exception message (%s) does not match with the actual exception message (%s).",
                        runningParameter.getExpectedMessage(), ResponseUtil.getErrorMessage(exception));
                getExceptionMap().put("expect", new RuntimeException(message));
                Log.expect(LOGGER, message);
            } else {
                getExceptionMap().remove(key);
                Log.expect(LOGGER, "Expected exception conditions have met, exception: " + expectedException
                        + ", message: " + runningParameter.getExpectedMessage());
            }
        }
        return entity;
    }

    private boolean isMessageEquals(Exception exception, RunningParameter runningParameter) {
        return StringUtils.isEmpty(runningParameter.getExpectedMessage())
                || Pattern.compile(runningParameter.getExpectedMessage()).matcher(ResponseUtil.getErrorMessage(exception)).find();
    }

    public void handleExceptionsDuringTest(TestErrorLog testErrorLog) {
        validated = true;
        checkShutdown();
        if (!exceptionMap.isEmpty()) {
            List<Clue> clues = resources.values().stream()
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
                testResult.setTestName(getTestMethodName().get());
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
        T entity = (T) resources.get(key);
        if (entity == null) {
            LOGGER.warn("Cannot found in the resources [{}], run with the default", entityClass.getSimpleName());
            entity = init(entityClass);
        }
        return entity;
    }

    public CloudbreakUser getWho(RunningParameter runningParameter) {
        Actor actor = runningParameter.getWho();
        if (actor == null) {
            LOGGER.info("Run with acting user. {}", getActingUser());
            return getActingUser();
        } else {
            CloudbreakUser who = actor.acting(testParameter);
            LOGGER.info("Run with given user. {}", who);
            return who;
        }
    }

    private <T> String getKeyForAwait(T entity, Class<? extends T> entityClass, RunningParameter runningParameter) {
        Optional<Map.Entry<String, CloudbreakTestDto>> foundEntry = resources.entrySet().stream()
                .filter(entry -> entry.getValue() == entity)
                .findFirst();
        if (foundEntry.isPresent()) {
            return foundEntry.get().getKey();
        }
        return getKey(entityClass, runningParameter);
    }

    private <T> String getKey(Class<T> entityClass, RunningParameter runningParameter) {
        String key = runningParameter.getKey();
        if (StringUtils.isEmpty(key)) {
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
        List<CloudbreakTestDto> testDtos = new ArrayList<>(getResources().values());
        List<CloudbreakTestDto> orderedTestDtos = testDtos.stream().sorted(new CompareByOrder()).collect(Collectors.toList());
        for (CloudbreakTestDto testDto : orderedTestDtos) {
            try {
                testDto.cleanUp(this, getAdminMicroserviceClient(testDto.getClass(), Crn.fromString(testDto.getCrn()).getAccountId()));
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{clients: " + clients + ", entities: " + resources + "}";
    }
}
