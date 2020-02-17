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
import com.sequenceiq.freeipa.entity.ChildEnvironment;
import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface ChildEnvironmentRepository extends BaseCrudRepository<ChildEnvironment, Long> {

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT c.stack FROM ChildEnvironment c WHERE c.environmentCrn = :childEnvironmentCrn AND c.stack.accountId = :accountId")
    Optional<Stack> findParentStackByChildEnvironmentCrn(@Param("childEnvironmentCrn") String childEnvironmentCrn, @Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT c FROM ChildEnvironment c "
            + "WHERE c.environmentCrn = :childEnvironmentCrn AND c.stack.environmentCrn = :parentEnvironmentCrn AND c.stack.accountId = :accountId")
    Optional<ChildEnvironment> findByParentAndChildEnvironmentCrns(
            @Param("parentEnvironmentCrn") String parentEnvironmentCrn,
            @Param("childEnvironmentCrn") String childEnvironmentCrn,
            @Param("accountId") String accountId);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT c FROM ChildEnvironment c WHERE c.stack.id = :stackId AND c.stack.accountId = :accountId")
    List<ChildEnvironment> findByStackId(@Param("stackId") Long stackId, @Param("accountId") String accountId);
}
