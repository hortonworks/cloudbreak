package com.sequenceiq.environment.environment.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.environment.environment.EnvironmentStatus;
import com.sequenceiq.environment.environment.domain.Environment;

@Transactional(TxType.REQUIRED)
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    @Query("SELECT e FROM Environment e LEFT JOIN FETCH e.network n LEFT JOIN FETCH e.credential c "
            + "LEFT JOIN FETCH e.authentication a WHERE e.accountId = :accountId")
    Set<Environment> findByAccountId(@Param("accountId") String accountId);

    Set<Environment> findByNameInAndAccountId(Set<String> names, String accountId);

    Set<Environment> findByResourceCrnInAndAccountId(Set<String> resourceCrns, String accountId);

    @Query("SELECT e FROM Environment e WHERE e.accountId = :accountId AND e.name = :name")
    Optional<Environment> findByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);

    @Query("SELECT e FROM Environment e WHERE e.accountId = :accountId AND e.resourceCrn = :resourceCrn")
    Optional<Environment> findByResourceCrnAndAccountId(@Param("resourceCrn") String resourceCrn, @Param("accountId") String accountId);

    @Query("SELECT COUNT(e)>0 FROM Environment e WHERE e.name = :name AND e.accountId = :accountId")
    boolean existsWithNameInAccount(@Param("name") String name, @Param("accountId") String accountId);

    List<Environment> findAllByIdInAndStatusIn(Collection<Long> ids, Collection<EnvironmentStatus> statuses);

    List<Environment> findAllByStatusIn(Collection<EnvironmentStatus> environmentStatuses);
}
