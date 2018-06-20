package com.sequenceiq.periscope.repository;

import com.sequenceiq.cloudbreak.aspect.BaseRepository;
import com.sequenceiq.cloudbreak.aspect.HasPermission;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.periscope.domain.PeriscopeUser;

@HasPermission
@EntityType(entityClass = PeriscopeUser.class)
public interface UserRepository extends BaseRepository<PeriscopeUser, String> {

    PeriscopeUser findByEmail(String email);

}
