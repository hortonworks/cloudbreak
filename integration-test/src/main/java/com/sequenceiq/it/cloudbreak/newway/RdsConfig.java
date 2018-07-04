package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;

public class RdsConfig extends RdsConfigEntity {
    private static final String RDSCONFIG = "RDSCONFIG";

    protected RdsConfig() {
        super(RDSCONFIG);
    }

    protected RdsConfig(String newId) {
        super(newId);
    }

    private static Function<IntegrationTestContext, RdsConfig> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, RdsConfig.class);
    }

    static Function<IntegrationTestContext, RdsConfig> getNew() {
        return testContext -> new RdsConfig();
    }

    public static RdsConfig request() {
        return new RdsConfig();
    }

    public static RdsConfig isCreated() {
        RdsConfig rdsConfig = new RdsConfig();
        rdsConfig.setCreationStrategy(RdsConfigAction::createInGiven);
        return rdsConfig;
    }

    public static RdsConfig isCreatedDeleted() {
        RdsConfig rdsConfig = new RdsConfig();
        rdsConfig.setCreationStrategy(RdsConfigAction::createDeleteInGiven);
        return rdsConfig;
    }

    public static Action<RdsConfig> post(String key) {
        return new Action<>(getTestContext(key), RdsConfigAction::post);
    }

    public static Action<RdsConfig> post() {
        return post(RDSCONFIG);
    }

    public static Action<RdsConfig> get(String key) {
        return new Action<>(getTestContext(key), RdsConfigAction::get);
    }

    public static Action<RdsConfig> get() {
        return get(RDSCONFIG);
    }

    public static Action<RdsConfig> getAll() {
        return new Action<>(getNew(), RdsConfigAction::getAll);
    }

    public static Action<RdsConfig> delete(String key) {
        return new Action<>(getTestContext(key), RdsConfigAction::delete);
    }

    public static Action<RdsConfig> delete() {
        return delete(RDSCONFIG);
    }

    public static Action<RdsConfig> testConnect(String key) {
        return new Action<>(getTestContext(key), RdsConfigAction::testConnect);
    }

    public static Action<RdsConfig> testConnect() {
        return testConnect(RDSCONFIG);
    }

    public static Assertion<RdsConfig> assertThis(BiConsumer<RdsConfig, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}