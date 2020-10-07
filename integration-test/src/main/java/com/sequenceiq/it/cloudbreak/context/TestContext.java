package com.sequenceiq.it.cloudbreak.context;

import static com.sequenceiq.cloudbreak.auth.ThreadBasedUserCrnProvider.INTERNAL_ACTOR_CRN;
import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.ws.rs.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;
import org.testng.ITestResult;
import org.testng.Reporter;

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.base.InstanceStatus;
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
import com.sequenceiq.it.cloudbreak.dto.database.RedbeamsDatabaseServerTestDto;
import com.sequenceiq.it.cloudbreak.dto.distrox.DistroXTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIpaTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxCMDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxDiagnosticsTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.finder.Attribute;
import com.sequenceiq.it.cloudbreak.finder.Capture;
import com.sequenceiq.it.cloudbreak.finder.Finder;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.testcase.authorization.AuthUserKeys;
import com.sequenceiq.it.cloudbreak.util.ErrorLogMessageProvider;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.FlowUtil;
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
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.DistroxInstanceAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.InstanceWaitObject;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.SdxInstanceAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.instance.SdxInternalInstanceAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.redbeams.RedbeamsAwait;
import com.sequenceiq.it.cloudbreak.util.wait.service.redbeams.RedbeamsWaitObject;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

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
    private WaitService<InstanceWaitObject> instanceWaitService;

    @Inject
    private DistroxInstanceAwait distroxInstanceAwait;

    @Inject
    private SdxInstanceAwait sdxInstanceAwait;

    @Inject
    private SdxInternalInstanceAwait sdxInternalInstanceAwait;

    @Inject
    private ErrorLogMessageProvider errorLogMessageProvider;

    @Inject
    private Tracer tracer;

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

    public WaitService<InstanceWaitObject> getInstanceWaitService() {
        return instanceWaitService;
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

    protected <T extends CloudbreakTestDto, U extends MicroserviceClient>
    T doActionAsAdmin(T entity, Class<? extends MicroserviceClient> clientClass, Action<T, U> action, String who) throws Exception {
        return action.action(getTestContext(), entity, getAdminMicroserviceClient(clientClass));
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
            UmsClient umsClient = UmsClient.createProxyUmsClient(tracer);
            RedbeamsClient redbeamsClient = RedbeamsClient.createProxyRedbeamsClient(testParameter, acting);
            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Map.of(CloudbreakClient.class, cloudbreakClient,
                    FreeIpaClient.class, freeIpaClient, EnvironmentClient.class, environmentClient, SdxClient.class, sdxClient,
                    RedbeamsClient.class, redbeamsClient,
                    UmsClient.class, umsClient);
            clients.put(acting.getAccessKey(), clientMap);
            clients.put(INTERNAL_ACTOR_ACCESS_KEY, clientMap);
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
        // real ums user
        if (Crn.isCrn(getActingUser().getCrn())) {
            return Crn.fromString(getActingUser().getCrn());
        }
        return Crn.fromString(new String(Base64.getDecoder().decode(getActingUserAccessKey())));
    }

    public String getActingUserName() {
        return Crn.fromString(new String(Base64.getDecoder().decode(getActingUserAccessKey()))).getUserId();
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

    public <U extends MicroserviceClient> U getAdminMicroserviceClient(Class<? extends MicroserviceClient> msClientClass) {
        String accessKey;
        if (CloudbreakUserCache.getInstance().isInitialized()) {
            accessKey = CloudbreakUserCache.getInstance().getByName(AuthUserKeys.ACCOUNT_ADMIN).getAccessKey();
        } else {
            accessKey = INTERNAL_ACTOR_ACCESS_KEY;
        }
        U microserviceClient = (U) clients.getOrDefault(accessKey, Map.of()).get(msClientClass);
        if (microserviceClient == null) {
            throw new IllegalStateException("Should create a client for this user: " + AuthUserKeys.ACCOUNT_ADMIN);
        }
        return microserviceClient;
    }

    public <U extends MicroserviceClient> U getMicroserviceClient(Class<? extends MicroserviceClient> msClientClass, String who) {
        U microserviceClient = (U) clients.getOrDefault(who, Map.of()).get(msClientClass);
        if (microserviceClient == null) {
            throw new IllegalStateException("Should create a client for this user: " + who);
        }
        return microserviceClient;
    }

    public SdxClient getSdxClient() {
        return getSdxClient(getActingUserAccessKey());
    }

    public <U extends MicroserviceClient> U getMicroserviceClient(Class<U> msClientClass) {
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

    public <T extends CloudbreakTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "Cloudbreak await for flow should be skipped because of previous error.");
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        CloudbreakTestDto awaitEntity = get(key);
        Log.await(LOGGER, String.format(" Cloudbreak await for flow %s ", entity));
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Cloudbreak key provided but no result in resource map, key=" + key);
            }
            CloudbreakClient cloudbreakClient = getAdminMicroserviceClient(CloudbreakClient.class);
            flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, cloudbreakClient);
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

    public <T extends SdxTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "Sdx await for flow should be skipped because of previous error.");
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxTestDto awaitEntity = get(key);
        Log.await(LOGGER, String.format(" Sdx await for flow %s ", entity));
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Sdx key provided but no result in resource map, key=" + key);
            }
            SdxClient sdxClient = getAdminMicroserviceClient(SdxClient.class);
            flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, sdxClient);
            try {
                awaitEntity.setResponse(
                        sdxClient.getSdxClient().sdxEndpoint()
                                .getDetail(awaitEntity.getName(), Collections.emptySet())
                );
            } catch (NotFoundException e) {
                LOGGER.warn("Sdx '{}:{}' has been removed. So cannot refresh!", entity.getName(), entity.getCrn());
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("Sdx await for flow '{}' is failed for: '{}', because of {}", entity, entity.getName(), e.getMessage(), e);
                Log.await(LOGGER, String.format(" Sdx await for flow '%s' is failed for '%s', because of %s",
                        entity, entity.getName(), e.getMessage()));
            }
            getExceptionMap().put("Sdx await for flow " + entity, e);
        }
        return entity;
    }

    public <T extends SdxInternalTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "Sdx internal await for flow should be skipped because of previous error.");
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxInternalTestDto awaitEntity = get(key);
        Log.await(LOGGER, String.format(" Sdx internal await for flow %s ", entity));
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Sdx internal key provided but no result in resource map, key=" + key);
            }
            SdxClient sdxClient = getAdminMicroserviceClient(SdxClient.class);
            flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, sdxClient);
            try {
                awaitEntity.setResponse(
                        sdxClient.getSdxClient().sdxEndpoint()
                                .getDetail(awaitEntity.getName(), Collections.emptySet())
                );
            } catch (NotFoundException e) {
                LOGGER.warn("Sdx internal '{}:{}' has been removed. So cannot refresh!", entity.getName(), entity.getCrn());
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("Sdx internal await for flow '{}' is failed for: '{}', because of {}", entity, entity.getName(), e.getMessage(), e);
                Log.await(LOGGER, String.format(" Sdx internal await for flow '%s' is failed for '%s', because of %s",
                        entity, entity.getName(), e.getMessage()));
            }
            getExceptionMap().put("Sdx internal await for flow " + entity, e);
        }
        return entity;
    }

    public <T extends DistroXTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "Distrox await for flow should be skipped because of previous error.");
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        DistroXTestDto awaitEntity = get(key);
        Log.await(LOGGER, String.format(" Distrox await for flow %s ", entity));
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Distrox key provided but no result in resource map, key=" + key);
            }
            CloudbreakClient cloudbreakClient = getAdminMicroserviceClient(CloudbreakClient.class);
            flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, cloudbreakClient);
            try {
                awaitEntity.setResponse(
                        cloudbreakClient.getCloudbreakClient().distroXV1Endpoint()
                                .getByName(awaitEntity.getName(), Collections.emptySet())
                );
            } catch (NotFoundException e) {
                LOGGER.warn("Distrox '{}:{}' has been removed. So cannot refresh!", entity.getName(), entity.getCrn());
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("Distrox await for flow '{}' is failed for: '{}', because of {}", entity, entity.getName(), e.getMessage(), e);
                Log.await(LOGGER, String.format(" Distrox await for flow '%s' is failed for '%s', because of %s",
                        entity, entity.getName(), e.getMessage()));
            }
            getExceptionMap().put("Distrox await for flow " + entity, e);
        }
        return entity;
    }

    public <T extends EnvironmentTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "Environment await for flow should be skipped because of previous error.");
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        EnvironmentTestDto awaitEntity = get(key);
        Log.await(LOGGER, String.format(" Environment await for flow %s ", entity));
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Environment key provided but no result in resource map, key=" + key);
            }

            EnvironmentClient environmentClient = getAdminMicroserviceClient(EnvironmentClient.class);
            flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, environmentClient);
            try {
                awaitEntity.setResponse(
                        environmentClient.getEnvironmentClient().environmentV1Endpoint()
                                .getByName(awaitEntity.getName())
                );
            } catch (NotFoundException e) {
                LOGGER.warn("Environment '{}:{}' has been removed. So cannot refresh!", entity.getName(), entity.getCrn());
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("Environment await for flow '{}' is failed for: '{}', because of {}", entity, entity.getName(), e.getMessage(), e);
                Log.await(LOGGER, String.format(" Environment await for flow '%s' is failed for '%s', because of %s",
                        entity, entity.getName(), e.getMessage()));
            }
            getExceptionMap().put("Environment await for flow " + entity, e);
        }
        return entity;
    }

    public <T extends FreeIpaDiagnosticsTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "FreeIpa diagnostics await for flow should be skipped because of previous error.");
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        FreeIpaDiagnosticsTestDto awaitEntity = get(key);
        Log.await(LOGGER, String.format(" FreeIpa diagnostics await for flow %s ", entity));
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("FreeIpa diagnostics key provided but no result in resource map, key=" + key);
            }
            FreeIpaClient freeIpaClient = getMicroserviceClient(FreeIpaClient.class, INTERNAL_ACTOR_ACCESS_KEY);
            flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, freeIpaClient);
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("FreeIpa diagnostics await for flow '{}' is failed for: '{}', because of {}", entity, entity.getName(), e.getMessage(), e);
                Log.await(LOGGER, String.format(" FreeIpa diagnostics await for flow '%s' is failed for '%s', because of %s",
                        entity, entity.getName(), e.getMessage()));
            }
            getExceptionMap().put("FreeIpa diagnostics await for flow " + entity, e);
        }
        return entity;
    }

    public <T extends SdxDiagnosticsTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "Sdx diagnostics await for flow should be skipped because of previous error.");
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxDiagnosticsTestDto awaitEntity = get(key);
        Log.await(LOGGER, String.format(" Sdx diagnostics await for flow %s ", entity));
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Sdx diagnostics key provided but no result in resource map, key=" + key);
            }
            SdxClient sdxClient = getMicroserviceClient(SdxClient.class, INTERNAL_ACTOR_ACCESS_KEY);
            flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, sdxClient);
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("Sdx diagnostics await for flow '{}' is failed for: '{}', because of {}", entity, entity.getName(), e.getMessage(), e);
                Log.await(LOGGER, String.format(" Sdx diagnostics await for flow '%s' is failed for '%s', because of %s",
                        entity, entity.getName(), e.getMessage()));
            }
            getExceptionMap().put("Sdx diagnostics await for flow " + entity, e);
        }
        return entity;
    }

    public <T extends SdxCMDiagnosticsTestDto> T awaitForFlow(T entity, RunningParameter runningParameter) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, "Sdx CM based diagnostics await for flow should be skipped because of previous error.");
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxDiagnosticsTestDto awaitEntity = get(key);
        Log.await(LOGGER, String.format(" Sdx CM based diagnostics await for flow %s ", entity));
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Sdx CM based diagnostics key provided but no result in resource map, key=" + key);
            }
            SdxClient sdxClient = getMicroserviceClient(SdxClient.class, INTERNAL_ACTOR_ACCESS_KEY);
            flowUtilSingleStatus.waitBasedOnLastKnownFlow(awaitEntity, sdxClient);
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("Sdx CM based diagnostics await for flow '{}' is failed for: '{}', because of {}",
                        entity, entity.getName(), e.getMessage(), e);
                Log.await(LOGGER, String.format(" Sdx CM based diagnostics await for flow '%s' is failed for '%s', because of %s",
                        entity, entity.getName(), e.getMessage()));
            }
            getExceptionMap().put("Sdx CM based diagnostics await for flow " + entity, e);
        }
        return entity;
    }

    public <T extends CloudbreakTestDto, E extends Status> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Cloudbreak await should be skipped because of previous error. await [%s]", desiredStatuses));
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
            Log.await(LOGGER, String.format("Environment await should be skipped because of previous error. await [%s]", desiredStatus));
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
            Log.await(LOGGER, String.format("FreeIpa await should be skipped because of previous error. await [%s]", desiredStatus));
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
            Log.await(LOGGER, String.format("Datalake await should be skipped because of previous error. await [%s]", desiredStatus));
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
            Log.await(LOGGER, String.format("Datalake Internal await should be skipped because of previous error. await [%s]", desiredStatus));
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
            Log.await(LOGGER, String.format("Redbeams await should be skipped because of previous error. await [%s]", desiredStatus));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        RedbeamsDatabaseServerTestDto awaitEntity = get(key);
        redbeamsAwait.await(awaitEntity, desiredStatus, getTestContext(), runningParameter, getPollingDurationInMills(), maxRetry);
        return entity;
    }

    public <T extends DistroXTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        return await(entity, desiredStatuses, runningParameter, getPollingDurationInMills());
    }

    public <T extends DistroXTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Distrox Instance await should be skipped because of previous error. await [%s]", desiredStatuses));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        DistroXTestDto awaitEntity = get(key);
        distroxInstanceAwait.await(awaitEntity, (Map<String, InstanceStatus>) desiredStatuses, getTestContext(), runningParameter, pollingInterval, maxRetry);
        return entity;
    }

    public <T extends SdxTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        return await(entity, desiredStatuses, runningParameter, getPollingDurationInMills());
    }

    public <T extends SdxTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Sdx Instance await should be skipped because of previous error. await [%s]", desiredStatuses));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxTestDto awaitEntity = get(key);
        sdxInstanceAwait.await(awaitEntity, (Map<String, InstanceStatus>) desiredStatuses, getTestContext(), runningParameter, pollingInterval, maxRetry);
        return entity;
    }

    public <T extends SdxInternalTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter) {
        return await(entity, desiredStatuses, runningParameter, getPollingDurationInMills());
    }

    public <T extends SdxInternalTestDto, E extends InstanceStatus> T await(T entity, Map<String, E> desiredStatuses, RunningParameter runningParameter,
            Duration pollingInterval) {
        checkShutdown();
        if (!getExceptionMap().isEmpty() && runningParameter.isSkipOnFail()) {
            Log.await(LOGGER, String.format("Sdx Instance await should be skipped because of previous error. await [%s]", desiredStatuses));
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxInternalTestDto awaitEntity = get(key);
        sdxInternalInstanceAwait.await(awaitEntity, (Map<String, InstanceStatus>) desiredStatuses, getTestContext(), runningParameter, pollingInterval,
                maxRetry);
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
            testErrorLog.report(LOGGER, errorMessage);

            ITestResult testResult = Reporter.getCurrentTestResult();
            Throwable testFailException = errorLogMessageProvider.getException(exceptionMap);

            if (testFailException != null) {
                testResult.setThrowable(testFailException);
                testResult.setTestName(getTestMethodName().get());
                testResult.setStatus(ITestResult.FAILURE);

                String methodName = testResult.getName();
                int status = testResult.getStatus();
                String testFailureType = testResult.getThrowable().getCause().getClass().getName();
                String message = testResult.getThrowable().getCause().getMessage() != null
                        ? testResult.getThrowable().getCause().getMessage()
                        : testResult.getThrowable().getMessage();
                LOGGER.info("Failed test results are: Test Case: {} | Status: {} | Failure Type: {} | Message: {}", methodName, status,
                        testFailureType, message);
                testResult.getTestContext().setAttribute(methodName + OUTPUT_FAILURE_TYPE, testFailureType);
                testResult.getTestContext().setAttribute(methodName + OUTPUT_FAILURE, testResult.getThrowable());
            } else {
                LOGGER.error("Test Context TestFailException is null! So cannot get the correct test fail result!");
            }

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
                testDto.cleanUp(this, getAdminMicroserviceClient(CloudbreakClient.class));
            } catch (Exception e) {
                LOGGER.error("Cleaning up of {} resource is failing, because of: {}", testDto.getName(), e.getMessage(), e);
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
