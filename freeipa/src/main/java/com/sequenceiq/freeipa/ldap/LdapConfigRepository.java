package com.sequenceiq.freeipa.ldap;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

@Transactional(TxType.REQUIRED)
public interface LdapConfigRepository extends JpaRepository<LdapConfig, Long> {

    Optional<LdapConfig> findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(String accountId, String environmentCrn);

    List<LdapConfig> findByAccountIdAndEnvironmentCrnAndArchivedIsFalse(String accountId, String environmentCrn);

    List<LdapConfig> findByAccountIdAndEnvironmentCrn(String accountId, String environmentCrn);

    Optional<LdapConfig> findByAccountIdAndEnvironmentCrnAndClusterNameAndArchivedIsFalse(String accountId, String environmentCrn, String clusterName);
}
