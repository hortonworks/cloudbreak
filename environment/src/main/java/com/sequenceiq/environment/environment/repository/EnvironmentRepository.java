package com.sequenceiq.environment.environment.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sequenceiq.environment.environment.domain.Environment;

@Transactional(TxType.REQUIRED)
public interface EnvironmentRepository extends JpaRepository<Environment, Long> {

    Set<Environment> findAllByNameInAndAccountId(Set<String> names, String accountId);

    Set<Environment> findByNameInAndAccountId(Set<String> names, String accountId);

    Optional<Environment> findByNameAndAccountId(String name, String accountId);
}
