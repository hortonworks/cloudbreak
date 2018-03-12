package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityRulesResponse;

public class SecurityRulesEntity extends AbstractCloudbreakEntity<SecurityRuleRequest, SecurityRulesResponse> {
    public static final String SECURITYRULES = "SECURITYRULES";

    private SecurityRulesEntity(String newId) {
        super(newId);
        setRequest(new SecurityRuleRequest());
    }

    SecurityRulesEntity() {
        this(SECURITYRULES);
    }
}
