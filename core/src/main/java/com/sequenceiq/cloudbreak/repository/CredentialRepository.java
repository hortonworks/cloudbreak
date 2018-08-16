package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.READ;

import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganizationId;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Credential;
import com.sequenceiq.cloudbreak.domain.Topology;
import com.sequenceiq.cloudbreak.service.EntityType;

@DisableHasPermission
@EntityType(entityClass = Credential.class)
@Transactional(TxType.REQUIRED)
@OrganizationResourceType(resource = OrganizationResource.CREDENTIAL)
public interface CredentialRepository extends OrganizationResourceRepository<Credential, Long> {

    @CheckPermissionsByReturnValue
    Set<Credential> findAllByCloudPlatform(@Param("cloudPlatform") String cloudPlatform);

    @CheckPermissionsByOrganizationId(action = READ)
    @Query("SELECT c FROM Credential c WHERE c.organization.id= :orgId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Set<Credential> findActiveForOrganizationFilterByPlatforms(@Param("orgId") Long orgId, @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @CheckPermissionsByOrganizationId(action = READ, organizationIdIndex = 1)
    @Query("SELECT c FROM Credential c WHERE c.name= :name AND c.organization.id= :orgId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Credential findActiveByNameAndOrgIdFilterByPlatforms(@Param("name") String name, @Param("orgId") Long orgId,
                    @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @CheckPermissionsByOrganizationId(action = READ, organizationIdIndex = 1)
    @Query("SELECT c FROM Credential c WHERE c.id= :id AND c.organization.id= :orgId AND c.archived IS FALSE AND cloudPlatform IN (:cloudPlatforms)")
    Credential findActiveByIdAndOrganizationFilterByPlatforms(@Param("id") Long id, @Param("orgId") Long orgId,
                    @Param("cloudPlatforms") Collection<String> cloudPlatforms);

    @CheckPermissionsByReturnValue
    Set<Credential> findByTopology(Topology topology);
}