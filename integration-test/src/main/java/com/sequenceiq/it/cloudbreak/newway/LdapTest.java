package com.sequenceiq.it.cloudbreak.newway;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.cloudbreak.api.endpoint.v4.ldaps.requests.LdapMinimalV4Request;
import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.newway.v4.LdapConfigV4Action;

public class LdapTest extends LdapTestEntity {
    private static final String LDAPTEST = "LDAPTEST";

    private final LdapMinimalV4Request ldapTestConnectionV4Request = new LdapMinimalV4Request();

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

    public LdapMinimalV4Request getRequest() {
        return ldapTestConnectionV4Request;
    }

    public static ResourceAction<LdapTest> testConnect(String key) {
        return new ResourceAction<>(getTestContext(key), LdapConfigV4Action::testConnect);
    }

    public static ResourceAction<LdapTest> testConnect() {
        return testConnect(LDAPTEST);
    }

    public static Assertion<LdapTest> assertThis(BiConsumer<LdapTest, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}