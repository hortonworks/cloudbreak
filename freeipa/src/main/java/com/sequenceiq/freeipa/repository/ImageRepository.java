package com.sequenceiq.freeipa.repository;

import javax.transaction.Transactional;

import com.sequenceiq.cloudbreak.workspace.repository.DisableHasPermission;
import com.sequenceiq.cloudbreak.workspace.repository.DisabledBaseRepository;
import com.sequenceiq.cloudbreak.workspace.repository.EntityType;
import com.sequenceiq.freeipa.entity.Image;
import com.sequenceiq.freeipa.entity.Stack;

@EntityType(entityClass = ImageRepository.class)
@Transactional(Transactional.TxType.REQUIRED)
@DisableHasPermission
public interface ImageRepository extends DisabledBaseRepository<Image, Long> {

    Image getByStack(Stack stack);
}
