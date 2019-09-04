package com.sequenceiq.freeipa.repository;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.freeipa.entity.Resource;

@Transactional(TxType.REQUIRED)
@AuthorizationResource(type = AuthorizationResourceType.ENVIRONMENT)
public interface ResourceRepository extends CrudRepository<Resource, Long> {

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId AND r.resourceName = :name AND r.resourceType = :type")
    Optional<Resource> findByStackIdAndNameAndType(@Param("stackId") Long stackId, @Param("name") String name,
            @Param("type") com.sequenceiq.common.api.type.ResourceType type);

    @Query("SELECT r FROM Resource r WHERE r.stack.id = :stackId")
    List<Resource> findAllByStackId(@Param("stackId") long stackId);

}
