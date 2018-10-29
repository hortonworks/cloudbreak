package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class Environment extends EnvironmentEntity {

    public Environment() {
    }

    public Environment(TestContext testContext) {
        super(testContext);
    }

    static Function<IntegrationTestContext, Environment> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, Environment.class);
    }

    static Function<IntegrationTestContext, Environment> getNew() {
        return testContext -> new Environment();
    }

    public static Environment request() {
        return new Environment();
    }

    public static Environment isCreated() {
        Environment environment = new Environment();
        environment.setCreationStrategy(EnvironmentAction::createInGiven);
        return environment;
    }

    public static EnvironmentEntity post(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().environmentV3Endpoint().create(cloudbreakClient.getWorkspaceId(), entity.getRequest())
        );
        return entity;
    }

    public static Action<Environment> post(String key) {
        return new Action<>(getTestContext(key), EnvironmentAction::post);
    }

    public static Action<Environment> post() {
        return post(ENVIRONMENT);
    }

    public static Action<Environment> get(String key) {
        return new Action<>(getTestContext(key), EnvironmentAction::get);
    }

    public static Action<Environment> get() {
        return get(ENVIRONMENT);
    }

    public static EnvironmentEntity getAll(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponseSimpleEnv(
                cloudbreakClient.getCloudbreakClient().environmentV3Endpoint().list(cloudbreakClient.getWorkspaceId())
        );
        return entity;
    }

    public static Action<Environment> getAll() {
        return new Action<>(getNew(), EnvironmentAction::list);
    }

    public static Action<Environment> putAttachResources(String key) {
        return new Action<>(getTestContext(key), EnvironmentAction::putAttachResources);
    }

    public static Action<Environment> putDetachResources(String key) {
        return new Action<>(getTestContext(key), EnvironmentAction::putDetachResources);
    }
}