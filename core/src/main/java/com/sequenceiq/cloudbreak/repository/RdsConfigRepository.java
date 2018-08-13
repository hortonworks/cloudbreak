package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.READ;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganization;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganizationId;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import com.sequenceiq.cloudbreak.domain.RDSConfig;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.repository.organization.OrganizationResourceRepository;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = RDSConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
@OrganizationResourceType(resource = OrganizationResource.RDS)
public interface RdsConfigRepository extends OrganizationResourceRepository<RDSConfig, Long> {

    @Override
    @CheckPermissionsByOrganizationId(action = READ)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.organization.id = :orgId")
    Set<RDSConfig> findAllByOrganizationId(@Param("orgId") Long orgId);

    @Override
    @CheckPermissionsByOrganization(action = READ, organizationIndex = 0)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.organization = :org")
    Set<RDSConfig> findAllByOrganization(@Param("org") Organization org);

    @CheckPermissionsByReturnValue(action = READ)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name= :name AND r.status = 'USER_MANAGED'")
    RDSConfig findUserManagedByName(@Param("name") String name);

    @Override
    @CheckPermissionsByOrganizationId(action = READ, organizationIdIndex = 1)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name= :name AND r.status = 'USER_MANAGED'"
            + "AND r.organization.id = :orgId")
    RDSConfig findByNameAndOrganizationId(@Param("name") String name, @Param("orgId") Long orgId);

    @Override
    @CheckPermissionsByOrganization(action = READ, organizationIndex = 1)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.name= :name AND r.status = 'USER_MANAGED'"
            + "AND r.organization = :org")
    RDSConfig findByNameAndOrganization(@Param("name") String name, @Param("org") Organization org);

    @CheckPermissionsByReturnValue(action = READ)
    @Query("SELECT r FROM RDSConfig r LEFT JOIN FETCH r.clusters WHERE r.id= :id AND r.status <> 'DEFAULT_DELETED'")
    Optional<RDSConfig> findById(@Param("id") Long id);

    @CheckPermissionsByReturnValue(action = READ)
    @Query("SELECT r FROM RDSConfig r INNER JOIN r.clusters cluster LEFT JOIN FETCH r.clusters WHERE cluster.id= :clusterId")
    Set<RDSConfig> findByClusterId(@Param("clusterId") Long clusterId);

    @CheckPermissionsByReturnValue(action = READ)
    @Query("SELECT r FROM RDSConfig r INNER JOIN r.clusters cluster LEFT JOIN FETCH r.clusters WHERE cluster.id= :clusterId "
            + "AND r.status = 'USER_MANAGED'")
    Set<RDSConfig> findUserManagedByClusterId(@Param("clusterId") Long clusterId);

    @CheckPermissionsByReturnValue(action = READ)
    @Query("SELECT r FROM RDSConfig r INNER JOIN r.clusters cluster WHERE cluster.id= :clusterId "
            + "AND r.status <> 'DEFAULT_DELETED' AND r.type= :type")
    RDSConfig findByClusterIdAndType(@Param("clusterId") Long clusterId, @Param("type") String type);
}
