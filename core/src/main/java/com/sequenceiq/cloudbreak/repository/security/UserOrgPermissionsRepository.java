package com.sequenceiq.cloudbreak.repository.security;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisablePermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.security.Organization;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.domain.security.UserOrgPermissions;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = UserOrgPermissions.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisablePermission
public interface UserOrgPermissionsRepository extends DisabledBaseRepository<UserOrgPermissions, Long> {

    @Query("SELECT o FROM UserOrgPermissions o WHERE o.user = :user")
    Set<UserOrgPermissions> findForUser(@Param("user") User user);

    @Query("SELECT o FROM UserOrgPermissions o WHERE o.organization = :organization")
    Set<UserOrgPermissions> findForOrganization(@Param("organization") Organization organization);

    @Query("SELECT o FROM UserOrgPermissions o WHERE o.user = :user AND o.organization = :organization")
    Set<UserOrgPermissions> findForUserAndOrganization(@Param("user") User user, @Param("organization") Organization organization);
}
