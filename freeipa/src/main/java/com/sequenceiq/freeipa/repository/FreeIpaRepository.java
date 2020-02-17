package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.freeipa.entity.FreeIpa;
import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface FreeIpaRepository extends BaseCrudRepository<FreeIpa, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<FreeIpa> getByStack(Stack stack);

    @Query("SELECT f FROM FreeIpa f WHERE f.stack.id = :stackId")
    @CheckPermission(action = ResourceAction.READ)
    Optional<FreeIpa> getByStackId(@Param("stackId") Long stackId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT f FROM FreeIpa f LEFT JOIN FETCH f.stack s WHERE s.accountId = :accountId AND s.terminated = -1")
    List<FreeIpa> findByAccountId(@Param("accountId") String accountId);
}
