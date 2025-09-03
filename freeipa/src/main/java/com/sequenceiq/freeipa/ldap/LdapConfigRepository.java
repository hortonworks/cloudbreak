package com.sequenceiq.freeipa.ldap;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;

@Transactional(TxType.REQUIRED)
public interface LdapConfigRepository extends JpaRepository<LdapConfig, Long>, VaultRotationAwareRepository {

    Optional<LdapConfig> findByAccountIdAndEnvironmentCrnAndClusterNameIsNullAndArchivedIsFalse(String accountId, String environmentCrn);

    List<LdapConfig> findByAccountIdAndEnvironmentCrnAndArchivedIsFalse(String accountId, String environmentCrn);

    List<LdapConfig> findByAccountIdAndEnvironmentCrn(String accountId, String environmentCrn);

    Optional<LdapConfig> findByAccountIdAndEnvironmentCrnAndClusterNameAndArchivedIsFalse(String accountId, String environmentCrn, String clusterName);

    @Override
    default Class<LdapConfig> getEntityClass() {
        return LdapConfig.class;
    }
}
