package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.proxies.requests.ProxyV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.ProxyConfigPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.ProxyV4Action;

public class ProxyConfig extends ProxyConfigEntity {
    private static final String PROXYCONFIG = "PROXYCONFIG";

    private final ProxyV4Request proxyV4Request = new ProxyV4Request();

    private ProxyConfig() {
        super(PROXYCONFIG);
    }

    private static Function<IntegrationTestContext, ProxyConfig> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, ProxyConfig.class);
    }

    static Function<IntegrationTestContext, ProxyConfig> getNew() {
        return testContext -> new ProxyConfig();
    }

    public static ProxyConfig request() {
        return new ProxyConfig();
    }

    public ProxyV4Request getRequest() {
        return proxyV4Request;
    }

    public static ProxyConfig isCreated() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setCreationStrategy(ProxyV4Action::createInGiven);
        return proxyConfig;
    }

    public static ProxyConfig isCreatedDeleted() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setCreationStrategy(ProxyV4Action::createDeleteInGiven);
        return proxyConfig;
    }

    public static ProxyConfigEntity post(TestContext testContext, ProxyConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().proxyConfigV4Endpoint().post(cloudbreakClient.getWorkspaceId(), entity.getRequest())
        );
        return entity;
    }

    public static ResourceAction post(String key) {
        return new ResourceAction(getTestContext(key), ProxyV4Action::post);
    }

    public static ResourceAction post() {
        return post(PROXYCONFIG);
    }

    public static ResourceAction get(String key) {
        return new ResourceAction(getTestContext(key), ProxyV4Action::get);
    }

    public static ResourceAction get() {
        return get(PROXYCONFIG);
    }

    public static ResourceAction getAll() {
        return new ResourceAction(getNew(), ProxyV4Action::getAll);
    }

    public static ResourceAction delete(String key) {
        return new ResourceAction(getTestContext(key), ProxyV4Action::delete);
    }

    public static ResourceAction delete() {
        return delete(PROXYCONFIG);
    }

    public static ProxyConfigEntity delete(TestContext testContext, ProxyConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().proxyConfigV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Assertion<ProxyConfig> assertThis(BiConsumer<ProxyConfig, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static ProxyConfigEntity getByName(TestContext testContext, ProxyConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().proxyConfigV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Action<ProxyConfigEntity> postV2() {
        return new ProxyConfigPostAction();
    }
}