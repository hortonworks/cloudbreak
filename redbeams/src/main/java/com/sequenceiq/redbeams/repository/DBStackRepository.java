package com.sequenceiq.redbeams.repository;

import java.util.Optional;

import javax.transaction.Transactional;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.redbeams.domain.stack.DBStack;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.DATALAKE)
public interface DBStackRepository extends BaseJpaRepository<DBStack, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<DBStack> findByNameAndEnvironmentId(String name, String environmentId);

    @CheckPermission(action = ResourceAction.READ)
    Optional<DBStack> findByResourceCrn(Crn crn);

}
