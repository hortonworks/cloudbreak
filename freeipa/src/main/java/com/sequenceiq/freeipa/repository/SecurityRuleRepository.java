package com.sequenceiq.freeipa.repository;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.SecurityRule;

@EntityType(entityClass = SecurityRule.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface SecurityRuleRepository extends DisabledBaseRepository<SecurityRule, Long> {

    List<SecurityRule> findAllBySecurityGroup(SecurityGroup securityGroup);

}
