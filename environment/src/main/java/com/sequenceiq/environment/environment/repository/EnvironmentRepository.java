package com.sequenceiq.environment.environment.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.sequenceiq.environment.environment.domain.Environment;

@Transactional(TxType.REQUIRED)
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    Set<Environment> findByAccountId(String accountId);

    Set<Environment> findByNameInAndAccountId(Set<String> names, String accountId);

    @Query("SELECT e FROM Environment e WHERE e.accountId = :accountId AND (e.name = :name OR e.resourceCrn = :name)")
    Optional<Environment> findByNameAndAccountId(String name, String accountId);

    @Query("SELECT COUNT(e)>0 FROM Environment e WHERE e.name = :name AND e.accountId = :accountId")
    boolean existsWithNameInAccount(String name, String accountId);
}
