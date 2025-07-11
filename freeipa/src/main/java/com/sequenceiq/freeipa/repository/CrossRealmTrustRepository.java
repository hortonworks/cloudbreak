package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.sequenceiq.freeipa.api.v1.freeipa.stack.model.describe.TrustStatus;
import com.sequenceiq.freeipa.entity.CrossRealmTrust;

@Transactional(Transactional.TxType.REQUIRED)
public interface CrossRealmTrustRepository extends JpaRepository<CrossRealmTrust, Long> {
    Optional<CrossRealmTrust> findByStackId(Long stackId);

    @Modifying
    @Query("UPDATE CrossRealmTrust trust SET trust.trustStatus = :trustStatus WHERE trust.stack.id = :stackId")
    int updateTrustStatusByStackId(Long stackId, TrustStatus trustStatus);

    @Modifying
    @Query("UPDATE CrossRealmTrust trust SET trust.operationId = :operationId WHERE trust.stack.id = :stackId")
    int updateOperationIdByStackId(Long stackId, String operationId);
}
