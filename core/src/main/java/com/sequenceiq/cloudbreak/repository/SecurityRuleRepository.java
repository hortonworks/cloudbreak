package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.SecurityRule;

@EntityType(entityClass = SecurityRule.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface SecurityRuleRepository extends CrudRepository<SecurityRule, Long> {

    @Query("SELECT r FROM SecurityRule r WHERE r.securityGroup.id= :securityGroupId")
    List<SecurityRule> findAllBySecurityGroupId(@Param("securityGroupId") Long securityGroupId);

}
