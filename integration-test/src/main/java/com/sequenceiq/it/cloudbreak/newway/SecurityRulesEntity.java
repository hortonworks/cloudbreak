package com.sequenceiq.it.cloudbreak.newway;

import com.sequenceiq.cloudbreak.api.model.SecurityRuleRequest;
import com.sequenceiq.cloudbreak.api.model.SecurityRulesResponse;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
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

    public SecurityRulesEntity valid() {
        return withSubnet("0.0.0.0/0")
                .withProtocol("tcp")
                .withPorts("22,443,8443,9443,8080");
    }

    public SecurityRulesEntity withSubnet(String subnet) {
        getRequest().setSubnet(subnet);
        return this;
    }

    public SecurityRulesEntity withProtocol(String protocol) {
        getRequest().setProtocol(protocol);
        return this;
    }

    public SecurityRulesEntity withPorts(String ports) {
        getRequest().setPorts(ports);
        return this;
    }
}
