package com.sequenceiq.cloudbreak.repository;

import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.aspect.DisableHasPermission;
import com.sequenceiq.cloudbreak.aspect.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.service.EntityType;

@EntityType(entityClass = Resource.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface ResourceRepository extends DisabledBaseRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    Resource findByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name, @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND (r.resourceName = :resource OR r.resourceReference = :resource)")
    Resource findByStackIdAndResourceNameOrReference(@Param("stackId") Long stackId, @Param("resource") String resource);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId")
    List<Resource> findAllByStackId(@Param("stackId") long stackId);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND r.instanceId = :instanceId AND r.resourceType = :type")
    Resource findByStackIdAndInstanceIdAndType(@Param("stackId") long stackId, @Param("instanceId") String instanceId,
            @Param("type") ResourceType type);
}
