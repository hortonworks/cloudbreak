package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.SecurityRule;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = SecurityRule.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface SecurityRuleRepository extends DisabledBaseRepository<SecurityRule, Long> {

    @Query("SELECT r FROM SecurityRule r WHERE r.securityGroup.id= :securityGroupId")
    List<SecurityRule> findAllBySecurityGroupId(@Param("securityGroupId") Long securityGroupId);

}
