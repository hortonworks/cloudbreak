package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
public interface FreeIpaRepository extends CrudRepository<FreeIpa, Long> {

    Optional<FreeIpa> getByStack(Stack stack);

    @Query("SELECT f FROM FreeIpa f WHERE f.stack.id = :stackId")
    Optional<FreeIpa> getByStackId(@Param("stackId") Long stackId);

    @Query("SELECT f FROM FreeIpa f LEFT JOIN FETCH f.stack s WHERE s.accountId = :accountId AND s.terminated = -1")
    List<FreeIpa> findByAccountId(@Param("accountId") String accountId);
}
