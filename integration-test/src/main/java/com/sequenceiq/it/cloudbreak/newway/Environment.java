package com.sequenceiq.it.cloudbreak.newway;

import java.util.Set;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentAttachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentChangeCredentialV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.requests.EnvironmentDetachV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.environment.responses.SimpleEnvironmentV4Response;
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
                cloudbreakClient.getCloudbreakClient().environmentV4Endpoint().post(cloudbreakClient.getWorkspaceId(), entity.getRequest())
        );
        return entity;
    }

    public static EnvironmentEntity delete(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponseSimpleEnv(
                cloudbreakClient.getCloudbreakClient().environmentV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static ResourceAction<Environment> post(String key) {
        return new ResourceAction<>(getTestContext(key), EnvironmentAction::post);
    }

    public static ResourceAction<Environment> post() {
        return post(ENVIRONMENT);
    }

    public static ResourceAction<Environment> get(String key) {
        return new ResourceAction<>(getTestContext(key), EnvironmentAction::get);
    }

    public static ResourceAction<Environment> get() {
        return get(ENVIRONMENT);
    }

    public static EnvironmentEntity get(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().environmentV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static EnvironmentEntity getAll(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponseSimpleEnvSet(
                (Set<SimpleEnvironmentV4Response>) cloudbreakClient.getCloudbreakClient().environmentV4Endpoint()
                        .list(cloudbreakClient.getWorkspaceId()).getResponses()
        );
        return entity;
    }

    public static ResourceAction<Environment> getAll() {
        return new ResourceAction<>(getNew(), EnvironmentAction::list);
    }

    public static ResourceAction<Environment> putAttachResources(String key) {
        return new ResourceAction<>(getTestContext(key), EnvironmentAction::putAttachResources);
    }

    public static EnvironmentEntity putAttachResources(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        EnvironmentAttachV4Request environmentAttachV4Request = new EnvironmentAttachV4Request();
        environmentAttachV4Request.setLdaps(entity.getRequest().getLdaps());
        environmentAttachV4Request.setProxies(entity.getRequest().getProxies());
        environmentAttachV4Request.setDatabases(entity.getRequest().getDatabases());
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().environmentV4Endpoint()
                        .attach(cloudbreakClient.getWorkspaceId(), entity.getName(), environmentAttachV4Request)
        );
        return entity;
    }

    public static ResourceAction<Environment> putDetachResources(String key) {
        return new ResourceAction<>(getTestContext(key), EnvironmentAction::putDetachResources);
    }

    public static EnvironmentEntity putDetachResources(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        EnvironmentDetachV4Request environmentDetachV4Request = new EnvironmentDetachV4Request();
        environmentDetachV4Request.setLdaps(entity.getRequest().getLdaps());
        environmentDetachV4Request.setProxies(entity.getRequest().getProxies());
        environmentDetachV4Request.setDatabases(entity.getRequest().getDatabases());
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().environmentV4Endpoint()
                        .detach(cloudbreakClient.getWorkspaceId(), entity.getName(), environmentDetachV4Request)
        );
        return entity;
    }

    public static EnvironmentEntity changeCredential(TestContext testContext, EnvironmentEntity entity, CloudbreakClient cloudbreakClient) {
        EnvironmentChangeCredentialV4Request envChangeCredentialRequest = new EnvironmentChangeCredentialV4Request();
        envChangeCredentialRequest.setCredential(entity.getRequest().getCredential());
        envChangeCredentialRequest.setCredentialName(entity.getRequest().getCredentialName());
        entity.setResponse(cloudbreakClient.getCloudbreakClient().environmentV4Endpoint().changeCredential(cloudbreakClient.getWorkspaceId(), entity.getName(),
                envChangeCredentialRequest)
        );
        return entity;
    }
}