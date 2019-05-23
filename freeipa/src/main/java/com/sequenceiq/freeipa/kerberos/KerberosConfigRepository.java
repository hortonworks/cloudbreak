package com.sequenceiq.freeipa.kerberos;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

@Transactional(TxType.REQUIRED)
public interface KerberosConfigRepository extends JpaRepository<KerberosConfig, Long> {
    Optional<KerberosConfig> findByAccountIdAndEnvironmentId(String accountId, String environmentId);
}
