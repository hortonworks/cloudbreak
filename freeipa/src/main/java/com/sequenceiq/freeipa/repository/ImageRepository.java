package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;

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
}
