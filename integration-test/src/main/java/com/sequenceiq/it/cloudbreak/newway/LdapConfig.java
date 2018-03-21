package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.ldap.LdapConfigRequest;
import com.sequenceiq.it.IntegrationTestContext;


public class LdapConfig extends LdapConfigEntity {
    private static final String LDAPCONFIG = "LdapCONFIG";

    private final LdapConfigRequest ldapConfigRequest = new LdapConfigRequest();

    private LdapConfig() {
        super(LDAPCONFIG);
    }

    private static Function<IntegrationTestContext, LdapConfig> getTestContext(String key) {
        return (testContext) -> testContext.getContextParam(key, LdapConfig.class);
    }

    static Function<IntegrationTestContext, LdapConfig> getNew() {
        return (testContext)->new LdapConfig();
    }

    public static LdapConfig request() {
        return new LdapConfig();
    }

    public LdapConfigRequest getRequest() {
        return ldapConfigRequest;
    }

    public static LdapConfig isCreated() {
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setCreationStrategy(LdapConfigAction::createInGiven);
        return ldapConfig;
    }

    public static LdapConfig isCreatedDeleted() {
        LdapConfig ldapConfig = new LdapConfig();
        ldapConfig.setCreationStrategy(LdapConfigAction::createDeleteInGiven);
        return ldapConfig;
    }

    public static Action<LdapConfig> post(String key) {
        return new Action<>(getTestContext(key), LdapConfigAction::post);
    }

    public static Action<LdapConfig> post() {
        return post(LDAPCONFIG);
    }

    public static Action<LdapConfig> get(String key) {
        return new Action<>(getTestContext(key), LdapConfigAction::get);
    }

    public static Action<LdapConfig> get() {
        return get(LDAPCONFIG);
    }

    public static Action<LdapConfig> getAll() {
        return new Action<>(getNew(), LdapConfigAction::getAll);
    }

    public static Action<LdapConfig> delete(String key) {
        return new Action<>(getTestContext(key), LdapConfigAction::delete);
    }

    public static Action<LdapConfig> delete() {
        return delete(LDAPCONFIG);
    }

    public static Assertion<LdapConfig> assertThis(BiConsumer<LdapConfig, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}