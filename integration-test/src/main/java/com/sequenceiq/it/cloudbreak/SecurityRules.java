package com.sequenceiq.it.cloudbreak;

import java.util.function.BiConsumer;
import java.util.function.Function;

import com.sequenceiq.it.IntegrationTestContext;
import com.sequenceiq.it.cloudbreak.context.TestContext;

public class SecurityRules extends SecurityRulesEntity {

    SecurityRules() {
    }

    public SecurityRules(TestContext testContext) {
        super(testContext);
    }

    static Function<IntegrationTestContext, SecurityRules> getTestContext(String key) {
        return testContext -> testContext.getContextParam(key, SecurityRules.class);
    }

    static Function<IntegrationTestContext, SecurityRules> getNew() {
        return testContext -> new SecurityRules();
    }

    public static SecurityRules request() {
        return new SecurityRules();
    }

    public static ResourceAction<SecurityRules> getDefaultSecurityRules(String key) {
        return new ResourceAction<>(getTestContext(key), SecurityRulesAction::getDefaultSecurityRules);
    }

    public static ResourceAction<SecurityRules> getDefaultSecurityRules() {
        return getDefaultSecurityRules(SECURITYRULES);
    }

    public static Assertion<SecurityRules> assertThis(BiConsumer<SecurityRules, IntegrationTestContext> check) {
        return new Assertion<>(getTestContext(GherkinTest.RESULT), check);
    }
}
