package com.sequenceiq.freeipa.repository;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.common.model.SeLinux;
import com.sequenceiq.freeipa.entity.SecurityConfig;

@Transactional(TxType.REQUIRED)
@EntityType(entityClass = SecurityConfig.class)
public interface SecurityConfigRepository extends CrudRepository<SecurityConfig, Long> {

    @Query("SELECT sc from SecurityConfig sc, Stack s where s.id = :stackId and s.securityConfig.id = sc.id")
    SecurityConfig findOneByStackId(@Param("stackId") Long stackId);

    @Modifying
    @Query("UPDATE SecurityConfig sc SET sc.seLinux = :selinuxMode where sc.id = :id")
    int updateSeLinuxSecurityConfig(@Param("selinuxMode") SeLinux selinuxMode, @Param("id") Long id);
}
