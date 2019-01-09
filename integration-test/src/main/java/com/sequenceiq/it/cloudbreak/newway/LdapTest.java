package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapTestConnectionV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.LdapConfigV4Action;

public class LdapTest extends LdapTestEntity {
    private static final String LDAPTEST = "LDAPTEST";

    private final LdapTestConnectionV4Request ldapTestConnectionV4Request = new LdapTestConnectionV4Request();

    private LdapTest() {
        super(LDAPTEST);
    }

    private static Function<IntegrationTestContext, LdapTest> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, LdapTest.class);
    }

    static Function<IntegrationTestContext, LdapTest> getNew() {
        return testContext -> new LdapTest();
    }

    public static LdapTest request() {
        return new LdapTest();
    }

    public LdapTestConnectionV4Request getRequest() {
        return ldapTestConnectionV4Request;
    }

    public static Action<LdapTest> testConnect(String key) {
        return new Action<>(getTestContext(key), LdapConfigV4Action::testConnect);
    }

    public static Action<LdapTest> testConnect() {
        return testConnect(LDAPTEST);
    }

    public static Assertion<LdapTest> assertThis(BiConsumer<LdapTest, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}