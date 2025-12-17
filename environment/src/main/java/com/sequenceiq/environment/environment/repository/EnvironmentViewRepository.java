package com.sequenceiq.environment.environment.repository;


import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.environment.environment.EnvironmentDeletionType;
import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.EnvironmentView;
import com.sequenceiq.environment.environment.dto.CompactViewDto;

@Transactional(TxType.REQUIRED)
public interface EnvironmentViewRepository extends JpaRepository<EnvironmentView, Long> {

    @Query("SELECT ev FROM EnvironmentView ev "
            + "LEFT JOIN FETCH ev.credential "
            + "WHERE ev.accountId= :accountId "
            + "AND ev.archived = false")
    Set<EnvironmentView> findAllByAccountId(@Param("accountId") String accountId);

    @Query("SELECT ev FROM EnvironmentView ev "
            + "LEFT JOIN FETCH ev.credential "
            + "LEFT JOIN FETCH ev.network "
            + "LEFT JOIN FETCH ev.authentication "
            + "LEFT JOIN FETCH ev.proxyConfig "
            + "WHERE ev.accountId= :accountId "
            + "AND ev.name in :names "
            + "AND ev.archived = false")
    Set<EnvironmentView> findAllByNameInAndAccountIdAndArchivedIsFalse(@Param("names") Collection<String> names, @Param("accountId") String accountId);

    @Query("SELECT ev FROM EnvironmentView ev "
            + "LEFT JOIN FETCH ev.credential "
            + "LEFT JOIN FETCH ev.network "
            + "LEFT JOIN FETCH ev.authentication "
            + "LEFT JOIN FETCH ev.proxyConfig "
            + "WHERE ev.accountId= :accountId "
            + "AND ev.resourceCrn in :resourceCrns "
            + "AND ev.archived = false")
    Set<EnvironmentView> findAllByResourceCrnInAndAccountIdAndArchivedIsFalse(@Param("resourceCrns") Collection<String> resourceCrns,
            @Param("accountId") String accountId);

    @Query("SELECT ev FROM EnvironmentView ev "
            + "LEFT JOIN FETCH ev.credential "
            + "LEFT JOIN FETCH ev.network "
            + "LEFT JOIN FETCH ev.authentication "
            + "LEFT JOIN FETCH ev.proxyConfig "
            + "WHERE ev.accountId= :accountId "
            + "AND ev.resourceCrn = :resourceCrn "
            + "AND ev.archived = false")
    Optional<EnvironmentView> findByResourceCrnAndAccountIdAndArchivedIsFalse(@Param("resourceCrn") String resourceCrn, @Param("accountId") String accountId);

    @Query("SELECT new com.sequenceiq.environment.environment.dto.CompactViewDto(ev.id, ev.name) FROM EnvironmentView ev "
            + "WHERE ev.resourceCrn = :resourceCrn "
            + "AND ev.archived = false")
    Optional<CompactViewDto> findCompactViewByResourceCrnAndArchivedIsFalse(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT ev FROM EnvironmentView ev "
            + "LEFT JOIN FETCH ev.credential "
            + "LEFT JOIN FETCH ev.network "
            + "LEFT JOIN FETCH ev.authentication "
            + "LEFT JOIN FETCH ev.proxyConfig "
            + "WHERE ev.accountId= :accountId "
            + "AND ev.name = :name "
            + "AND ev.archived = false")
    Optional<EnvironmentView> findByNameAndAccountIdAndArchivedIsFalse(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT ev FROM EnvironmentView ev "
            + "LEFT JOIN FETCH ev.credential "
            + "LEFT JOIN FETCH ev.network "
            + "LEFT JOIN FETCH ev.authentication "
            + "LEFT JOIN FETCH ev.proxyConfig "
            + "WHERE ev.id = :id "
            + "AND ev.archived = false")
    Optional<EnvironmentView> findByIdAndArchivedIsFalse(@Param("id") Long id);

    Set<EnvironmentView> findAllByCredentialIdAndArchivedIsFalse(Long credentialId);

    Set<EnvironmentView> findAllByProxyConfigIdAndArchivedIsFalse(Long proxyConfigId);

    Long getIdByNameAndAccountIdAndArchivedIsFalse(String name, String accountId);

    Long getIdByResourceCrnAndAccountIdAndArchivedIsFalse(String resourceCrn, String accountId);

    @Modifying
    @Query("UPDATE EnvironmentView ev SET ev.deletionType = :deletionType WHERE ev.id = :id")
    int updateDeletionTypeById(@Param("id") Long id, @Param("deletionType") EnvironmentDeletionType deletionType);

    @Modifying
    @Query("UPDATE EnvironmentView ev SET ev.status = :status, ev.statusReason = :statusReason WHERE ev.id = :id")
    int updateStatusAndStatusReasonById(@Param("id") Long id, @Param("status") EnvironmentStatus status, @Param("statusReason") String statusReason);

    @Query("SELECT e.name FROM Environment e "
            + "JOIN e.parentEnvironment pe "
            + "WHERE pe.id = :parentEnvironmentId AND e.accountId = :accountId AND e.archived = false")
    List<String> findNameWithAccountIdAndParentEnvIdAndArchivedIsFalse(@Param("accountId") String accountId,
            @Param("parentEnvironmentId") Long parentEnvironmentId);

    @Query("SELECT e.resourceCrn from Environment e WHERE e.archived = false and e.cloudPlatform = :cloudPlatform")
    List<String> findAllResourceCrnByArchivedIsFalseAndCloudPlatform(String cloudPlatform);

}
