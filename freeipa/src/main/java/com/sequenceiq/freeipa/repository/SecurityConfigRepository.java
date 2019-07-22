package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.freeipa.entity.SecurityConfig;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface SecurityConfigRepository extends BaseCrudRepository<SecurityConfig, Long> {

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT sc from SecurityConfig sc, Stack s where s.id = :stackId and s.securityConfig.id = sc.id")
    SecurityConfig findOneByStackId(@Param("stackId") Long stackId);

}
