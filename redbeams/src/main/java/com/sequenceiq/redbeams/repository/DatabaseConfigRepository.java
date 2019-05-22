package com.sequenceiq.redbeams.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.cloudbreak.workspace.repository.check.CheckPermissionsByReturnValue;
import com.sequenceiq.cloudbreak.workspace.resource.ResourceAction;
import com.sequenceiq.redbeams.domain.DatabaseConfig;

import java.util.List;

@EntityType(entityClass = DatabaseConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface DatabaseConfigRepository extends DisabledBaseRepository<DatabaseConfig, Long> {

    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    DatabaseConfig findByEnvironmentIdAndName(String environmentId, String name);

    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    List<DatabaseConfig> findByEnvironmentId(String environmentId);
}
