package com.sequenceiq.freeipa.kerberos;

import java.util.List;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

@Transactional(TxType.REQUIRED)
public interface KerberosConfigWithArchivedRepository extends JpaRepository<KerberosConfigWithArchived, Long> {

    List<KerberosConfigWithArchived> findAllByAccountIdAndEnvironmentCrn(String accountId, String environmentCrn);
}
