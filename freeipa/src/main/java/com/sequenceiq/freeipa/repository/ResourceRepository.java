package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import jakarta.transaction.Transactional;
import jakarta.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.common.api.type.CommonStatus;
import com.sequenceiq.common.api.type.ResourceType;
import com.sequenceiq.freeipa.entity.Resource;

@Transactional(TxType.REQUIRED)
public interface ResourceRepository extends CrudRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    Optional<Resource> findByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name,
            @Param("type") com.sequenceiq.common.api.type.ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId")
    List<Resource> findAllByStackId(@Param("stackId") long stackId);

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
    List<Resource> findByResourceReferencesAndStatusAndTypeAndStack(
            @Param("resourceReferences") List<String> resourceReferences,
            @Param("status") CommonStatus status,
            @Param("type") ResourceType resourceType,
            @Param("stackId") Long stackId);

    List<Resource> findAllByResourceStatusAndResourceTypeAndStackId(CommonStatus status, ResourceType resourceType, Long stackId);

    @Query("SELECT count(r) > 0 FROM Resource r WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    boolean existsByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name, @Param("type") ResourceType type);

    @Query("SELECT count(r) > 0 FROM Resource r WHERE r.resourceReference = :resourceReference AND r.resourceType = :type AND r.stack.id is null")
    boolean existsByResourceReferenceAndType(@Param("resourceReference") String resourceReference, @Param("type") ResourceType type);

    @Modifying
    @Query("DELETE FROM Resource r WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    void deleteByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name, @Param("type") ResourceType type);

    Optional<Resource> findFirstByResourceStatusAndResourceTypeAndStackId(
            @Param("resourceStatus") CommonStatus status,
            @Param("resourceType") ResourceType resourceType,
            @Param("stackId") Long stackId);
}
