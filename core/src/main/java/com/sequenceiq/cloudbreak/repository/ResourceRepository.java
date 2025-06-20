package com.sequenceiq.cloudbreak.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT r FROM Resource r WHERE r.stack.id in :stackIds AND r.resourceType = :type")
    List<Resource> findByStackIdsAndType(@Param("stackIds") List<Long> stackIds, @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId")
    List<Resource> findAllByStackId(@Param("stackId") long stackId);

    @Query("SELECT r FROM Resource r " +
            "LEFT JOIN r.stack s " +
            "WHERE s.id = :stackId " +
            "AND r.resourceStatus in :statuses")
    List<Resource> findAllByStackIdAndStatusIn(@Param("stackId") long stackId, @Param("statuses") Collection<CommonStatus> statuses);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND (r.resourceType NOT LIKE '%INSTANCE%' OR r.resourceType NOT LIKE '%DISK%')")
    Set<Resource> findAllByStackIdNotInstanceOrDisk(@Param("stackId") Long stackId);

    @Query("SELECT r FROM Resource r WHERE r.resourceReference in :resourceReferences AND r.resourceStatus = :status AND r.resourceType = :type "
            + "AND r.stack.id is null")
    List<Resource> findByResourceReferencesAndStatusAndType(@Param("resourceReferences") List<String> resourceReferences,
            @Param("status") CommonStatus status,
            @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.resourceReference = :resourceReference AND r.resourceType = :type AND r.stack.id is null")
    Optional<Resource> findByResourceReferenceAndType(@Param("resourceReference") String resourceReference,
            @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.resourceReference in :resourceReferences AND r.resourceStatus = :status AND r.resourceType = :type "
            + "AND r.stack.id = :stackId")
    List<Resource> findByResourceReferenceAndStatusAndTypeAndStack(
            @Param("resourceReferences") List<String> resourceReferences,
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

    List<Resource> findAllByResourceStatusAndResourceTypeAndStackId(
            @Param("resourceStatus") CommonStatus status,
            @Param("resourceType") ResourceType resourceType,
            @Param("stackId") Long stackId);

    @Query("SELECT count(r) > 0 FROM Resource r WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    boolean existsByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name, @Param("type") ResourceType type);

    @Query("SELECT count(r) > 0 FROM Resource r WHERE r.resourceReference = :resourceReference AND r.resourceType = :type AND r.stack.id is null")
    boolean existsByResourceReferenceAndType(@Param("resourceReference") String resourceReference, @Param("type") ResourceType type);

    @Modifying
    @Query("DELETE FROM Resource r WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    void deleteByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name, @Param("type") ResourceType type);

    @Modifying
    @Query("DELETE FROM Resource r WHERE r.stack.id = :stackId")
    void deleteByStackId(@Param("stackId") Long stackId);

    @Modifying
    @Query("DELETE FROM Resource r WHERE r.resourceReference = :resourceReference AND r.resourceType = :type AND r.stack.id is null")
    boolean deleteByResourceReferenceAndType(@Param("resourceReference") String resourceReference, @Param("type") ResourceType type);

    List<Resource> findAllByStackIdAndResourceTypeIn(Long stackId, Collection<ResourceType> resourceTypes);

    List<Resource> findAllByStackIdAndInstanceGroupAndResourceTypeInAndInstanceIdIsNotNull(Long stackId, String instanceGroup,
            Collection<ResourceType> resourceTypes);
}
