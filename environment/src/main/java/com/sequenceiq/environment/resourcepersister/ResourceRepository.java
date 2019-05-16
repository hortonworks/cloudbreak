package com.sequenceiq.environment.resourcepersister;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;

@EntityType(entityClass = Resource.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface ResourceRepository extends DisabledBaseRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE r.crn = :crn AND r.resourceName = :name AND r.resourceType = :type")
    Optional<Resource> findByStackIdAndNameAndType(@Param("crn") String crnId, @Param("name") String name, @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.crn = :crn")
    List<Resource> findAllByStackId(@Param("crn") String crn);

}
