package com.sequenceiq.freeipa.ldap;

import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

@Transactional(TxType.REQUIRED)
public interface LdapConfigRepository extends JpaRepository<LdapConfig, Long> {
    Optional<LdapConfig> findByAccountIdAndEnvironmentId(String accountId, String environmentId);
}
