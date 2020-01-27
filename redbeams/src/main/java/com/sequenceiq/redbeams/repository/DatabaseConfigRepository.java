package com.sequenceiq.redbeams.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseConfig;

@EntityType(entityClass = DatabaseConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.DATALAKE)
public interface DatabaseConfigRepository extends BaseJpaRepository<DatabaseConfig, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Optional<DatabaseConfig> findByEnvironmentIdAndName(String environmentId, String name);

    @CheckPermission(action = ResourceAction.READ)
    Optional<DatabaseConfig> findByName(String name);

    @CheckPermission(action = ResourceAction.READ)
    Optional<DatabaseConfig> findByResourceCrn(Crn crn);

    @CheckPermission(action = ResourceAction.READ)
    Set<DatabaseConfig> findByEnvironmentId(String environmentId);

    @CheckPermission(action = ResourceAction.READ)
    Set<DatabaseConfig> findByResourceCrnIn(Set<Crn> resourceCrns);

    // save does not require a permission check
}
