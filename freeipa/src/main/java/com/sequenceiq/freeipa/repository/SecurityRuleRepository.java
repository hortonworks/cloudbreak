package com.sequenceiq.freeipa.repository;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.repository.CrudRepository;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.ResourceType;
import com.sequenceiq.freeipa.entity.SecurityGroup;
import com.sequenceiq.freeipa.entity.SecurityRule;

@Transactional(TxType.REQUIRED)
@AuthorizationResource(type = ResourceType.ENVIRONMENT)
public interface SecurityRuleRepository extends CrudRepository<SecurityRule, Long> {

    List<SecurityRule> findAllBySecurityGroup(SecurityGroup securityGroup);

}
