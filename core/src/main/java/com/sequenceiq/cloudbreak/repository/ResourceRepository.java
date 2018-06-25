package com.sequenceiq.cloudbreak.repository;

import javax.transaction.Transactional;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.common.type.ResourceType;
import com.sequenceiq.cloudbreak.domain.Resource;

@EntityType(entityClass = Resource.class)
@Transactional(Transactional.TxType.REQUIRED)
public interface ResourceRepository extends CrudRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    Resource findByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name, @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND (r.resourceName = :resource OR r.resourceReference = :resource)")
    Resource findByStackIdAndResourceNameOrReference(@Param("stackId") Long stackId, @Param("resource") String resource);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId")
    List<Resource> findAllByStackId(@Param("stackId") long stackId);
}
