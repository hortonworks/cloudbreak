package com.sequenceiq.cloudbreak.repository;

import static com.sequenceiq.cloudbreak.authorization.OrganizationPermissions.Action.READ;

import java.util.Collection;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.organization.CheckPermissionsByOrganizationId;
import com.sequenceiq.cloudbreak.aspect.organization.OrganizationResourceType;
import com.sequenceiq.cloudbreak.domain.Recipe;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.cloudbreak.authorization.OrganizationResource;

@DisableHasPermission
@EntityType(entityClass = Recipe.class)
@Transactional(TxType.REQUIRED)
@OrganizationResourceType(resource = OrganizationResource.RECIPE)
public interface RecipeRepository extends OrganizationResourceRepository<Recipe, Long> {

    @CheckPermissionsByOrganizationId(action = READ, organizationIdIndex = 1)
    @Query("SELECT r FROM Recipe r WHERE r. name in :names AND r.organization.id = :orgId")
    Set<Recipe> findByNamesInOrganization(@Param("names") Collection<String> names, @Param("orgId") Long orgId);

    @CheckPermissionsByOrganizationId(action = READ)
    @Query("SELECT r FROM Recipe r WHERE r.organization.id = :orgId")
    Set<Recipe> listByOrganizationId(@Param("orgId") Long orgId);
}
