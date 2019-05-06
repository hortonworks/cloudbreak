package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.freeipa.entity.SecurityConfig;

@EntityType(entityClass = SecurityConfig.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface SecurityConfigRepository extends DisabledBaseRepository<SecurityConfig, Long> {

    @Query("SELECT sc from SecurityConfig sc, Stack s where s.id = :stackId and s.securityConfig.id = sc.id")
    SecurityConfig findOneByStackId(@Param("stackId")Long stackId);

}
