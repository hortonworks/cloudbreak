package com.sequenceiq.periscope.repository;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.repository.BaseRepository;
import com.sequenceiq.cloudbreak.service.EntityType;
import com.sequenceiq.periscope.domain.PeriscopeUser;

@DisableHasPermission
@EntityType(entityClass = PeriscopeUser.class)
public interface UserRepository extends BaseRepository<PeriscopeUser, String> {

    PeriscopeUser findByEmail(String email);

}
