package com.sequenceiq.cloudbreak.repository.organization;

import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.domain.security.Tenant;
import com.sequenceiq.cloudbreak.domain.security.User;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = User.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface UserRepository extends DisabledBaseRepository<User, Long> {

    User findByUserId(String userId);

    @Query("SELECT u FROM User u WHERE u.tenant= :tenant")
    Set<User> findAllByTenant(@Param("tenant") Tenant tenant);

    Set<User> findByUserIdIn(Set<String> userIds);
}
