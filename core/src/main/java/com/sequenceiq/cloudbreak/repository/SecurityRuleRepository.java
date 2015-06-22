package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.SecurityRule;

public interface SecurityRuleRepository extends CrudRepository<SecurityRule, Long> {

    List<SecurityRule> findAllBySecurityGroupId(@Param("securityGroupId") Long securityGroupId);

}
