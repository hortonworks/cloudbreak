package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.model.ldap.LdapValidationRequest;
import com.sequenceiq.it.IntegrationTestContext;


public class LdapTest extends LdapTestEntity {
    private static final String LDAPTEST = "LDAPTEST";

    private final LdapValidationRequest ldapValidationRequest = new LdapValidationRequest();

    private LdapTest() {
        super(LDAPTEST);
    }

    private static Function<IntegrationTestContext, LdapTest> getTestContext(String key) {
        return (testContext) -> testContext.getContextParam(key, LdapTest.class);
    }

    static Function<IntegrationTestContext, LdapTest> getNew() {
        return (testContext)->new LdapTest();
    }

    public static LdapTest request() {
        return new LdapTest();
    }

    public LdapValidationRequest getRequest() {
        return ldapValidationRequest;
    }

    public static Action<LdapTest> testConnect(String key) {
        return new Action<>(getTestContext(key), LdapConfigAction::testConnect);
    }

    public static Action<LdapTest> testConnect() {
        return testConnect(LDAPTEST);
    }

    public static Assertion<LdapTest> assertThis(BiConsumer<LdapTest, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}