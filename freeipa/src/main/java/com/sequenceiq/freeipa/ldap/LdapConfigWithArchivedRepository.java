package com.sequenceiq.freeipa.ldap;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

@Transactional(TxType.REQUIRED)
public interface LdapConfigWithArchivedRepository extends JpaRepository<LdapConfigWithArchived, Long> {

    List<LdapConfigWithArchived> findByAccountIdAndEnvironmentCrn(String accountId, String environmentCrn);
}
