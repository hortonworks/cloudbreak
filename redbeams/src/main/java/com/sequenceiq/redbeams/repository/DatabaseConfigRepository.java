package com.sequenceiq.redbeams.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.domain.DatabaseConfig;

@EntityType(entityClass = DatabaseConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface DatabaseConfigRepository extends DisabledBaseRepository<DatabaseConfig, Long> {

}
