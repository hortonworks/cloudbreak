package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.SecurityConfig;

@Transactional(TxType.REQUIRED)
public interface SecurityConfigRepository extends CrudRepository<SecurityConfig, Long> {

    @Query("SELECT sc from SecurityConfig sc, Stack s where s.id = :stackId and s.securityConfig.id = sc.id")
    SecurityConfig findOneByStackId(@Param("stackId") Long stackId);

}
