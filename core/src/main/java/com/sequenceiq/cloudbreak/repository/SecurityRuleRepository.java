package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisablePermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = SecurityRule.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisablePermission
public interface SecurityRuleRepository extends DisabledBaseRepository<SecurityRule, Long> {

    @Query("SELECT r FROM SecurityRule r WHERE r.securityGroup.id= :securityGroupId")
    List<SecurityRule> findAllBySecurityGroupId(@Param("securityGroupId") Long securityGroupId);

}
