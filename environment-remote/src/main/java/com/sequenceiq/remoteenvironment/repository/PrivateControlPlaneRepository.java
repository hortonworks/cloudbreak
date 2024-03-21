package com.sequenceiq.remoteenvironment.repository;


import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.dal.ResourceBasicView;
import com.sequenceiq.cloudbreak.common.dal.repository.AccountAwareResourceRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.remoteenvironment.domain.PrivateControlPlane;

@Transactional(Transactional.TxType.REQUIRED)
@EntityType(entityClass = PrivateControlPlane.class)
public interface PrivateControlPlaneRepository extends AccountAwareResourceRepository<PrivateControlPlane, Long> {

    @Modifying
    @Query("DELETE FROM PrivateControlPlane " +
            "WHERE resourceCrn IN (:crns)")
    void deleteByResourceCrns(@Param("crns") Set<String> crns);

    @Query("SELECT e.resourceCrn FROM PrivateControlPlane e " +
            "WHERE e.name in (:names) " +
            "AND e.accountId = :accountId")
    List<String> findAllCrnByNameInAndAccountId(@Param("names") Collection<String> names, @Param("accountId") String accountId);

    @Query("SELECT e.id FROM PrivateControlPlane e " +
            "WHERE e.accountId = :accountId " +
            "AND e.resourceCrn = :resourceCrn")
    Optional<Long> findIdByResourceCrnAndAccountId(@Param("resourceCrn") String resourceCrn, @Param("accountId") String accountId);

    @Query("SELECT e.id FROM PrivateControlPlane e " +
            "WHERE e.accountId = :accountId " +
            "AND e.name = :name")
    Optional<Long> findIdByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT e.resourceCrn FROM PrivateControlPlane e " +
            "WHERE e.name = :name " +
            "AND e.accountId = :accountId")
    Optional<String> findResourceCrnByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name FROM PrivateControlPlane e " +
            "WHERE e.name in (:names) " +
            "AND e.accountId = :accountId")
    List<ResourceBasicView> findAllResourceBasicViewByNamesAndAccountId(@Param("names") Collection<String> names, @Param("accountId") String accountId);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name FROM PrivateControlPlane e " +
            "WHERE e.resourceCrn = :resourceCrn")
    Optional<ResourceBasicView> findResourceBasicViewByResourceCrn(@Param("resourceCrn") String resourceCrn);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name FROM PrivateControlPlane e " +
            "WHERE e.resourceCrn in (:resourceCrns)")
    List<ResourceBasicView> findAllResourceBasicViewByResourceCrns(@Param("resourceCrns") Collection<String> resourceCrns);

    @Query("SELECT e.resourceCrn as resourceCrn, e.id as id, e.name as name FROM PrivateControlPlane e " +
            "WHERE e.name = :name " +
            "AND e.accountId = :accountId")
    Optional<ResourceBasicView> findResourceBasicViewByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);
}
