package com.sequenceiq.it.cloudbreak.context;

import static com.sequenceiq.it.cloudbreak.context.RunningParameter.emptyRunningParameter;

import java.util.ArrayList;
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

import com.sequenceiq.cloudbreak.api.endpoint.v4.common.Status;
import com.sequenceiq.environment.api.v1.environment.model.response.EnvironmentStatus;
import com.sequenceiq.it.TestParameter;
import com.sequenceiq.it.cloudbreak.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.EnvironmentClient;
import com.sequenceiq.it.cloudbreak.FreeIPAClient;
import com.sequenceiq.it.cloudbreak.MicroserviceClient;
import com.sequenceiq.it.cloudbreak.SdxClient;
import com.sequenceiq.it.cloudbreak.action.Action;
import com.sequenceiq.it.cloudbreak.actor.Actor;
import com.sequenceiq.it.cloudbreak.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.assertion.Assertion;
import com.sequenceiq.it.cloudbreak.cloud.v4.CloudProviderProxy;
import com.sequenceiq.it.cloudbreak.dto.CloudbreakTestDto;
import com.sequenceiq.it.cloudbreak.dto.environment.EnvironmentTestDto;
import com.sequenceiq.it.cloudbreak.dto.freeipa.FreeIPATestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxInternalTestDto;
import com.sequenceiq.it.cloudbreak.dto.sdx.SdxTestDto;
import com.sequenceiq.it.cloudbreak.exception.TestFailException;
import com.sequenceiq.it.cloudbreak.finder.Attribute;
import com.sequenceiq.it.cloudbreak.finder.Capture;
import com.sequenceiq.it.cloudbreak.finder.Finder;
import com.sequenceiq.it.cloudbreak.log.Log;
import com.sequenceiq.it.cloudbreak.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.util.ResponseUtil;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtil;
import com.sequenceiq.it.cloudbreak.util.wait.WaitUtilForMultipleStatuses;
import com.sequenceiq.sdx.api.model.SdxClusterStatusResponse;

