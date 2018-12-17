package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentAttachRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentChangeCredentialRequest;
import com.sequenceiq.cloudbreak.api.model.environment.request.EnvironmentDetachRequest;
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

    public static EnvironmentEntity delete(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponseSimpleEnv(
                cloudbreakClient.getCloudbreakClient().environmentV3Endpoint().delete(cloudbreakClient.getWorkspaceId(), entity.getName())
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

    public static EnvironmentEntity get(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().environmentV3Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static EnvironmentEntity getAll(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponseSimpleEnvSet(
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

    public static EnvironmentEntity putAttachResources(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        EnvironmentAttachRequest environmentAttachRequest = new EnvironmentAttachRequest();
        environmentAttachRequest.setLdapConfigs(entity.getRequest().getLdapConfigs());
        environmentAttachRequest.setProxyConfigs(entity.getRequest().getProxyConfigs());
        environmentAttachRequest.setRdsConfigs(entity.getRequest().getRdsConfigs());
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().environmentV3Endpoint().attachResources(cloudbreakClient.getWorkspaceId(), entity.getName(),
                        environmentAttachRequest)
        );
        return entity;
    }

    public static Action<Environment> putDetachResources(String key) {
        return new Action<>(getTestContext(key), EnvironmentAction::putDetachResources);
    }

    public static EnvironmentEntity putDetachResources(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        EnvironmentDetachRequest environmentDetachRequest = new EnvironmentDetachRequest();
        environmentDetachRequest.setLdapConfigs(entity.getRequest().getLdapConfigs());
        environmentDetachRequest.setProxyConfigs(entity.getRequest().getProxyConfigs());
        environmentDetachRequest.setRdsConfigs(entity.getRequest().getRdsConfigs());
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().environmentV3Endpoint().detachResources(cloudbreakClient.getWorkspaceId(), entity.getName(),
                        environmentDetachRequest)
        );
        return entity;
    }

    public static EnvironmentEntity changeCredential(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        EnvironmentChangeCredentialRequest envChangeCredentialRequest = new EnvironmentChangeCredentialRequest();
        envChangeCredentialRequest.setCredential(entity.getRequest().getCredential());
        envChangeCredentialRequest.setCredentialName(entity.getRequest().getCredentialName());
        entity.setResponse(cloudbreakClient.getCloudbreakClient().environmentV3Endpoint().changeCredential(cloudbreakClient.getWorkspaceId(), entity.getName(),
                envChangeCredentialRequest)
        );
        return entity;
    }
}