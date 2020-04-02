package com.sequenceiq.environment.telemetry.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.environment.telemetry.domain.AccountTelemetry;

@EntityType(entityClass = AccountTelemetry.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface AccountTelemetryRepository extends JpaRepository<AccountTelemetry, Long> {

    @Query("SELECT a FROM AccountTelemetry a WHERE a.accountId = :accountId")
    Optional<AccountTelemetry> findByAccountId(@Param("accountId") String accountId);

    @Modifying
    @Query("UPDATE AccountTelemetry a SET a.archived = TRUE WHERE a.accountId= :accountId")
    void archiveAll(@Param("accountId") String accountId);

}
