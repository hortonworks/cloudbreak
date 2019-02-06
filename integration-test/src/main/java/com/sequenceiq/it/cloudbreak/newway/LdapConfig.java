package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.LdapConfigPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.LdapConfigV4Action;

@Prototype
public class LdapConfig extends LdapConfigEntity {
    private static final String LDAPCONFIG = "LdapCONFIG";

    private LdapConfig() {
        super(LDAPCONFIG);
    }

    public LdapConfig(TestContext testContext) {
        super(testContext);
    }

    private static Function<IntegrationTestContext, LdapConfig> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, LdapConfig.class);
    }

    static Function<IntegrationTestContext, LdapConfig> getNew() {
        return testContext -> new LdapConfig();
    }

    public static LdapConfig request() {
        return new LdapConfig();
    }

    public static LdapConfig isCreated() {
        var ldapConfig = new LdapConfig();
        ldapConfig.setCreationStrategy(LdapConfigV4Action::createInGiven);
        return ldapConfig;
    }

    public static LdapConfig isCreatedDeleted() {
        var ldapConfig = new LdapConfig();
        ldapConfig.setCreationStrategy(LdapConfigV4Action::createDeleteInGiven);
        return ldapConfig;
    }

    public static LdapConfigEntity post(TestContext testContext, LdapConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().ldapConfigV4Endpoint().post(cloudbreakClient.getWorkspaceId(), entity.getRequest())

        );
        return entity;
    }

    public static ResourceAction post(String key) {
        return new ResourceAction(getTestContext(key), LdapConfigV4Action::post);
    }

    public static ResourceAction post() {
        return post(LDAPCONFIG);
    }

    public static ResourceAction get(String key) {
        return new ResourceAction(getTestContext(key), LdapConfigV4Action::get);
    }

    public static ResourceAction get() {
        return get(LDAPCONFIG);
    }

    public static ResourceAction getAll() {
        return new ResourceAction(getNew(), LdapConfigV4Action::getAll);
    }

    public static ResourceAction delete(String key) {
        return new ResourceAction(getTestContext(key), LdapConfigV4Action::delete);
    }

    public static ResourceAction delete() {
        return delete(LDAPCONFIG);
    }

    public static LdapConfigEntity delete(TestContext testContext, LdapConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().ldapConfigV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), entity.getName())

        );
        return entity;
    }

    public static Assertion<LdapConfig> assertThis(BiConsumer<LdapConfig, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static LdapConfig isCreatedWithParameters(TestParameter testParameter) {
        var ldapConfig = new LdapConfig();
        ldapConfig.setRequest(LdapConfigRequestDataCollector.createLdapRequestWithProperties(testParameter));
        ldapConfig.setCreationStrategy(LdapConfigV4Action::createInGiven);
        return ldapConfig;
    }

    public static LdapConfig isCreatedWithParametersAndName(TestParameter testParameter, String name) {
        var ldapConfig = new LdapConfig();
        ldapConfig.setRequest(LdapConfigRequestDataCollector.createLdapRequestWithPropertiesAndName(testParameter, name));
        ldapConfig.setCreationStrategy(LdapConfigV4Action::createInGiven);
        return ldapConfig;
    }

    public static Action<LdapConfigEntity> postV2() {
        return new LdapConfigPostAction();
    }

    public static LdapConfigEntity getByName(TestContext testContext, LdapConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().ldapConfigV4Endpoint().get(cloudbreakClient.getWorkspaceId(), entity.getName())
        );
        return entity;
    }
}