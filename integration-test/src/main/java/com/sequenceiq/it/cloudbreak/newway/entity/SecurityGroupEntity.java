package com.sequenceiq.it.cloudbreak.newway.entity;

import static com.google.common.collect.Sets.newHashSet;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.request.instancegroup.securitygroup.SecurityGroupV4Request;
import com.sequenceiq.cloudbreak.api.endpoint.v4.stacks.response.instancegroup.securitygroup.SecurityGroupV4Response;
import com.sequenceiq.it.cloudbreak.newway.Prototype;
import com.sequenceiq.it.cloudbreak.newway.SecurityRulesEntity;
import com.sequenceiq.it.cloudbreak.newway.context.TestContext;

@Prototype
public class SecurityGroupEntity extends AbstractCloudbreakEntity<SecurityGroupV4Request, SecurityGroupV4Response, SecurityGroupEntity> {

    protected SecurityGroupEntity(SecurityGroupV4Request request, TestContext testContext) {
        super(request, testContext);
    }

    protected SecurityGroupEntity(TestContext testContext) {
        super(new SecurityGroupV4Request(), testContext);
    }

    public SecurityGroupEntity() {
        super(SecurityGroupEntity.class.getSimpleName().toUpperCase());
    }

    public SecurityGroupEntity valid() {
        return withSecurityRules(Collections.singletonList(getTestContext().init(SecurityRulesEntity.class)));
    }

    public SecurityGroupEntity withSecurityRules(String... keys) {
        List<SecurityRulesEntity> securityRulesEntities = Stream.of(keys)
                .map(key -> (SecurityRulesEntity) getTestContext().get(key)).collect(Collectors.toList());
        return withSecurityRules(securityRulesEntities);
    }

    public SecurityGroupEntity withSecurityRules(List<SecurityRulesEntity> securityRules) {
        getRequest().setSecurityRules(securityRules.stream().map(SecurityRulesEntity::getRequest).collect(Collectors.toList()));
        return this;
    }

    public SecurityGroupEntity withSecurityGroupIds(String... securityGroupIds) {
        getRequest().setSecurityGroupIds(newHashSet(securityGroupIds));
        return this;
    }
}