public abstract class TestContext implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

    private static final String DESCRIPTION = "DESCRIPTION";

    private ApplicationContext applicationContext;

    private final Map<String, CloudbreakTestDto> resources = new LinkedHashMap<>();

    private final Map<String, Map<Class<? extends MicroserviceClient>, MicroserviceClient>> clients = new HashMap<>();

    private final Map<String, Exception> exceptionMap = new HashMap<>();

    private boolean shutdown;

    private final Map<String, String> statuses = new HashMap<>();

    private final Map<String, Object> selections = new HashMap<>();

    private final Map<String, Capture> captures = new HashMap<>();

    private Map<String, Object> contextParameters = new HashMap<>();

    @Inject
    private WaitUtilForMultipleStatuses waitUtil;

    @Inject
    private WaitUtil waitUtilSingleStatus;

    @Inject
    private TestParameter testParameter;

    @Value("${integrationtest.testsuite.cleanUpOnFailure:true}")
    private boolean cleanUpOnFailure;

    @Inject
    private CloudProviderProxy cloudProvider;

    private DefaultModel model;

    private boolean validated;

    private boolean initialized;

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

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped beacause of previous error. when [{}]", key);
            return entity;
        }

        String who = getWho(runningParameter);

        LOGGER.info("when {} action on {}, name: {}", key, entity, entity.getName());
        try {
            return action.action(this, entity, getMicroserviceClient(clientClass, who));
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("when [{}] action is failed: {}, name: {}", key, ResponseUtil.getErrorMessage(e), entity.getName(), e);
            }
            exceptionMap.put(key, e);
        }
        return entity;
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

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped beacause of previous error. when [{}]", key);
            return entity;
        }

        String who = getWho(runningParameter);

        LOGGER.info("then {} assertion on {}, name: {}", key, entity, entity.getName());
        try {
            CloudbreakTestDto cloudbreakTestDto = resources.get(key);
            if (cloudbreakTestDto != null) {
                return assertion.doAssertion(this, (T) cloudbreakTestDto, getMicroserviceClient(clientClass, who));
            } else {
                assertion.doAssertion(this, entity, getMicroserviceClient(clientClass, who));
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("then [{}] assertion is failed: {}, name: {}", key, ResponseUtil.getErrorMessage(e), entity.getName(), e);
            }
            exceptionMap.put(key, e);
        }
        return entity;
    }

    public TestContext as() {
        checkShutdown();
        return as(Actor::defaultUser);
    }

    public TestContext as(Actor actor) {
        checkShutdown();
        CloudbreakUser acting = actor.acting(testParameter);
        if (clients.get(acting.getAccessKey()) == null) {
            CloudbreakClient cloudbreakClient = CloudbreakClient.createProxyCloudbreakClient(testParameter, acting);
            FreeIPAClient freeIPAClient = FreeIPAClient.createProxyFreeIPAClient(testParameter, acting);
            EnvironmentClient environmentClient = EnvironmentClient.createProxyEnvironmentClient(testParameter, acting);
            SdxClient sdxClient = SdxClient.createProxySdxClient(testParameter, acting);
            Map<Class<? extends MicroserviceClient>, MicroserviceClient> clientMap = Map.of(CloudbreakClient.class, cloudbreakClient,
                    FreeIPAClient.class, freeIPAClient, EnvironmentClient.class, environmentClient, SdxClient.class, sdxClient);
            clients.put(acting.getAccessKey(), clientMap);
            cloudbreakClient.setWorkspaceId(0L);
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

    protected String getDefaultUser() {
        return testParameter.get(CloudbreakTest.ACCESS_KEY);
    }

    public <O extends CloudbreakTestDto> O init(Class<O> clss) {
        checkShutdown();
        Log.log(LOGGER, "init " + clss.getSimpleName());
        CloudbreakTestDto bean = applicationContext.getBean(clss, this);
        initialized = true;
        return (O) bean.valid();
    }

    public <O extends CloudbreakTestDto> O given(Class<O> clss) {
        return given(clss.getSimpleName(), clss);
    }

    public <O extends CloudbreakTestDto> O given(String key, Class<O> clss) {
        Optional<TestCaseDescription> description = getDescription();
        if (description.isPresent()) {
            Log.log(LOGGER, "Test case description: " + description.get().getValue());
        }
        checkShutdown();
        O cloudbreakEntity = (O) resources.get(key);
        if (cloudbreakEntity == null) {
            cloudbreakEntity = init(clss);
            resources.put(key, cloudbreakEntity);
        }
        return cloudbreakEntity;
    }

    public Map<String, String> getStatuses() {
        return statuses;
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

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
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
            exceptionMap.put(key, e);
        }
        return entity;
    }

    public <O, T extends CloudbreakTestDto> T capture(T entity, Attribute<T, O> attribute, RunningParameter runningParameter) {
        checkShutdown();
        String key = runningParameter.getKey();
        if (StringUtils.isEmpty(key)) {
            key = entity.getClass().getSimpleName();
        }

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
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
            exceptionMap.put(key, e);
        }
        return entity;
    }

    public <O, T extends CloudbreakTestDto> T verify(T entity, Attribute<T, O> attribute, RunningParameter runningParameter) {
        checkShutdown();
        String key = runningParameter.getKey();
        if (StringUtils.isEmpty(key)) {
            key = attribute.getClass().getSimpleName();
        }

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
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
            exceptionMap.put(key, e);
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
        return getCloudbreakClient(getDefaultUser());
    }

    public <T extends CloudbreakTestDto> T await(Class<T> entityClass, Map<String, Status> desiredStatuses) {
        return await(entityClass, desiredStatuses, emptyRunningParameter(), -1);
    }

    public <T extends CloudbreakTestDto> T await(Class<T> entityClass, Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(getEntityFromEntityClass(entityClass, runningParameter), desiredStatuses, runningParameter, -1);
    }

    public <T extends CloudbreakTestDto> T await(Class<T> entityClass, Map<String, Status> desiredStatuses, RunningParameter runningParameter,
            long pollingInterval) {
        return await(getEntityFromEntityClass(entityClass, runningParameter), desiredStatuses, runningParameter, pollingInterval);
    }

    public <T extends CloudbreakTestDto> T await(T entity, Map<String, Status> desiredStatuses) {
        return await(entity, desiredStatuses, emptyRunningParameter());
    }

    public <T extends CloudbreakTestDto> T await(T entity, Map<String, Status> desiredStatuses, RunningParameter runningParameter) {
        return await(entity, desiredStatuses, runningParameter, -1);
    }

    public <T extends CloudbreakTestDto> T await(T entity, Map<String, Status> desiredStatuses, RunningParameter runningParameter, long pollingInterval) {
        checkShutdown();

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped beacause of previous error. await [{}]", desiredStatuses);
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        CloudbreakTestDto awaitEntity = get(key);
        LOGGER.info("await {} for {}", key, desiredStatuses);
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Key provided but no result in resource map, key=" + key);
            }

            CloudbreakClient cloudbreakClient = getCloudbreakClient(getWho(runningParameter));
            statuses.putAll(waitUtil.waitAndCheckStatuses(cloudbreakClient, awaitEntity.getName(), desiredStatuses, pollingInterval));
            if (!desiredStatuses.containsValue(Status.DELETE_COMPLETED)) {
                awaitEntity.refresh(this, cloudbreakClient);
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("await [{}] is failed for statuses {}: {}, name: {}", entity, desiredStatuses, ResponseUtil.getErrorMessage(e), entity.getName());
            }
            exceptionMap.put("await " + entity + " for desired statuses" + desiredStatuses, e);
        }
        return entity;
    }

    public FreeIPATestDto await(FreeIPATestDto entity, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status desiredStatuses,
            RunningParameter runningParameter) {
        return await(entity, desiredStatuses, runningParameter, -1);
    }

    public FreeIPATestDto await(FreeIPATestDto entity, com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status desiredStatuses,
            RunningParameter runningParameter, long pollingInterval) {
        checkShutdown();

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped beacause of previous error. await [{}]", desiredStatuses);
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        FreeIPATestDto awaitEntity = get(key);
        LOGGER.info("await {} for {}", key, desiredStatuses);
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Key provided but no result in resource map, key=" + key);
            }

            FreeIPAClient freeIPAClient = getMicroserviceClient(FreeIPAClient.class, getWho(runningParameter));
            String environmentCrn = awaitEntity.getRequest().getEnvironmentCrn();
            statuses.putAll(waitUtilSingleStatus.waitAndCheckStatuses(freeIPAClient, environmentCrn, desiredStatuses, pollingInterval));
            if (!desiredStatuses.equals(com.sequenceiq.freeipa.api.v1.freeipa.stack.model.common.Status.DELETE_COMPLETED)) {
                awaitEntity.refresh(this, null);
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("await [{}] is failed for statuses {}: {}, name: {}", entity, desiredStatuses, ResponseUtil.getErrorMessage(e), entity.getName());
            }
            exceptionMap.put("await " + entity + " for desired statuses " + desiredStatuses, e);
        }
        return entity;
    }

    public EnvironmentTestDto await(EnvironmentTestDto entity, EnvironmentStatus desiredStatuses,
            RunningParameter runningParameter) {
        return await(entity, desiredStatuses, runningParameter, -1);
    }

    public EnvironmentTestDto await(EnvironmentTestDto entity, EnvironmentStatus desiredStatuses,
            RunningParameter runningParameter, long pollingInterval) {
        checkShutdown();

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped beacause of previous error. await [{}]", desiredStatuses);
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        EnvironmentTestDto awaitEntity = get(key);
        LOGGER.info("await {} for {}", key, desiredStatuses);
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Key provided but no result in resource map, key=" + key);
            }

            EnvironmentClient environmentClient = getMicroserviceClient(EnvironmentClient.class, getWho(runningParameter));
            String environmentName = awaitEntity.getResponse().getName();
            statuses.putAll(waitUtilSingleStatus.waitAndCheckStatuses(environmentClient, environmentName, desiredStatuses, pollingInterval));
            if (!desiredStatuses.equals(EnvironmentStatus.ARCHIVED)) {
                awaitEntity.refresh(this, null);
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("await [{}] is failed for statuses {}: {}, name: {}", entity, desiredStatuses, ResponseUtil.getErrorMessage(e), entity.getName());
            }
            exceptionMap.put("await " + entity + " for desired statuses " + desiredStatuses, e);
        }
        return entity;
    }

    public SdxTestDto await(SdxTestDto entity, SdxClusterStatusResponse desiredStatuses,
            RunningParameter runningParameter) {
        return await(entity, desiredStatuses, runningParameter, -1);
    }

    public SdxTestDto await(SdxTestDto entity, SdxClusterStatusResponse desiredStatuses,
            RunningParameter runningParameter, long pollingInterval) {
        checkShutdown();

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped beacause of previous error. await [{}]", desiredStatuses);
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxTestDto awaitEntity = get(key);
        LOGGER.info("await {} for {}", key, desiredStatuses);
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Key provided but no result in resource map, key=" + key);
            }

            SdxClient sdxClient = getMicroserviceClient(SdxClient.class, getWho(runningParameter));
            String sdxName = entity.getName();
            statuses.putAll(waitUtilSingleStatus.waitAndCheckStatuses(sdxClient, sdxName, desiredStatuses, pollingInterval));
            if (!desiredStatuses.equals(SdxClusterStatusResponse.DELETED)) {
                awaitEntity.refresh(this, (CloudbreakClient) null);
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("await [{}] is failed for statuses {}: {}, name: {}", entity, desiredStatuses, ResponseUtil.getErrorMessage(e), entity.getName());
            }
            exceptionMap.put("await " + entity + " for desired statuses " + desiredStatuses, e);
        }
        return entity;
    }

    public SdxInternalTestDto await(SdxInternalTestDto entity, SdxClusterStatusResponse desiredStatuses,
            RunningParameter runningParameter) {
        return await(entity, desiredStatuses, runningParameter, -1);
    }

    public SdxInternalTestDto await(SdxInternalTestDto entity, SdxClusterStatusResponse desiredStatuses,
            RunningParameter runningParameter, long pollingInterval) {
        checkShutdown();

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped beacause of previous error. await [{}]", desiredStatuses);
            return entity;
        }
        String key = getKeyForAwait(entity, entity.getClass(), runningParameter);
        SdxInternalTestDto awaitEntity = get(key);
        LOGGER.info("await {} for {}", key, desiredStatuses);
        try {
            if (awaitEntity == null) {
                throw new RuntimeException("Key provided but no result in resource map, key=" + key);
            }

            SdxClient sdxClient = getMicroserviceClient(SdxClient.class, getWho(runningParameter));
            String sdxName = entity.getName();
            statuses.putAll(waitUtilSingleStatus.waitAndCheckStatuses(sdxClient, sdxName, desiredStatuses, pollingInterval));
            if (!desiredStatuses.equals(SdxClusterStatusResponse.DELETED)) {
                awaitEntity.refresh(this, (CloudbreakClient) null);
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("await [{}] is failed for statuses {}: {}, name: {}", entity, desiredStatuses, ResponseUtil.getErrorMessage(e), entity.getName());
            }
            exceptionMap.put("await " + entity + " for desired statuses " + desiredStatuses, e);
        }
        return entity;
    }

    public <E extends Exception, T extends CloudbreakTestDto> T expect(T entity, Class<E> expectedException, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKey(entity.getClass(), runningParameter);
        Exception exception = exceptionMap.get(key);
        if (exception == null) {
            String message = "Expected an exception but cannot find with key: " + key;
            exceptionMap.put("expect", new RuntimeException(message));
        } else {
            if (!exception.getClass().equals(expectedException)) {
                String message = String.format("Expected exception (%s) does not match with the actual exception (%s).",
                        expectedException, exception.getClass());
                exceptionMap.put("expect", new RuntimeException(message));
            } else if (!isMessageEquals(exception, runningParameter)) {
                String message = String.format("Expected exception message (%s) does not match with the actual exception message (%s).",
                        runningParameter.getExpectedMessage(), ResponseUtil.getErrorMessage(exception));
                exceptionMap.put("expect", new RuntimeException(message));
            } else {
                exceptionMap.remove(key);
            }
        }
        return entity;
    }

    private boolean isMessageEquals(Exception exception, RunningParameter runningParameter) {
        return StringUtils.isEmpty(runningParameter.getExpectedMessage())
                || Pattern.compile(runningParameter.getExpectedMessage()).matcher(ResponseUtil.getErrorMessage(exception)).find();
    }

    public void handleExecptionsDuringTest(boolean silently) {
        validated = true;
        checkShutdown();
        Map<String, Exception> exceptionsDuringTest = getErrors();
        if (!exceptionsDuringTest.isEmpty()) {
            StringBuilder builder = new StringBuilder("All Exceptions that occurred during the test are logged before this message")
                    .append(System.lineSeparator());
            exceptionsDuringTest.forEach((msg, ex) -> {
                LOGGER.error(msg, ex);
                builder.append(msg).append(": ").append(ResponseUtil.getErrorMessage(ex)).append(System.lineSeparator());
            });
            exceptionsDuringTest.clear();
            if (!silently) {
                throw new TestFailException(builder.toString());
            }
        }
    }

    private <T extends CloudbreakTestDto> T getEntityFromEntityClass(Class<T> entityClass, RunningParameter runningParameter) {
        String key = getKey(entityClass, runningParameter);
        T entity = (T) resources.get(key);
        if (entity == null) {
            LOGGER.warn("Cannot found in the resources [{}], run with the default", entityClass.getSimpleName());
            entity = init(entityClass);
        }
        return entity;
    }

    private String getWho(RunningParameter runningParameter) {
        String who = runningParameter.getWho();
        if (StringUtils.isEmpty(who)) {
            who = getDefaultUser();
            LOGGER.info("Run with default user. {}", who);
        } else {
            String secondUser = testParameter.get(who);
            if (StringUtils.isEmpty(secondUser)) {
                LOGGER.info("Run with the given user. {}", secondUser);
            } else {
                who = secondUser;
                LOGGER.info("Run with the second user. {}", who);
            }
        }
        return who;
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
        if (!validated && initialized) {
            throw new IllegalStateException(
                    "Test context should be validated! Maybe do you forgot to call .validate() end of the test? See other tests as an example.");
        }
        checkShutdown();
        handleExecptionsDuringTest(true);
        if (!cleanUpOnFailure && !getExceptionMap().isEmpty()) {
            LOGGER.info("Cleanup skipped beacuse cleanupOnFail is false");
            return;
        }
        List<CloudbreakTestDto> testDtos = new ArrayList<>(getResources().values());
        List<CloudbreakTestDto> orderedTestDtos = testDtos.stream().sorted(new CompareByOrder()).collect(Collectors.toList());
        for (CloudbreakTestDto testDto : orderedTestDtos) {
            try {
                testDto.cleanUp(this, getCloudbreakClient(getDefaultUser()));
            } catch (Exception e) {
                LOGGER.error("Was not able to cleanup resource [{}]., {}", testDto.getName(), ResponseUtil.getErrorMessage(e), e);
            }
        }
        shutdown();
    }

    public void shutdown() {
        setShutdown(true);
    }
}
