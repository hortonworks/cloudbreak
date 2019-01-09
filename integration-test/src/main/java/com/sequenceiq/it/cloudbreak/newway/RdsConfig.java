package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.ActionV2;
import com.sequenceiq.it.cloudbreak.newway.action.RdsConfigPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.RdsConfigV4Action;

@Prototype
public class RdsConfig extends RdsConfigEntity {
    private static final String RDSCONFIG = "RDSCONFIG";

    protected RdsConfig() {
        super(RDSCONFIG);
    }

    protected RdsConfig(String newId) {
        super(newId);
    }

    public RdsConfig(TestContext testContext) {
        super(testContext);
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
        rdsConfig.setCreationStrategy(RdsConfigV4Action::createInGiven);
        return rdsConfig;
    }

    public static RdsConfig isCreated(String id) {
        var rdsConfig = new RdsConfig();
        rdsConfig.setCreationStrategy(RdsConfigV4Action::createInGiven);
        return rdsConfig;
    }

    public static RdsConfig isCreatedDeleted() {
        RdsConfig rdsConfig = new RdsConfig();
        rdsConfig.setCreationStrategy(RdsConfigV4Action::createDeleteInGiven);
        return rdsConfig;
    }

    public static RdsConfigEntity post(TestContext testContext, RdsConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().databaseV4Endpoint().create(cloudbreakClient.getWorkspaceId(), entity.getRequest())
        );
        return entity;
    }

    public static Action<RdsConfig> post(String key) {
        return new Action<>(getTestContext(key), RdsConfigV4Action::post);
    }

    public static Action<RdsConfig> post() {
        return post(RDSCONFIG);
    }

    public static Action<RdsConfig> get(String key) {
        return new Action<>(getTestContext(key), RdsConfigV4Action::get);
    }

    public static Action<RdsConfig> get() {
        return get(RDSCONFIG);
    }

    public static Action<RdsConfig> getAll() {
        return new Action<>(getNew(), RdsConfigV4Action::getAll);
    }

    public static Action<RdsConfig> delete(String key) {
        return new Action<>(getTestContext(key), RdsConfigV4Action::delete);
    }

    public static Action<RdsConfig> delete() {
        return delete(RDSCONFIG);
    }

    public static RdsConfigEntity delete(TestContext testContext, RdsConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().databaseV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }

    public static Action<RdsConfig> testConnect(String key) {
        return new Action<>(getTestContext(key), RdsConfigV4Action::testConnect);
    }

    public static Action<RdsConfig> testConnect() {
        return testConnect(RDSCONFIG);
    }

    public static Assertion<RdsConfig> assertThis(BiConsumer<RdsConfig, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static ActionV2<RdsConfigEntity> postV2() {
        return new RdsConfigPostAction();
    }
}