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
public interface EnvironmentViewRepository extends JpaRepository<EnvironmentView, Long> {

    @Query("SELECT ev FROM EnvironmentView ev "
            + "LEFT JOIN FETCH ev.credential "
            + "WHERE ev.accountId= :accountId "
            + "AND ev.archived = false")
    Set<EnvironmentView> findAllByAccountId(@Param("accountId") String accountId);

    Set<EnvironmentView> findAllByNameInAndAccountIdAndArchivedIsFalse(Collection<String> names, String accountId);

    Set<EnvironmentView> findAllByResourceCrnInAndAccountIdAndArchivedIsFalse(Collection<String> resourceCrns, String accountId);

    Set<EnvironmentView> findAllByCredentialIdAndArchivedIsFalse(Long credentialId);

    Long getIdByNameAndAccountIdAndArchivedIsFalse(String name, String accountId);

    Long getIdByResourceCrnAndAccountIdAndArchivedIsFalse(String resourceCrn, String accountId);
}
