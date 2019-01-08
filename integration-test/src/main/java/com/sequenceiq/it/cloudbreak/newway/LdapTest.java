package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapV4ValidationRequest;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.LdapConfigV4Action;

public class LdapTest extends LdapTestEntity {
    private static final String LDAPTEST = "LDAPTEST";

    private final LdapV4ValidationRequest ldapV4ValidationRequest = new LdapV4ValidationRequest();

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

    public LdapV4ValidationRequest getRequest() {
        return ldapV4ValidationRequest;
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