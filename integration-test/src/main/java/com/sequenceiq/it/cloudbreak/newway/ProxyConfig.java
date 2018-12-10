package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.ActionV2;
import com.sequenceiq.it.cloudbreak.newway.action.ProxyConfigPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.ProxyConfigV3Action;

public class ProxyConfig extends ProxyConfigEntity {
    private static final String PROXYCONFIG = "PROXYCONFIG";

    private final ProxyConfigRequest proxyConfigRequest = new ProxyConfigRequest();

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

    public ProxyConfigRequest getRequest() {
        return proxyConfigRequest;
    }

    public static ProxyConfig isCreated() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setCreationStrategy(ProxyConfigV3Action::createInGiven);
        return proxyConfig;
    }

    public static ProxyConfig isCreatedDeleted() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setCreationStrategy(ProxyConfigV3Action::createDeleteInGiven);
        return proxyConfig;
    }

    public static ProxyConfigEntity post(TestContext testContext, ProxyConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().proxyConfigV3Endpoint().createInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getRequest())
        );
        return entity;
    }

    public static Action<ProxyConfig> post(String key) {
        return new Action<>(getTestContext(key), ProxyConfigV3Action::post);
    }

    public static Action<ProxyConfig> post() {
        return post(PROXYCONFIG);
    }

    public static Action<ProxyConfig> get(String key) {
        return new Action<>(getTestContext(key), ProxyConfigV3Action::get);
    }

    public static Action<ProxyConfig> get() {
        return get(PROXYCONFIG);
    }

    public static Action<ProxyConfig> getAll() {
        return new Action<>(getNew(), ProxyConfigV3Action::getAll);
    }

    public static Action<ProxyConfig> delete(String key) {
        return new Action<>(getTestContext(key), ProxyConfigV3Action::delete);
    }

    public static Action<ProxyConfig> delete() {
        return delete(PROXYCONFIG);
    }

    public static ProxyConfigEntity delete(TestContext testContext, ProxyConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().proxyConfigV3Endpoint().deleteInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Assertion<ProxyConfig> assertThis(BiConsumer<ProxyConfig, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static ProxyConfigEntity getByName(TestContext testContext, ProxyConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().proxyConfigV3Endpoint().getByNameInWorkspace(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static ActionV2<ProxyConfigEntity> postV2() {
        return new ProxyConfigPostAction();
    }
}