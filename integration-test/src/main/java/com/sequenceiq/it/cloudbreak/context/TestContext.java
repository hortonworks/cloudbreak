package com.sequenceiq.it.cloudbreak.context;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

import com.google.common.collect.Iterables;
import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.common.mappable.CloudPlatform;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIpaClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.RedbeamsClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonCloudProperties;
import com.sequenceiq.it.cloudbreak.cloud.v4.CommonClusterManagerProperties;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.finder.Attribute;
import com.sequenceiq.it.cloudbreak.finder.Capture;
import com.sequenceiq.it.cloudbreak.finder.Finder;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.it.cloudbreak.util.wait.service.WaitService;
import com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak.CloudbreakAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.cloudbreak.CloudbreakWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.datalake.DatalakeAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.datalake.DatalakeInternalAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.datalake.DatalakeWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.environment.EnvironmentAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.environment.EnvironmentWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.FreeIpaAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.freeipa.FreeIpaWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.redbeams.RedbeamsAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.redbeams.RedbeamsWaitObject;
import com.sequenceiq.it.util.TagsUtil;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public abstract class TestContext implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

    private static final String DESCRIPTION = "DESCRIPTION";

    private static final String TEST_METHOD_NAME = "TEST_METHOD_NAME";

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
    private WaitUtil waitUtilSingleStatus;

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
    private EnvironmentAwait environmentAwait;

    @Inject
    private FreeIpaAwait freeIpaAwait;

    @Inject
    private DatalakeAwait datalakeAwait;

    @Inject
    private DatalakeInternalAwait datalakeInternalAwait;

    @Inject
    private RedbeamsAwait redbeamsAwait;

    @Inject
    private CloudbreakAwait cloudbreakAwait;

    @Inject
    private WaitService<EnvironmentWaitObject> environmentWaitService;

    @Inject
    private WaitService<FreeIpaWaitObject> freeIpaWaitService;

    @Inject
    private WaitService<DatalakeWaitObject> datalakeWaitService;

    @Inject
    private WaitService<RedbeamsWaitObject> redbeamsWaitService;

    @Inject
    private WaitService<CloudbreakWaitObject> cloudbreakWaitService;

    @Inject
    private TagsUtil tagsUtil;

    private DefaultModel model;

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

    public WaitService<EnvironmentWaitObject> getEnvironmentWaitService() {
        return environmentWaitService;
    }

    public WaitService<FreeIpaWaitObject> getFreeIpaWaitService() {
        return freeIpaWaitService;
    }

    public WaitService<DatalakeWaitObject> getDatalakeWaitService() {
        return datalakeWaitService;
    }

    public WaitService<RedbeamsWaitObject> getRedbeamsWaitService() {
        return redbeamsWaitService;
    }

    public WaitService<CloudbreakWaitObject> getCloudbreakWaitService() {
        return cloudbreakWaitService;
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
            LOGGER.info("Should be skipped beacause of previous error. when [{}]", key);
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

    protected TestContext getTestContext() {
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
        return action.action(getTestContext(), entity, getMicroserviceClient(clientClass, who));
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
            LOGGER.info("Should be skipped beacause of previous error. when [{}]", key);
            return entity;
        }

        CloudbreakUser who = getWho(runningParameter);

        Log.then(LOGGER, assertion.getClass().getSimpleName() + " assertion on " + entity + " by " + who);
        try {
            CloudbreakTestDto cloudbreakTestDto = resources.get(key);
            if (cloudbreakTestDto != null) {
                return assertion.doAssertion(this, (T) cloudbreakTestDto, getMicroserviceClient(clientClass, who.getAccessKey()));
            } else {
                assertion.doAssertion(this, entity, getMicroserviceClient(clientClass, who.getAccessKey()));
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
            RedbeamsClient redbeamsClient = RedbeamsClient.createProxyRedbeamsClient(testParameter, acting);
            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Map.of(CloudbreakClient.class, cloudbreakClient,
                    FreeIpaClient.class, freeIpaClient, EnvironmentClient.class, environmentClient, SdxClient.class, sdxClient,
                    RedbeamsClient.class, redbeamsClient);
            clients.put(acting.getAccessKey(), clientMap);
            cloudbreakClient.setWorkspaceId(0L);
            redbeamsClient.setEnvironmentCrn(Crn.builder()
                    .setService(Crn.Service.ENVIRONMENTS)
                    .setAccountId("it")
                    .setResourceType(Crn.ResourceType.ENVIRONMENT)
                    .setResource("test-environment")
                    .build().toString());
        }
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

    public TestContext setTestMethodName(String testMethodName) {
        getTestContext().contextParameters.put(TEST_METHOD_NAME, testMethodName);
        return this;
    }

    public Optional<String> getTestMethodName() {
        return Optional.ofNullable(getTestContext().contextParameters.get(TEST_METHOD_NAME))
                .map(Object::toString);
    }

    protected String getActingUserAccessKey() {
        if (this.actingUser == null) {
            return testParameter.get(CloudbreakTest.ACCESS_KEY);
        }
        return actingUser.getAccessKey();
    }

    public Crn getActingUserCrn() {
        return Crn.fromString(new String(Base64.getDecoder().decode(getActingUserAccessKey())));
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
            tagsUtil.addTestNameTag(bean, getTestMethodName().orElse("missing"));
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
            LOGGER.info("Should be skipped beacause of previous error. select: attr: [{}], finder: [{}]", attribute, finder);
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
            LOGGER.info("Should be skipped beacause of previous error. capture [{}]", attribute);
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
            LOGGER.info("Should be skipped beacause of previous error. verify [{}]", attribute);
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

    public <U extends MicroserviceClient> U getMicroserviceClient(Class<? extends MicroserviceClient> msClientClass, String who) {
        U microserviceClient = (U) clients.getOrDefault(who, Map.of()).get(msClientClass);
        if (microserviceClient == null) {
            throw new IllegalStateException("Should create a client for this user: " + who);
        }
        return microserviceClient;
    }

    public CloudbreakClient getCloudbreakClient() {
        return getCloudbreakClient(getActingUserAccessKey());
    }

    public <U extends MicroserviceClient> U getMicroserviceClient(Class<? extends MicroserviceClient> msClientClass) {
        return getMicroserviceClient(msClientClass, getActingUserAccessKey());
    }

    public <T extends CloudbreakTestDto> T await(Class<T> entityClass, Map<String, Status> desiredStatuses) {
        return await(entityClass, desiredStatuses, emptyRunningParameter(), getPollingDurationInMills());
    }

    public <T extends CloudbreakTestDto> T await(Class<T> entityClass, Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(getEntityFromEntityClass(entityClass, runningParameter), desiredStatuses, runningParameter, getPollingDurationInMills());
    }

    public <T extends CloudbreakTestDto> T await(Class<T> entityClass, Map<String, Status> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        return await(getEntityFromEntityClass(entityClass, runningParameter), desiredStatuses, runningParameter, pollingInterval);
    }

    public <T extends CloudbreakTestDto> T await(T entity, Map<String, Status> desiredStatuses) {
        return await(entity, desiredStatuses, emptyRunningParameter(), getPollingDurationInMills());
    }

    public <T extends CloudbreakTestDto> T await(T entity, Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(entity, desiredStatuses, runningParameter, getPollingDurationInMills());
    }

    public <T extends SdxTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxTestDto awaitEntity = get(key);
        SdxClient sdxClient = getMicroserviceClient(SdxClient.class, getWho(runningParameter).getAccessKey());
        waitUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, sdxClient);
        return entity;
    }

    public <T extends SdxInternalTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxInternalTestDto awaitEntity = get(key);
        SdxClient sdxClient = getMicroserviceClient(SdxClient.class, getWho(runningParameter).getAccessKey());
        waitUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, sdxClient);
        return entity;
    }

    public <T extends CloudbreakTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        CloudbreakTestDto awaitEntity = get(key);
        CloudbreakClient cloudbreakClient = getCloudbreakClient();
        waitUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, cloudbreakClient);
        return entity;
    }

    public <T extends CloudbreakTestDto, E extends Status> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Cloudbreak await should be skipped beacause of previous error. await [%s]", desiredStatuses));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        CloudbreakTestDto awaitEntity = get(key);
        cloudbreakAwait.await(awaitEntity, (Map<String, Status>) desiredStatuses, getTestContext(), runningParameter, pollingInterval, maxRetry);
        return entity;
    }

    public <T extends EnvironmentTestDto, E extends EnvironmentStatus> T await(T entity, E desiredStatus, RunningParameter runningParameter) {
        return await(entity, desiredStatus, runningParameter, getPollingDurationInMills());
    }

    public <T extends EnvironmentTestDto, E extends EnvironmentStatus> T await(T entity, E desiredStatus, RunningParameter runningParameter,
            Duration pollingInterval) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Environment await should be skipped beacause of previous error. await [%s]", desiredStatus));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        EnvironmentTestDto awaitEntity = get(key);
        environmentAwait.await(awaitEntity, desiredStatus, getTestContext(), runningParameter, pollingInterval, maxRetry);
        return entity;
    }

    public <T extends FreeIpaTestDto, E extends com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status> T await(T entity, E desiredStatus,
            RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("FreeIpa await should be skipped beacause of previous error. await [%s]", desiredStatus));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        FreeIpaTestDto awaitEntity = get(key);
        freeIpaAwait.await(awaitEntity, desiredStatus, getTestContext(), runningParameter, getPollingDurationInMills(), maxRetry);
        return entity;
    }

    public <T extends SdxTestDto, E extends SdxClusterStatusResponse> T await(T entity, E desiredStatus,
            RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Datalake await should be skipped beacause of previous error. await [%s]", desiredStatus));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxTestDto awaitEntity = get(key);
        datalakeAwait.await(awaitEntity, desiredStatus, getTestContext(), runningParameter, getPollingDurationInMills(), maxRetry);
        return entity;
    }

    public <T extends SdxInternalTestDto, E extends SdxClusterStatusResponse> T await(T entity, E desiredStatus,
            RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Datalake Internal await should be skipped beacause of previous error. await [%s]", desiredStatus));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxInternalTestDto awaitEntity = get(key);
        datalakeInternalAwait.await(awaitEntity, desiredStatus, getTestContext(), runningParameter, getPollingDurationInMills(), maxRetry);
        return entity;
    }

    public <T extends RedbeamsDatabaseServerTestDto, E extends com.sequenceiq.redbeams.api.model.common.Status> T await(T entity, E desiredStatus,
            RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Redbeams await should be skipped beacause of previous error. await [%s]", desiredStatus));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        RedbeamsDatabaseServerTestDto awaitEntity = get(key);
        redbeamsAwait.await(awaitEntity, desiredStatus, getTestContext(), runningParameter, getPollingDurationInMills(), maxRetry);
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
        Map<String, Exception> exceptionsDuringTest = getErrors();
        if (!exceptionsDuringTest.isEmpty()) {
            StringBuilder builder = new StringBuilder("All Exceptions that occurred during the test are logged after this message")
                    .append(System.lineSeparator());
            exceptionsDuringTest.forEach((msg, ex) -> {
                LOGGER.error("Exception during test: " + msg, ex);
                builder.append(msg).append(": ").append(ResponseUtil.getErrorMessage(ex)).append(System.lineSeparator());
            });
            collectStructuredEvents(builder);
            exceptionsDuringTest.clear();
            testErrorLog.report(LOGGER, builder.toString().replace("%", "%%"));
        }
    }

    private void collectStructuredEvents(StringBuilder builder) {
        Iterable<Investigable> investigables = Iterables.filter(resources.values(), Investigable.class);
        investigables.forEach(dto -> builder.append(dto.investigate()).append(System.lineSeparator()));
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
            throw new IllegalStateException("Cannot access this MockedTestContext anymore because of it is shutted down.");
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
                testDto.cleanUp(this, getCloudbreakClient(getActingUserAccessKey()));
            } catch (Exception e) {
                LOGGER.error("Was not able to cleanup resource [{}]., {}", testDto.getName(), ResponseUtil.getErrorMessage(e), e);
            }
        }
        shutdown();
        testDtos.forEach(tagsUtil::verifyTestNameTag);
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
