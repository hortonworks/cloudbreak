package com.sequenceiq.cloudbreak.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.SecurityConfig;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.common.model.SeLinux;

@EntityType(entityClass = SecurityConfig.class)
@Transactional(TxType.REQUIRED)
public interface SecurityConfigRepository extends CrudRepository<SecurityConfig, Long> {

    Optional<SecurityConfig> findOneByStackId(Long stackId);

    @Modifying
    @Query("UPDATE SecurityConfig sc SET sc.seLinux = :selinuxMode where sc.id = :id")
    int updateSeLinuxSecurityConfig(@Param("selinuxMode") SeLinux selinuxMode, @Param("id") Long id);
}
