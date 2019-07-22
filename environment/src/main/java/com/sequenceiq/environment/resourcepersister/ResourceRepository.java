package com.sequenceiq.environment.resourcepersister;

import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;

@Transactional(TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface ResourceRepository extends BaseCrudRepository<Resource, Long> {

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT r FROM Resource r WHERE r.crn = :crn AND r.resourceName = :name AND r.resourceType = :type")
    Optional<Resource> findByStackIdAndNameAndType(@Param("crn") String crnId, @Param("name") String name, @Param("type") ResourceType type);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT r FROM Resource r WHERE r.crn = :crn")
    List<Resource> findAllByStackId(@Param("crn") String crn);

}
