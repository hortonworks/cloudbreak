package com.sequenceiq.environment.parameters.dao.repository;

import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.environment.parameters.dao.domain.BaseParameters;

@Transactional(TxType.REQUIRED)
public interface BaseParametersRepository<T extends BaseParameters> extends JpaRepository<T, Long> {

    Optional<BaseParameters> findByEnvironmentId(Long envId);

    @Query("SELECT COUNT(ep.id) > 0 "
            + "FROM BaseParameters ep "
            + "JOIN Environment e ON e.id = ep.environment.id "
            + "WHERE e.accountId = :accountId "
            + "AND e.cloudPlatform = :cloudPlatform "
            + "AND e.status IN :activeStatuses "
            + "AND e.location = :location "
            + "AND ep.s3guardTableName = :dynamoTableName")
    boolean isS3GuardTableUsed(@Param("accountId") String accountId, @Param("cloudPlatform") String cloudPlatform,
            @Param("activeStatuses") Set<String> activeStatuses, @Param("location") String location, @Param("dynamoTableName") String dynamoTableName);
}
