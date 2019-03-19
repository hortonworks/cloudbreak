package com.sequenceiq.it.cloudbreak.newway;

import java.util.Arrays;

import com.sequenceiq.cloudbreak.api.endpoint.v4.util.requests.SecurityRuleV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.util.responses.SecurityRulesV4Response;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;
import com.sequenceiq.it.cloudbreak.newway.dto.AbstractCloudbreakTestDto;

@Prototype
public class SecurityRulesEntity extends AbstractCloudbreakTestDto<SecurityRuleV4Request, SecurityRulesV4Response, SecurityRules> {
    public static final String SECURITYRULES = "SECURITYRULES";

    private SecurityRulesEntity(String newId) {
        super(newId);
    }

    SecurityRulesEntity() {
        this(SECURITYRULES);
    }

    public SecurityRulesEntity(TestContext testContext) {
        super(new SecurityRuleV4Request(), testContext);
    }

    public SecurityRulesEntity valid() {
        return withSubnet("0.0.0.0/0")
                .withProtocol("tcp")
                .withPorts("22", "443", "8443", "9443", "8080");
    }

    public SecurityRulesEntity withSubnet(String subnet) {
        getRequest().setSubnet(subnet);
        return this;
    }

    public SecurityRulesEntity withProtocol(String protocol) {
        getRequest().setProtocol(protocol);
        return this;
    }

    public SecurityRulesEntity withPorts(String... ports) {
        getRequest().setPorts(Arrays.asList(ports));
        return this;
    }
}
