package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.proxy.ProxyConfigRequest;
import com.sequenceiq.it.IntegrationTestContext;

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
        proxyConfig.setCreationStrategy(ProxyConfigAction::createInGiven);
        return proxyConfig;
    }

    public static ProxyConfig isCreatedDeleted() {
        ProxyConfig proxyConfig = new ProxyConfig();
        proxyConfig.setCreationStrategy(ProxyConfigAction::createDeleteInGiven);
        return proxyConfig;
    }

    public static Action<ProxyConfig> post(String key) {
        return new Action<>(getTestContext(key), ProxyConfigAction::post);
    }

    public static Action<ProxyConfig> post() {
        return post(PROXYCONFIG);
    }

    public static Action<ProxyConfig> get(String key) {
        return new Action<>(getTestContext(key), ProxyConfigAction::get);
    }

    public static Action<ProxyConfig> get() {
        return get(PROXYCONFIG);
    }

    public static Action<ProxyConfig> getAll() {
        return new Action<>(getNew(), ProxyConfigAction::getAll);
    }

    public static Action<ProxyConfig> delete(String key) {
        return new Action<>(getTestContext(key), ProxyConfigAction::delete);
    }

    public static Action<ProxyConfig> delete() {
        return delete(PROXYCONFIG);
    }

    public static Assertion<ProxyConfig> assertThis(BiConsumer<ProxyConfig, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}