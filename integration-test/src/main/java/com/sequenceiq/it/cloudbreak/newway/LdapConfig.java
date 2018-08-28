package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v3.LdapConfigV3Action;

public class LdapConfig extends LdapConfigEntity {
    private static final String LDAPCONFIG = "LdapCONFIG";

    private LdapConfig() {
        super(LDAPCONFIG);
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
        ldapConfig.setCreationStrategy(LdapConfigV3Action::createInGiven);
        return ldapConfig;
    }

    public static LdapConfig isCreatedDeleted() {
        var ldapConfig = new LdapConfig();
        ldapConfig.setCreationStrategy(LdapConfigV3Action::createDeleteInGiven);
        return ldapConfig;
    }

    public static Action<LdapConfig> post(String key) {
        return new Action<>(getTestContext(key), LdapConfigV3Action::post);
    }

    public static Action<LdapConfig> post() {
        return post(LDAPCONFIG);
    }

    public static Action<LdapConfig> get(String key) {
        return new Action<>(getTestContext(key), LdapConfigV3Action::get);
    }

    public static Action<LdapConfig> get() {
        return get(LDAPCONFIG);
    }

    public static Action<LdapConfig> getAll() {
        return new Action<>(getNew(), LdapConfigV3Action::getAll);
    }

    public static Action<LdapConfig> delete(String key) {
        return new Action<>(getTestContext(key), LdapConfigV3Action::delete);
    }

    public static Action<LdapConfig> delete() {
        return delete(LDAPCONFIG);
    }

    public static Assertion<LdapConfig> assertThis(BiConsumer<LdapConfig, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }

    public static LdapConfig isCreatedWithParameters(TestParameter testParameter) {
        var ldapConfig = new LdapConfig();
        ldapConfig.setRequest(LdapConfigRequestDataCollector.createLdapRequestWithProperties(testParameter));
        ldapConfig.setCreationStrategy(LdapConfigV3Action::createInGiven);
        return ldapConfig;
    }

    public static LdapConfig isCreatedWithParametersAndName(TestParameter testParameter, String name) {
        var ldapConfig = new LdapConfig();
        ldapConfig.setRequest(LdapConfigRequestDataCollector.createLdapRequestWithPropertiesAndName(testParameter, name));
        ldapConfig.setCreationStrategy(LdapConfigV3Action::createInGiven);
        return ldapConfig;
    }
}
