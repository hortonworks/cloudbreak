package com.sequenceiq.environment.environment.repository;


import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.sequenceiq.environment.environment.domain.EnvironmentView;

@Transactional(TxType.REQUIRED)
public interface  EnvironmentViewRepository extends JpaRepository<EnvironmentView, Long> {

    @Query("SELECT ev FROM EnvironmentView ev LEFT JOIN FETCH ev.credential WHERE ev.accountId= :accountId")
    Set<EnvironmentView> findAllByAccountId(@Param("accountId") String accountId);

    Set<EnvironmentView> findAllByNameInAndAccountId(Collection<String> names, String accountId);

    Set<EnvironmentView> findAllByCredentialId(Long credentialId);

    Long getIdByNameAndAccountId(String name, String accountId);
}
