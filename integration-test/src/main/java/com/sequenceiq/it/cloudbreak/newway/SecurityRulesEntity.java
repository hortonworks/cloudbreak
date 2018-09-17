package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityRulesResponse;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

public class SecurityRulesEntity extends AbstractCloudbreakEntity<SecurityRuleRequest, SecurityRulesResponse, SecurityRules> {
    public static final String SECURITYRULES = "SECURITYRULES";

    private SecurityRulesEntity(String newId) {
        super(newId);
        setRequest(new SecurityRuleRequest());
    }

    SecurityRulesEntity() {
        this(SECURITYRULES);
    }

    public SecurityRulesEntity(TestContext testContext) {
        super(new SecurityRuleRequest(), testContext);
    }
}
