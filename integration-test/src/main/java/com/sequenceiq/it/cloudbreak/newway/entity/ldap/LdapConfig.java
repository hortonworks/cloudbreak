package com.sequenceiq.it.cloudbreak.newway.entity.ldap;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.Assertion;
import com.sequenceiq.it.cloudbreak.newway.CloudbreakClient;
import com.sequenceiq.it.cloudbreak.newway.GherkinTest;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.ResourceAction;
import com.sequenceiq.it.cloudbreak.newway.TestParameter;
import com.sequenceiq.it.cloudbreak.newway.action.Action;
import com.sequenceiq.it.cloudbreak.newway.action.ldap.LdapConfigDeleteAction;
import com.sequenceiq.it.cloudbreak.newway.action.ldap.LdapConfigPostAction;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.LdapConfigV4Action;

@Prototype
public class LdapConfig extends LdapConfigEntity {

    private static final String LDAPCONFIG = "LDAP_CONFIG";

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

    public static LdapConfigEntity post(LdapConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().ldapConfigV4Endpoint().post(cloudbreakClient.getWorkspaceId(), entity.getRequest())
        );
        return entity;
    }

    public static ResourceAction<LdapConfig> post(String key) {
        return new ResourceAction<>(getTestContext(key), LdapConfigV4Action::post);
    }

    public static ResourceAction<LdapConfig> post() {
        return post(LDAPCONFIG);
    }

    public static Action<LdapConfigEntity> postV4() {
        return new LdapConfigPostAction();
    }

    public static ResourceAction<LdapConfig> get(String key) {
        return new ResourceAction<>(getTestContext(key), LdapConfigV4Action::get);
    }

    public static ResourceAction<LdapConfig> get() {
        return get(LDAPCONFIG);
    }

    public static ResourceAction<LdapConfig> getAll() {
        return new ResourceAction<>(getNew(), LdapConfigV4Action::getAll);
    }

    public static ResourceAction<LdapConfig> delete(String key) {
        return new ResourceAction<>(getTestContext(key), LdapConfigV4Action::delete);
    }

    public static ResourceAction<LdapConfig> delete() {
        return delete(LDAPCONFIG);
    }

    public static LdapConfigEntity delete(TestContext testContext, LdapConfigEntity entity, CloudbreakClient cloudbreakClient) {
        entity.setResponse(
                cloudbreakClient.getCloudbreakClient().ldapConfigV4Endpoint().delete(cloudbreakClient.getWorkspaceId(), entity.getName())

        );
        return entity;
    }

    public static Action<LdapConfigEntity> deleteV4() {
        return new LdapConfigDeleteAction();
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