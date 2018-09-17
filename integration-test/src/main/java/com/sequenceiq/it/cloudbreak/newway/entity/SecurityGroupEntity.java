package com.sequenceiq.it.cloudbreak.newway.entity;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sequenceiq.cloudbreak.api.model.SecurityGroupResponse;
import com.sequenceiq.cloudbreak.api.model.v2.SecurityGroupV2Request;
import com.sequenceiq.it.cloudbreak.newway.AbstractCloudbreakEntity;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.SecurityRules;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class SecurityGroupEntity extends AbstractCloudbreakEntity<SecurityGroupV2Request, SecurityGroupResponse, SecurityGroupEntity> {

    protected SecurityGroupEntity(SecurityGroupV2Request request, TestContext testContext) {
        super(request, testContext);
    }

    protected SecurityGroupEntity(TestContext testContext) {
        super(new SecurityGroupV2Request(), testContext);
    }

    public SecurityGroupEntity valid() {
        return withSecurityRules(Collections.singletonList(getTestContext().init(SecurityRules.class)));
    }

    public SecurityGroupEntity withSecurityRules(List<SecurityRules> securityRules) {
        getRequest().setSecurityRules(securityRules.stream().map(SecurityRules::getRequest).collect(Collectors.toList()));
        return this;
    }

    public SecurityGroupEntity withSecurityGroupIds(List<String> securityGroupIds) {
        return this;
    }
}
