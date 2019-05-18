package com.sequenceiq.environment.proxy;

import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.environment.environment.repository.EnvironmentResourceRepository;

@Transactional(TxType.REQUIRED)
interface ProxyConfigRepository extends EnvironmentResourceRepository<ProxyConfig, Long> {

    @Query("SELECT p FROM ProxyConfig p WHERE p.accountId = :accountId")
    Set<ProxyConfig> findAllByWorkspaceId(@Param("accountId") String accountId);
}
