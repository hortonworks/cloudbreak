package com.sequenceiq.cloudbreak.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.cloudbreak.domain.Resource;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;

@EntityType(entityClass = Resource.class)
@Transactional(TxType.REQUIRED)
public interface ResourceRepository extends CrudRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    Optional<Resource> findByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name, @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND r.resourceType = :type")
    List<Resource> findByStackIdAndType(@Param("stackId") Long stackId, @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId")
    List<Resource> findAllByStackId(@Param("stackId") long stackId);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND (r.resourceType NOT LIKE '%INSTANCE%' OR r.resourceType NOT LIKE '%DISK%')")
    Set<Resource> findAllByStackIdNotInstanceOrDisk(@Param("stackId") Long stackId);

    @Query("SELECT r FROM Resource r WHERE r.resourceReference = :resourceReference AND r.resourceStatus = :status AND r.resourceType = :type "
            + "AND r.stack.id is null")
    Optional<Resource> findByResourceReferenceAndStatusAndType(@Param("resourceReference") String resourceReference, @Param("status") CommonStatus status,
            @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.resourceReference = :resourceReference AND r.resourceType = :type AND r.stack.id is null")
    Optional<Resource> findByResourceReferenceAndType(@Param("resourceReference") String resourceReference,
            @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.resourceReference = :resourceReference AND r.resourceStatus = :status AND r.resourceType = :type "
            + "AND r.stack.id = :stackId")
    Optional<Resource> findByResourceReferenceAndStatusAndTypeAndStack(
            @Param("resourceReference") String resourceReference,
            @Param("status") CommonStatus status,
            @Param("type") ResourceType resourceType,
            @Param("stackId") Long stackId);

    Optional<Resource> findFirstByResourceStatusAndResourceTypeAndStackId(
            @Param("resourceStatus") CommonStatus status,
            @Param("resourceType") ResourceType resourceType,
            @Param("stackId") Long stackId);

    List<Resource> findAllByResourceStatusAndResourceTypeAndStackIdAndInstanceGroup(
            @Param("resourceStatus") CommonStatus status,
            @Param("resourceType") ResourceType resourceType,
            @Param("stackId") Long stackId,
            @Param("instanceGroup") String instanceGroup);
}
