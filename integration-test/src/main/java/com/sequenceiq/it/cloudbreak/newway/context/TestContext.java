package com.sequenceiq.it.cloudbreak.newway.context;

import static com.sequenceiq.it.cloudbreak.newway.context.RunningParameter.emptyRunningParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakTest;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.action.ActionV2;
import com.sequenceiq.it.cloudbreak.newway.actor.Actor;
import com.sequenceiq.it.cloudbreak.newway.actor.CloudbreakUser;
import com.sequenceiq.it.cloudbreak.newway.assertion.AssertionV2;
import com.sequenceiq.it.cloudbreak.newway.config.SparkServer;
import com.sequenceiq.it.cloudbreak.newway.entity.CloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.finder.Attribute;
import com.sequenceiq.it.cloudbreak.newway.finder.Capture;
import com.sequenceiq.it.cloudbreak.newway.finder.Finder;
import com.sequenceiq.it.cloudbreak.newway.log.Log;
import com.sequenceiq.it.cloudbreak.newway.mock.DefaultModel;
import com.sequenceiq.it.cloudbreak.newway.mock.ImageCatalogMockServerSetup;
import com.sequenceiq.it.cloudbreak.newway.wait.WaitUtil;

@Prototype
public class TestContext implements ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestContext.class);

    private Map<String, CloudbreakEntity> resources = new LinkedHashMap<>();

    private Map<String, CloudbreakClient> clients = new HashMap<>();

    private Map<String, String> statuses = new HashMap<>();

    private Map<String, Exception> exceptionMap = new HashMap<>();

    private Map<String, Object> selections = new HashMap<>();

    private Map<String, Capture> captures = new HashMap<>();

    @Value("${integrationtest.testsuite.cleanUpOnFailure:true}")
    private boolean cleanUpOnFailure;

    @Value("${mock.server.address:localhost}")
    private String mockServerAddress;

    @Inject
    private WaitUtil waitUtil;

    @Inject
    private TestParameter testParameter;

    @Inject
    private SparkServer sparkServer;

    @Inject
    private ImageCatalogMockServerSetup imageCatalogMockServerSetup;

    private DefaultModel model;

    private boolean shutdown;

    private ApplicationContext applicationContext;

    @PostConstruct
    private void init() {
        sparkServer.initSparkService(9750, 9900);
        imageCatalogMockServerSetup.configureImgCatalogMock();
        model = new DefaultModel();
        model.startModel(sparkServer.getSparkService(), mockServerAddress);
    }

    public <T extends CloudbreakEntity> T when(Class<T> entityClass, ActionV2<T> action) {
        return when(entityClass, action, emptyRunningParameter());
    }

    public <T extends CloudbreakEntity> T when(Class<T> entityClass, ActionV2<T> action, RunningParameter runningParameter) {
        return when(getEntityFromEntityClass(entityClass, runningParameter), action, runningParameter);
    }

    public <T extends CloudbreakEntity> T when(T entity, ActionV2<T> action) {
        return when(entity, action, emptyRunningParameter());
    }

    public <T extends CloudbreakEntity> T when(T entity, ActionV2<T> action, RunningParameter runningParameter) {
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
            return action.action(this, entity, getCloudbreakClient(who));
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("when [{}] action is failed: {}, name: {}", key, e.getMessage(), entity.getName(), e);
            }
            exceptionMap.put(key, e);
        }
        return entity;
    }

    public <T extends CloudbreakEntity> T then(Class<T> entityClass, AssertionV2<T> assertion) {
        return then(entityClass, assertion, emptyRunningParameter());
    }

    public <T extends CloudbreakEntity> T then(Class<T> entityClass, AssertionV2<T> assertion, RunningParameter runningParameter) {
        return then(getEntityFromEntityClass(entityClass, runningParameter), assertion, emptyRunningParameter());
    }

    public <T extends CloudbreakEntity> T then(T entity, AssertionV2<T> assertion) {
        return then(entity, assertion, emptyRunningParameter());
    }

    public <T extends CloudbreakEntity> T then(T entity, AssertionV2<T> assertion, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKey(assertion.getClass(), runningParameter);

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped beacause of previous error. when [{}]", key);
            return entity;
        }

        String who = getWho(runningParameter);

        LOGGER.info("then {} assertion on {}, name: {}", key, entity, entity.getName());
        try {
            return assertion.doAssertion(this, entity, getCloudbreakClient(who));
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("then [{}] assertion is failed: {}, name: {}", key, e.getMessage(), entity.getName(), e);
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
        if (clients.get(acting.getUsername()) == null) {
            CloudbreakClient cloudbreakClient = CloudbreakClient.createProxyCloudbreakClient(testParameter, acting);
            clients.put(acting.getUsername(), cloudbreakClient);
            Long workspaceId = cloudbreakClient.getCloudbreakClient()
                    .workspaceV3Endpoint()
                    .getByName(acting.getUsername()).getId();
            cloudbreakClient.setWorkspaceId(workspaceId);
        }
        return this;
    }

    private String getDefaultUser() {
        return testParameter.get(CloudbreakTest.USER);
    }

    public <O extends CloudbreakEntity> O init(Class<O> clss) {
        checkShutdown();
        Log.log(LOGGER, "init " + clss.getSimpleName());
        CloudbreakEntity bean = applicationContext.getBean(clss, this);
        return (O) bean.valid();
    }

    public <O extends CloudbreakEntity> O given(Class<O> clss) {
        return given(clss.getSimpleName(), clss);
    }

    public <O extends CloudbreakEntity> O given(String key, Class<O> clss) {
        checkShutdown();
        O entity = (O) resources.computeIfAbsent(key, value -> init(clss));
        resources.put(clss.getSimpleName(), entity);
        return entity;
    }

    public void addStatuses(Map<String, String> statuses) {
        this.statuses.putAll(statuses);
    }

    public Map<String, String> getStatuses() {
        return statuses;
    }

    public Map<String, Exception> getErrors() {
        return exceptionMap;
    }

    public <T extends CloudbreakEntity> T get(String key) {
        return (T) resources.get(key);
    }

    public <T extends CloudbreakEntity> T get(Class<T> clss) {
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

    public <O, T extends CloudbreakEntity> T select(Class<T> entityClass, Attribute<T, O> attribute, Finder<O> finder, RunningParameter runningParameter) {
        return select(getEntityFromEntityClass(entityClass, runningParameter), attribute, finder, runningParameter);
    }

    public <O, T extends CloudbreakEntity> T select(T entity, Attribute<T, O> attribute, Finder<O> finder, RunningParameter runningParameter) {
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
                        key, attribute, finder, e.getMessage(), entity.getName());
            }
            exceptionMap.put(key, e);
        }
        return entity;
    }

    public <O, T extends CloudbreakEntity> T capture(T entity, Attribute<T, O> attribute, RunningParameter runningParameter) {
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
                LOGGER.error("capture [{}] is failed: {}, name: {}", key, e.getMessage(), entity.getName());
            }
            exceptionMap.put(key, e);
        }
        return entity;
    }

    public <O, T extends CloudbreakEntity> T verify(T entity, Attribute<T, O> attribute, RunningParameter runningParameter) {
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
                LOGGER.error("verify [key={}] is failed: {}, name: {}", key, e.getMessage(), entity.getName(), e);
            }
            exceptionMap.put(key, e);
        }
        return entity;
    }

    private CloudbreakClient getCloudbreakClient(String who) {
        CloudbreakClient cloudbreakClient = clients.get(who);
        if (cloudbreakClient == null) {
            throw new IllegalStateException("Should create a client for this user: " + who);
        }
        return cloudbreakClient;
    }

    public <T extends CloudbreakEntity> T await(Class<T> entityClass, Map<String, String> desiredStatuses) {
        return await(entityClass, desiredStatuses, emptyRunningParameter());
    }

    public <T extends CloudbreakEntity> T await(Class<T> entityClass, Map<String, String> desiredStatuses, RunningParameter runningParameter) {
        return await(getEntityFromEntityClass(entityClass, runningParameter), desiredStatuses, runningParameter);
    }

    public <T extends CloudbreakEntity> T await(T entity, Map<String, String> desiredStatuses) {
        return await(entity, desiredStatuses, emptyRunningParameter());
    }

    public <T extends CloudbreakEntity> T await(T entity, Map<String, String> desiredStatuses, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKey(entity.getClass(), runningParameter);

        if (!exceptionMap.isEmpty() && runningParameter.isSkipOnFail()) {
            LOGGER.info("Should be skipped beacause of previous error. await [{}]", desiredStatuses);
            return entity;
        }
        LOGGER.info("await {} for {}", key, desiredStatuses);
        try {
            CloudbreakClient cloudbreakClient = getCloudbreakClient(getWho(runningParameter));
            statuses.putAll(waitUtil.waitAndCheckStatuses(cloudbreakClient, entity.getName(), desiredStatuses));
            if (!desiredStatuses.values().contains("DELETE_COMPLETED")) {
                entity.refresh(this, cloudbreakClient);
            }
        } catch (Exception e) {
            if (runningParameter.isLogError()) {
                LOGGER.error("await [{}] is failed for statuses {}: {}, name: {}", entity, desiredStatuses, e.getMessage(), entity.getName());
            }
            exceptionMap.put("await " + entity + " for desired statuses" + desiredStatuses, e);
        }
        return entity;
    }

    public void cleanupTestContextEntity() {
        checkShutdown();
        handleExecptionsDuringTest();
        if (!cleanUpOnFailure && !exceptionMap.isEmpty()) {
            LOGGER.info("Cleanup skipped beacuse cleanupOnFail is false");
            return;
        }
        List<CloudbreakEntity> entities = new ArrayList<>(resources.values());
        Collections.reverse(entities);
        entities.stream().forEach(entryset -> {
            try {
                //TODO this needs better implementation
                entryset.cleanUp(this, clients.get(getDefaultUser()));
            } catch (Exception e) {
                LOGGER.error("Was not able to cleanup resource, possible that it was cleaned up before");
            }
        });
        shutdown();
    }

    public DefaultModel getModel() {
        return model;
    }

    public SparkServer getSparkServer() {
        return sparkServer;
    }

    public ImageCatalogMockServerSetup getImageCatalogMockServerSetup() {
        return imageCatalogMockServerSetup;
    }

    public <E extends Exception, T extends CloudbreakEntity> T expect(T entity, Class<E> expectedException, RunningParameter runningParameter) {
        checkShutdown();
        String key = getKey(entity.getClass(), runningParameter);
        Exception exception = exceptionMap.get(key);
        if (exception == null) {
            exceptionMap.put("expect", new RuntimeException("Expected an exception but cannot find with key: " + key));
        }
        if (exception != null && !exception.getClass().equals(expectedException)) {
            exceptionMap.put("expect", new RuntimeException(String.format("Expected exception (%s) is not match with the actual exception (%s).",
                    expectedException, exception.getClass())));
        } else {
            exceptionMap.remove(key);
        }
        return entity;
    }

    public void handleExecptionsDuringTest() {
        checkShutdown();
        Map<String, Exception> exceptionsDuringTest = getErrors();
        if (!exceptionsDuringTest.isEmpty()) {
            LOGGER.error("Status reason: {}", statuses.get("statusReason"));
            StringBuilder br = new StringBuilder("All Exceptions during test are logged before this message").append(System.lineSeparator());
            exceptionsDuringTest.forEach((msg, ex) -> {
                LOGGER.error(msg, ex);
                br.append(msg).append(": ").append(ex.getMessage()).append(System.lineSeparator());
            });
            exceptionsDuringTest.clear();
            Assert.fail(br.toString());
        }
    }

    private <T extends CloudbreakEntity> T getEntityFromEntityClass(Class<T> entityClass, RunningParameter runningParameter) {
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

    private <T> String getKey(Class<T> entityClass, RunningParameter runningParameter) {
        String key = runningParameter.getKey();
        if (StringUtils.isEmpty(key)) {
            key = entityClass.getSimpleName();
        }
        return key;
    }

    private void checkShutdown() {
        if (shutdown) {
            throw new IllegalStateException("Cannot access this TestContext anymore because of it is shutted down.");
        }
    }

    public void shutdown() {
        sparkServer.shutdown();
        imageCatalogMockServerSetup.shutdown();
        shutdown = true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
