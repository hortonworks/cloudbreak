package com.sequenceiq.freeipa.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.freeipa.entity.CrossRealmTrust;

@Transactional(Transactional.TxType.REQUIRED)
public interface CrossRealmTrustRepository extends JpaRepository<CrossRealmTrust, Long> {

    Optional<CrossRealmTrust> findByStackId(Long stackId);
}
