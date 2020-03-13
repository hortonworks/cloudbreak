package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.sequenceiq.authorization.repository.BaseCrudRepository;
import com.sequenceiq.authorization.repository.CheckPermission;
import com.sequenceiq.authorization.resource.AuthorizationResource;
import com.sequenceiq.authorization.resource.AuthorizationResourceType;
import com.sequenceiq.authorization.resource.ResourceAction;
import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.Stack;

@Transactional(Transactional.TxType.REQUIRED)
@AuthorizationResourceType(resource = AuthorizationResource.ENVIRONMENT)
public interface ImageRepository extends BaseCrudRepository<Image, Long> {

    @CheckPermission(action = ResourceAction.READ)
    Image getByStack(Stack stack);

    @CheckPermission(action = ResourceAction.READ)
    @Query("SELECT i FROM Image i " +
            "LEFT JOIN i.stack s " +
            "WHERE s.id = :stackId")
    Image getByStackId(@Param("stackId") Long stackId);
}
