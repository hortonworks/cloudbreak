package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.common.api.type.ResourceType;

@EntityType(entityClass = Resource.class)
@Transactional(TxType.REQUIRED)
@DisableHasPermission
public interface ResourceRepository extends DisabledBaseRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    Optional<Resource> findByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name, @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId")
    List<Resource> findAllByStackId(@Param("stackId") long stackId);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND (r.resourceType NOT LIKE '%INSTANCE%' OR r.resourceType NOT LIKE '%DISK%')")
    Set<Resource> findAllByStackIdNotInstanceOrDisk(@Param("stackId") Long stackId);

}
