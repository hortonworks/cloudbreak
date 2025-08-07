package com.sequenceiq.environment.terms.repository;

import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.environment.api.v1.terms.model.TermType;
import com.sequenceiq.environment.marketplace.domain.Terms;

@EntityType(entityClass = Terms.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface TermsRepository extends JpaRepository<Terms, Long> {

    @Query("SELECT t FROM Terms t WHERE t.accountId = :accountId AND t.termType = :termType")
    Optional<Terms> findByAccountIdAndTermType(@Param("accountId") String accountId, @Param("termType") TermType termType);
}
