package com.sequenceiq.cloudbreak.repository.organization;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.organization.Organization;
import com.sequenceiq.cloudbreak.domain.organization.User;
import com.sequenceiq.cloudbreak.domain.organization.UserOrgPermissions;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = UserOrgPermissions.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface UserOrgPermissionsRepository extends DisabledBaseRepository<UserOrgPermissions, Long> {

    @Query("SELECT o FROM UserOrgPermissions o WHERE o.user = :user")
    Set<UserOrgPermissions> findForUser(@Param("user") User user);

    @Query("SELECT o FROM UserOrgPermissions o WHERE o.organization = :organization")
    Set<UserOrgPermissions> findForOrganization(@Param("organization") Organization organization);

    @Query("SELECT o FROM UserOrgPermissions o WHERE o.user = :user AND o.organization = :organization")
    UserOrgPermissions findForUserAndOrganization(@Param("user") User user, @Param("organization") Organization organization);

    @Query("SELECT o FROM UserOrgPermissions o WHERE o.user = :user AND o.organization.id = :orgId")
    UserOrgPermissions findForUserByOrganizationId(@Param("user") User user, @Param("orgId") Long orgId);

    @Query("SELECT o FROM UserOrgPermissions o WHERE o.user = :user AND o.organization.id in :orgIds")
    Set<UserOrgPermissions> findForUserByOrganizationIds(@Param("user") User user, @Param("orgIds") Set<Long> orgIds);

    Long deleteByOrganization(Organization organization);
}
