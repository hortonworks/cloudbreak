package com.sequenceiq.environment.marketplace.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.environment.marketplace.domain.Terms;

@EntityType(entityClass = Terms.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface TermsRepository extends JpaRepository<Terms, Long> {

    Optional<Terms> findByAccountId(@Param("accountId") String accountId);
}
