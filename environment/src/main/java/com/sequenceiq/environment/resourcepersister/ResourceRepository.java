package com.sequenceiq.environment.resourcepersister;

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

@Transactional(TxType.REQUIRED)
public interface ResourceRepository extends CrudRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE r.environment.id = :environmentId AND r.resourceName = :name AND r.resourceType = :type")
    Optional<Resource> findByEnvironmentIdAndNameAndType(@Param("environmentId") Long environmentId, @Param("name") String name,
            @Param("type") com.sequenceiq.common.api.type.ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.environment.id = :environmentId")
    List<Resource> findAllByEnvironmentId(@Param("environmentId") Long environmentId);

    @Query("SELECT r FROM Resource r WHERE r.resourceReference in :resourceReferences AND r.resourceStatus = :status AND r.resourceType = :type")
    List<Resource> findByResourceReferencesAndStatusAndType(@Param("resourceReferences") List<String> resourceReferences,
            @Param("status") CommonStatus status,
            @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.resourceReference = :resourceReference AND r.resourceType = :type")
    Optional<Resource> findByResourceReferenceAndType(@Param("resourceReference") String resourceReference, @Param("type") ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.environment.id = :environmentId AND r.resourceType = :type")
    Optional<Resource> findByEnvironmentIdAndType(@Param("environmentId") Long environmentId, @Param("type") com.sequenceiq.common.api.type.ResourceType type);

    @Query("SELECT count(r) > 0 FROM Resource r WHERE r.resourceReference = :resourceReference AND r.resourceType = :type")
    boolean existsByResourceReferenceAndType(@Param("resourceReference") String resourceReference, @Param("type") ResourceType type);

    @Modifying
    @Query("DELETE FROM Resource r WHERE r.resourceReference = :resourceReference AND r.resourceType = :type")
    void deleteByReferenceAndType(@Param("resourceReference") String resourceReference, @Param("type") ResourceType type);

    @Modifying
    @Query("DELETE FROM Resource r WHERE r.environment.id = :environmentId")
    void deleteAllByEnvironmentId(@Param("environmentId") Long environmentId);
}
