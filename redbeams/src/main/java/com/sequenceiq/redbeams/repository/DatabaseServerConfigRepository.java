package com.sequenceiq.redbeams.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseJpaRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.cloudbreak.auth.altus.Crn;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseServerConfig;

@EntityType(entityClass = DatabaseServerConfig.class)
@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.DATALAKE)
public interface DatabaseServerConfigRepository extends BaseJpaRepository<DatabaseServerConfig, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Set<DatabaseServerConfig> findByWorkspaceIdAndEnvironmentId(Long workspaceId, String environmentId);

    @CheckPermission(action = ResourceAction.READ)
    Optional<DatabaseServerConfig> findByResourceCrn(Crn crn);

    @CheckPermission(action = ResourceAction.READ)
    Optional<DatabaseServerConfig> findByNameAndWorkspaceIdAndEnvironmentId(String name, Long workspaceId, String environmentId);

    @CheckPermission(action = ResourceAction.READ)
    Set<DatabaseServerConfig> findByResourceCrnIn(Set<Crn> resourceCrns);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT s FROM DatabaseServerConfig s WHERE s.workspaceId = :workspaceId AND s.environmentId = :environmentId "
            + "AND (s.name IN :names OR s.resourceCrn IN :names)")
    Set<DatabaseServerConfig> findByNameInAndWorkspaceIdAndEnvironmentId(
            @Param("names") Set<String> names,
            @Param("workspaceId") Long workspaceId,
            @Param("environmentId") String environmentId);

    // save does not require a permission check
}
