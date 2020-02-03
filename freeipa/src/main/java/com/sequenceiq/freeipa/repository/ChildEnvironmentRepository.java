package com.sequenceiq.freeipa.repository;

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

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface ChildEnvironmentRepository extends BaseCrudRepository<ChildEnvironment, Long> {

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT c.stack.environmentCrn FROM ChildEnvironment c WHERE c.environmentCrn = :childEnvironmentCrn AND c.stack.accountId = :accountId")
    Optional<String> findParentByChildEnvironmentCrn(@Param("childEnvironmentCrn") String childEnvironmentCrn, @Param("accountId") String accountId);
}
