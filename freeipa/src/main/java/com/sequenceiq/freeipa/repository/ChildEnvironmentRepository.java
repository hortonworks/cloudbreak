package com.sequenceiq.freeipa.repository;

import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.freeipa.entity.ChildEnvironment;
import org.springframework.data.jpa.repository.Query;

import javax.transaction.Transactional;
import java.util.Optional;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface ChildEnvironmentRepository extends BaseCrudRepository<ChildEnvironment, Long> {

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT c.stack.environmentCrn FROM ChildEnvironment c WHERE c.childEnvironmentCrn = :childEnvironmentCrn AND c.stack.accountId = :accountId")
    Optional<String> findParentByChildEnvironmentCrn(String childEnvironmentCrn, String accountId);
}
