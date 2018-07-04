package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.it.IntegrationTestContext;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class SecurityRules extends SecurityRulesEntity {

    static Function<IntegrationTestContext, SecurityRules> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, SecurityRules.class);
    }

    static Function<IntegrationTestContext, SecurityRules> getNew() {
        return testContext -> new SecurityRules();
    }

    public static SecurityRules request() {
        return new SecurityRules();
    }

    public static Action<SecurityRules> getDefaultSecurityRules(String key) {
        return new Action<>(getTestContext(key), SecurityRulesAction::getDefaultSecurityRules);
    }

    public static Action<SecurityRules> getDefaultSecurityRules() {
        return getDefaultSecurityRules(SECURITYRULES);
    }

    public static Assertion<SecurityRules> assertThis(BiConsumer<SecurityRules, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
