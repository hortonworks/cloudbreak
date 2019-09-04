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

    @Query("SELECT e FROM Environment e "
            + "LEFT JOIN FETCH e.network n "
            + "LEFT JOIN FETCH n.environment ev "
            + "LEFT JOIN FETCH e.credential c "
            + "LEFT JOIN FETCH e.authentication a "
            + "LEFT JOIN FETCH e.parameters p "
            + "WHERE e.accountId = :accountId "
            + "AND e.archived = false")
    Set<Environment> findByAccountId(@Param("accountId") String accountId);

    Set<Environment> findByNameInAndAccountIdAndArchivedIsFalse(Collection<String> names, String accountId);

    Set<Environment> findByResourceCrnInAndAccountIdAndArchivedIsFalse(Collection<String> resourceCrns, String accountId);

    Optional<Environment> findByNameAndAccountIdAndArchivedIsFalse(@Param("name") String name, @Param("accountId") String accountId);

    Optional<Environment> findByResourceCrnAndAccountIdAndArchivedIsFalse(@Param("resourceCrn") String resourceCrn, @Param("accountId") String accountId);

    @Query("SELECT COUNT(e)>0 FROM Environment e WHERE e.name = :name AND e.accountId = :accountId AND e.archived = false")
    boolean existsWithNameAndAccountAndArchivedIsFalse(@Param("name") String name, @Param("accountId") String accountId);

    List<Environment> findAllByIdInAndStatusInAndArchivedIsFalse(Collection<Long> ids, Collection<EnvironmentStatus> statuses);

    List<Environment> findAllByStatusInAndArchivedIsFalse(Collection<EnvironmentStatus> statuses);

    @Query("SELECT e.resourceCrn FROM Environment e WHERE e.name = :name AND e.accountId = :accountId")
    Optional<String> findResourceCrnByNameAndAccountId(@Param("name") String name, @Param("accountId") String accountId);
}
