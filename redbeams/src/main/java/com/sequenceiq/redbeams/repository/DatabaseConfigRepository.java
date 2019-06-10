package com.sequenceiq.redbeams.repository;

import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.redbeams.authorization.CheckPermissionsByReturnValue;
import com.sequenceiq.redbeams.authorization.ResourceAction;
import com.sequenceiq.redbeams.domain.DatabaseConfig;

@EntityType(entityClass = DatabaseConfig.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface DatabaseConfigRepository extends JpaRepository<DatabaseConfig, Long> {

    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    @Query("SELECT d FROM DatabaseConfig d WHERE d.environmentId = :environmentId "
            + "AND (d.name = :name OR d.resourceCrn = :name)")
    Optional<DatabaseConfig> findByEnvironmentIdAndName(@Param("environmentId") String environmentId, @Param("name") String name);

    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    Set<DatabaseConfig> findByEnvironmentId(String environmentId);

    @CheckPermissionsByReturnValue(action = ResourceAction.READ)
    @Query("SELECT d FROM DatabaseConfig d WHERE d.environmentId = :environmentId "
            + "AND (d.name IN :names OR d.resourceCrn IN :names)")
    Set<DatabaseConfig> findAllByEnvironmentIdAndNameIn(@Param("environmentId") String environmentId, @Param("names") Set<String> names);
}
