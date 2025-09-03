package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.service.list.ResourceWithId;
import com.sequenceiq.cloudbreak.service.secret.VaultRotationAwareRepository;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;
import com.sequenceiq.freeipa.entity.projection.FreeIpaListView;

@Transactional(Transactional.TxType.REQUIRED)
public interface FreeIpaRepository extends CrudRepository<FreeIpa, Long>, VaultRotationAwareRepository {

    Optional<FreeIpa> getByStack(Stack stack);

    @Query("SELECT f FROM FreeIpa f WHERE f.stack.id = :stackId")
    Optional<FreeIpa> getByStackId(@Param("stackId") Long stackId);

    @Query("SELECT new com.sequenceiq.freeipa.entity.projection.FreeIpaListView(f.domain, s.name, s.resourceCrn, s.environmentCrn, s.stackStatus) "
            + "FROM FreeIpa f LEFT JOIN f.stack s WHERE s.accountId = :accountId AND s.terminated = -1")
    List<FreeIpaListView> findViewByAccountId(@Param("accountId") String accountId);

    @Query("SELECT new com.sequenceiq.freeipa.entity.projection.FreeIpaListView(f.domain, s.name, s.resourceCrn, s.environmentCrn, s.stackStatus) "
            + "FROM FreeIpa f LEFT JOIN f.stack s WHERE f.id IN :ids AND s.terminated = -1")
    List<FreeIpaListView> findAllViewByIds(@Param("ids") List<Long> ids);

    @Query("SELECT new com.sequenceiq.authorization.service.list.ResourceWithId(f.id, f.stack.environmentCrn) " +
            "FROM FreeIpa f WHERE f.stack.accountId = :accountId AND f.stack.terminated = -1")
    List<ResourceWithId> findAllAsAuthorizationResources(@Param("accountId") String accountId);

    @Override
    default Class<FreeIpa> getEntityClass() {
        return FreeIpa.class;
    }
}
